package com.jarvis.assistant.vision

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.voice.TextToSpeechHelper

class VisionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun onTextDetected(detectedText: String) {
        if (detectedText.isNotEmpty()) {
            TextToSpeechHelper.speak(this, detectedText)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TextToSpeechHelper.shutdown()
    }
}
