export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname !== '/command' || request.method !== 'POST') {
      return new Response('Not Found', { status: 404 });
    }

    let body;
    try {
      body = await request.json();
    } catch (e) {
      return new Response(JSON.stringify({ error: 'Invalid JSON body' }), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      });
    }

    const { speech, deviceId } = body;

    if (!speech || typeof speech !== 'string') {
      return new Response(JSON.stringify({ error: 'Missing "speech" field' }), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      });
    }

    if (!deviceId || typeof deviceId !== 'string') {
      return new Response(JSON.stringify({ error: 'Missing "deviceId" field' }), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      });
    }

    // --- Rate limiting: max 50 requests per device per day ---
    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
    const rateLimitKey = `ratelimit:${deviceId}:${today}`;
    const DAILY_LIMIT = 50;

    const currentCountRaw = await env.RATE_LIMIT_KV.get(rateLimitKey);
    const currentCount = currentCountRaw ? parseInt(currentCountRaw, 10) : 0;

    if (currentCount >= DAILY_LIMIT) {
      return new Response(
        JSON.stringify({
          action: 'reply',
          message: 'Daily request limit reached. Try again tomorrow.',
        }),
        { status: 429, headers: { 'content-type': 'application/json' } }
      );
    }

    // Increment count, expire after 26 hours (safety buffer past midnight)
    await env.RATE_LIMIT_KV.put(rateLimitKey, String(currentCount + 1), {
      expirationTtl: 60 * 60 * 26,
    });

    // --- Call Gemini API ---
    const systemPrompt = `
You are the reasoning core of an Android voice assistant called Jarvis.
The user speaks a command; you must respond with ONLY a single JSON object,
no prose, no markdown fences, matching this exact schema:

{
  "action": "open_app | call | send_sms | toggle_setting | read_screen | reply",
  "target": "string or null - app name / contact name / setting name",
  "message": "string or null - sms body, or the spoken reply text for the user",
  "extra": {"key": "value"}
}

Rules:
- If the user just wants conversation (no device action), use action "reply" and put
  your spoken response in "message".
- For "open_app", target must be the common app name as installed (e.g. "WhatsApp", "Camera").
- For "send_sms", target = contact name, message = body text.
- For "call", target = contact name.
- For "toggle_setting", target = one of "wifi","bluetooth","flashlight","airplane_mode".
- Never include commentary outside the JSON object.
`.trim();

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${env.GEMINI_API_KEY}`;

    const geminiBody = {
      system_instruction: {
        parts: [{ text: systemPrompt }],
      },
      contents: [
        {
          role: 'user',
          parts: [{ text: speech }],
        },
      ],
      generationConfig: {
        maxOutputTokens: 300,
      },
    };

    let geminiResponse;
    try {
      geminiResponse = await fetch(geminiUrl, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(geminiBody),
      });
    } catch (e) {
      return new Response(
        JSON.stringify({ action: 'reply', message: 'Network error contacting AI service.' }),
        { status: 502, headers: { 'content-type': 'application/json' } }
      );
    }

    if (!geminiResponse.ok) {
      const errText = await geminiResponse.text();
      return new Response(
        JSON.stringify({
          action: 'reply',
          message: `AI service error: ${geminiResponse.status} - ${errText}`,
        }),
        { status: 502, headers: { 'content-type': 'application/json' } }
      );
    }

    const geminiData = await geminiResponse.json();

    let text;
    try {
      text = geminiData.candidates[0].content.parts[0].text
        .trim()
        .replace(/^```json/, '')
        .replace(/^```/, '')
        .replace(/```$/, '')
        .trim();
    } catch (e) {
      return new Response(
        JSON.stringify({ action: 'reply', message: 'Could not parse AI response.' }),
        { status: 502, headers: { 'content-type': 'application/json' } }
      );
    }

    // Validate it's actually JSON before sending back
    try {
      JSON.parse(text);
    } catch (e) {
      return new Response(
        JSON.stringify({ action: 'reply', message: 'Sorry, I could not understand that.' }),
        { status: 200, headers: { 'content-type': 'application/json' } }
      );
    }

    return new Response(text, {
      status: 200,
      headers: { 'content-type': 'application/json' },
    });
  },
};
