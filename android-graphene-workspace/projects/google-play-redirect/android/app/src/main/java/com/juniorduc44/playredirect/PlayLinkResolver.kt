package com.juniorduc44.playredirect

import android.net.Uri

/**
 * Converts Play Store / market intents into a normal HTTPS URL the system
 * browser can open — no GMS / Play Store package required.
 */
object PlayLinkResolver {

    private const val PLAY_STORE_WEB = "https://play.google.com/store"

    /**
     * @return browser-friendly HTTPS URL, or null if the URI cannot be mapped
     */
    fun toBrowserUrl(uri: Uri?): String? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase() ?: return null

        return when (scheme) {
            "market" -> marketToHttps(uri)
            "http", "https" -> httpsPlayToCanonical(uri)
            else -> null
        }
    }

    private fun marketToHttps(uri: Uri): String {
        // market://details?id=com.example.app
        // market://search?q=foo
        // market://apps/details?id=...
        val host = uri.host?.lowercase().orEmpty()
        val id = uri.getQueryParameter("id")
        val q = uri.getQueryParameter("q")

        return when {
            !id.isNullOrBlank() ->
                "https://play.google.com/store/apps/details?id=${Uri.encode(id)}"
            host == "details" && !id.isNullOrBlank() ->
                "https://play.google.com/store/apps/details?id=${Uri.encode(id)}"
            !q.isNullOrBlank() ->
                "https://play.google.com/store/search?q=${Uri.encode(q)}"
            host == "search" && !q.isNullOrBlank() ->
                "https://play.google.com/store/search?q=${Uri.encode(q)}"
            else -> {
                // Best-effort: drop scheme and send path/query to play.google.com
                val path = uri.encodedPath?.trimStart('/') ?: ""
                val query = uri.encodedQuery
                buildString {
                    append(PLAY_STORE_WEB)
                    if (path.isNotEmpty()) {
                        append('/')
                        append(path)
                    }
                    if (!query.isNullOrBlank()) {
                        append('?')
                        append(query)
                    }
                }
            }
        }
    }

    private fun httpsPlayToCanonical(uri: Uri): String? {
        val host = uri.host?.lowercase() ?: return null
        if (host != "play.google.com" && host != "market.android.com") {
            return null
        }
        // Prefer https always
        return uri.buildUpon().scheme("https").build().toString()
    }
}
