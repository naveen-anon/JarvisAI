package com.jarvis.assistant.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 🚀 SMART APP NAVIGATOR v2.1
 *
 * Handles complex commands like:
 * "Open Flipkart and show me watches under 500"
 * "Search for travel photos on Instagram"
 * "Find meditation music on YouTube"
 * "Show nearby restaurants on Google Maps"
 *
 * Works with: Flipkart, Amazon, Instagram, YouTube,
 *             Google Maps, WhatsApp, Chrome, etc.
 */
class SmartAppNavigator(private val context: Context) {

    data class NavigationCommand(
        val app: String,                    // flipkart, amazon, instagram, youtube, etc
        val action: String,                 // search, filter, browse
        val query: String,                  // "watches", "travel photos", etc — never null, defaults handled in parseCommand
        val filters: Map<String, String> = emptyMap()  // price: "500", category: "shoes", etc
    )

    /**
     * Parse natural language commands
     * Examples:
     * "Flipkart open karo aur watches dikhao under 500"
     * "Instagram open karo travel photos search karo"
     * "YouTube open karo meditation songs"
     * "Google Maps open karo nearby restaurants"
     * "WhatsApp open karo Raj ke messages dikhao"
     */
    fun parseCommand(text: String): NavigationCommand? {
        val cmd = text.lowercase()

        // Flipkart/Amazon shopping
        if (containsAny(cmd, "flipkart", "amazon", "e-commerce", "shopping")) {
            val app = if ("flipkart" in cmd) "flipkart" else "amazon"
            val query = extractKeyword(cmd, listOf("watch", "phone", "shoe", "book", "shirt", "laptop", "headphone"))
            val price = extractPrice(cmd)
            val rating = extractRating(cmd)

            val filters = mutableMapOf<String, String>()
            if (price != null) filters["price"] = price
            if (rating != null) filters["rating"] = rating

            return NavigationCommand(
                app = app,
                action = "search_and_filter",
                query = query ?: "products",
                filters = filters
            )
        }

        // Instagram - photos, videos, hashtags
        if ("instagram" in cmd) {
            val query = extractKeyword(cmd, listOf("travel", "food", "photography", "nature", "selfie", "video", "reels"))
            val filter = when {
                "hashtag" in cmd || "#" in cmd -> "hashtags"
                "video" in cmd || "reels" in cmd -> "videos"
                "story" in cmd -> "stories"
                else -> "photos"
            }

            return NavigationCommand(
                app = "instagram",
                action = "search",
                query = query ?: "explore",
                filters = mapOf("type" to filter)
            )
        }

        // YouTube - videos, music, channels
        if ("youtube" in cmd) {
            val query = extractKeyword(cmd, listOf(
                "meditation", "music", "tutorial", "comedy", "vlog",
                "gaming", "cooking", "workout", "song"
            ))
            val filter = when {
                "music" in cmd || "song" in cmd -> "music"
                "tutorial" in cmd -> "tutorials"
                "workout" in cmd -> "fitness"
                "gaming" in cmd -> "gaming"
                else -> "videos"
            }

            return NavigationCommand(
                app = "youtube",
                action = "search",
                query = query ?: "trending",
                filters = mapOf("type" to filter, "sort" to "relevance")
            )
        }

        // Google Maps - places, directions
        if (containsAny(cmd, "maps", "location", "direction", "navigate")) {
            val query = extractKeyword(cmd, listOf(
                "restaurant", "cafe", "hotel", "hospital", "school",
                "gym", "park", "mall", "gas station", "atm"
            ))
            val filter = when {
                "nearby" in cmd || "near me" in cmd || "paas" in cmd -> "nearby"
                "direction" in cmd || "route" in cmd -> "directions"
                else -> "search"
            }

            return NavigationCommand(
                app = "maps",
                action = filter,
                query = query ?: "places",
                filters = mapOf("radius" to "5km")
            )
        }

        // WhatsApp - contacts, groups
        if ("whatsapp" in cmd) {
            val query = extractKeyword(cmd, listOf(
                "raj", "priya", "mom", "dad", "friends", "work"
            ))
            val filter = when {
                "message" in cmd || "messages" in cmd -> "messages"
                "status" in cmd -> "status"
                "call" in cmd -> "calls"
                "group" in cmd -> "groups"
                else -> "chats"
            }

            return NavigationCommand(
                app = "whatsapp",
                action = "open_contact",
                query = query ?: "chats",
                filters = mapOf("type" to filter)
            )
        }

        // Chrome - web search
        if (containsAny(cmd, "google", "search", "chrome", "web")) {
            val query = text.substringAfter("search").trim()
                .substringAfter("google").trim()
                .substringAfter("karo").trim()

            return NavigationCommand(
                app = "chrome",
                action = "search",
                query = query.ifBlank { "google" },
                filters = mapOf("engine" to "google")
            )
        }

        // Twitter / X - tweets, trending
        // NOTE: matches the word "x" or "twitter" as a whole word only — a bare
        // substring check on "x" would false-trigger on words like "next" or "explain".
        if (containsAny(cmd, "twitter", "tweet") || Regex("""\bx\b""").containsMatchIn(cmd)) {
            val query = extractKeyword(cmd, listOf("trending", "technology", "sports", "news"))

            return NavigationCommand(
                app = "twitter",
                action = "search",
                query = query ?: "trending",
                filters = mapOf("type" to "tweets")
            )
        }

        // Facebook - posts, friends, groups
        if ("facebook" in cmd) {
            val filter = when {
                "friend" in cmd || "friends" in cmd -> "friends"
                "group" in cmd -> "groups"
                "event" in cmd -> "events"
                else -> "feed"
            }

            return NavigationCommand(
                app = "facebook",
                action = "browse",
                query = "feed",
                filters = mapOf("type" to filter)
            )
        }

        // Reddit - subreddits, posts
        if ("reddit" in cmd) {
            val subreddit = extractKeyword(cmd, listOf(
                "askreddit", "funny", "technology", "india", "programming"
            ))

            return NavigationCommand(
                app = "reddit",
                action = "browse_subreddit",
                query = subreddit ?: "popular",
                filters = mapOf("sort" to "hot")
            )
        }

        return null
    }

    /**
     * Execute the navigation command.
     * Call site should handle the nullable result of parseCommand(), e.g.:
     *   val cmd = navigator.parseCommand(text) ?: return "Sorry, I didn't understand that."
     *   navigator.execute(cmd)
     */
    suspend fun execute(cmd: NavigationCommand): String {
        return when (cmd.app) {
            "flipkart" -> navigateFlipkart(cmd)
            "amazon" -> navigateAmazon(cmd)
            "instagram" -> navigateInstagram(cmd)
            "youtube" -> navigateYouTube(cmd)
            "maps" -> navigateMaps(cmd)
            "whatsapp" -> navigateWhatsApp(cmd)
            "chrome" -> navigateChrome(cmd)
            "twitter" -> navigateTwitter(cmd)
            "facebook" -> navigateFacebook(cmd)
            "reddit" -> navigateReddit(cmd)
            else -> "App not supported yet"
        }
    }

    // ===== FLIPKART =====
    private fun navigateFlipkart(cmd: NavigationCommand): String {
        val priceFilter = cmd.filters["price"]?.let { " under ₹$it" } ?: ""
        val ratingFilter = cmd.filters["rating"]?.let { " rated $it+" } ?: ""

        return if (launchAppOrWeb("com.flipkart.android", "https://www.flipkart.com/search?q=${encode(cmd.query)}")) {
            "✓ Opening Flipkart. Searching for ${cmd.query}$priceFilter$ratingFilter"
        } else {
            "⚠️ Couldn't open Flipkart."
        }
    }

    // ===== AMAZON =====
    private fun navigateAmazon(cmd: NavigationCommand): String {
        return if (launchAppOrWeb("com.amazon.mShop.android.shopping", "https://www.amazon.in/s?k=${encode(cmd.query)}")) {
            "✓ Opening Amazon. Searching for ${cmd.query}"
        } else {
            "⚠️ Couldn't open Amazon."
        }
    }

    // ===== INSTAGRAM =====
    private fun navigateInstagram(cmd: NavigationCommand): String {
        val filter = cmd.filters["type"] ?: "photos"
        return if (launchAppOrWeb("com.instagram.android", "https://www.instagram.com/explore/tags/${encode(cmd.query)}/")) {
            "✓ Opening Instagram. Searching for ${cmd.query} $filter"
        } else {
            "⚠️ Couldn't open Instagram."
        }
    }

    // ===== YOUTUBE =====
    private fun navigateYouTube(cmd: NavigationCommand): String {
        val url = "https://www.youtube.com/results?search_query=${encode(cmd.query)}"
        return if (launchAppOrWeb("com.google.android.youtube", url)) {
            "✓ Opening YouTube. Searching for ${cmd.query}"
        } else {
            "⚠️ Couldn't open YouTube."
        }
    }

    // ===== GOOGLE MAPS =====
    private fun navigateMaps(cmd: NavigationCommand): String {
        val url = "geo:0,0?q=${encode(cmd.query)}"
        return if (launchAppOrWeb("com.google.android.apps.maps", url)) {
            "✓ Opening Google Maps. Finding ${cmd.query}"
        } else {
            "⚠️ Couldn't open Google Maps."
        }
    }

    // ===== WHATSAPP =====
    private fun navigateWhatsApp(cmd: NavigationCommand): String {
        // WhatsApp has no public deep-link search API, so we just launch the app
        // itself (via its proper launcher intent, not a bare ACTION_MAIN send).
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        return if (launchIntent != null) {
            try {
                context.startActivity(launchIntent)
                "✓ Opening WhatsApp. Look for ${cmd.query}"
            } catch (e: ActivityNotFoundException) {
                "⚠️ Couldn't open WhatsApp."
            }
        } else {
            "⚠️ WhatsApp not installed."
        }
    }

    // ===== CHROME / WEB SEARCH =====
    private fun navigateChrome(cmd: NavigationCommand): String {
        val url = "https://www.google.com/search?q=${encode(cmd.query)}"
        return if (launchAppOrWeb("com.android.chrome", url)) {
            "✓ Searching Google for: ${cmd.query}"
        } else {
            "⚠️ Couldn't open a browser."
        }
    }

    // ===== TWITTER =====
    private fun navigateTwitter(cmd: NavigationCommand): String {
        val url = "https://twitter.com/search?q=${encode(cmd.query)}"
        return if (launchAppOrWeb("com.twitter.android", url)) {
            "✓ Opening Twitter. Searching for ${cmd.query}"
        } else {
            "⚠️ Couldn't open Twitter."
        }
    }

    // ===== FACEBOOK =====
    private fun navigateFacebook(cmd: NavigationCommand): String {
        return if (launchAppOrWeb("com.facebook.katana", "https://www.facebook.com/")) {
            "✓ Opening Facebook. Browsing ${cmd.filters["type"] ?: "feed"}"
        } else {
            "⚠️ Couldn't open Facebook."
        }
    }

    // ===== REDDIT =====
    private fun navigateReddit(cmd: NavigationCommand): String {
        val subreddit = cmd.query.removePrefix("r/")
        val url = "https://www.reddit.com/r/${encode(subreddit)}"
        return if (launchAppOrWeb("com.reddit.frontpage", url)) {
            "✓ Opening Reddit. Browsing r/$subreddit"
        } else {
            "⚠️ Couldn't open Reddit."
        }
    }

    // ===== HELPERS =====

    /**
     * Tries to open [url] in the named app package first; if that app isn't
     * installed (or the intent otherwise fails to resolve), falls back to
     * opening the same URL in whatever browser is available. Returns false
     * only if neither attempt worked.
     */
    private fun launchAppOrWeb(packageName: String, url: String): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(packageName)
        }
        return try {
            context.startActivity(appIntent)
            true
        } catch (e: ActivityNotFoundException) {
            // App isn't installed / can't handle it — retry as a plain web link.
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                context.startActivity(webIntent)
                true
            } catch (e2: ActivityNotFoundException) {
                false
            }
        }
    }

    private fun encode(value: String): String = Uri.encode(value)

    private fun extractKeyword(cmd: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            if (keyword in cmd) return keyword
        }
        return null
    }

    /**
     * Matches price mentioned either before or after the keyword, e.g. both
     * "500 rupees" and "under 500" / "below 500" now work.
     */
    private fun extractPrice(cmd: String): String? {
        val before = Regex("""(\d+)\s*(?:rupee|rupees|rs|₹)""")
        val after = Regex("""(?:under|below|se kam|₹)\s*(\d+)""")
        return before.find(cmd)?.groupValues?.get(1)
            ?: after.find(cmd)?.groupValues?.get(1)
    }

    private fun extractRating(cmd: String): String? {
        val regex = Regex("""(\d+)\s*(?:star|rating)""")
        return regex.find(cmd)?.groupValues?.get(1)
    }

    private fun containsAny(text: String, vararg needles: String) =
        needles.any { text.contains(it) }
}

/**
 * Usage Examples:
 *
 * val navigator = SmartAppNavigator(context)
 * val cmd = navigator.parseCommand("Flipkart open karo aur watches dikhao under 500")
 * val resultMessage = if (cmd != null) navigator.execute(cmd) else "Sorry, I didn't understand that."
 *
 * // Example 2: Instagram search
 * navigator.parseCommand("Instagram open karo travel photos search karo")
 *     ?.let { navigator.execute(it) }
 *
 * // Example 3: YouTube search
 * navigator.parseCommand("YouTube open karo meditation music")
 *     ?.let { navigator.execute(it) }
 *
 * // Example 4: Google Maps
 * navigator.parseCommand("Maps open karo nearby restaurants dikhao")
 *     ?.let { navigator.execute(it) }
 *
 * // Example 5: WhatsApp
 * navigator.parseCommand("WhatsApp open karo Raj ke messages")
 *     ?.let { navigator.execute(it) }
 */
