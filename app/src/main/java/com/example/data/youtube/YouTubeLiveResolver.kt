package com.example.data.youtube

import android.util.Log
import com.example.data.model.LiveArabicChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "YouTubeLiveResolver"

data class ResolvedYouTubeStream(
    val videoId: String?,
    val channelId: String?,
    val fallbackDirectUrl: String
)

object YouTubeLiveResolver {

    /**
     * Dynamically resolves the active YouTube Live videoId and channelId from a channel's
     * official /live URL so streams never break when channels rotate broadcast IDs.
     */
    suspend fun resolveLiveStream(channel: LiveArabicChannel): ResolvedYouTubeStream =
        withContext(Dispatchers.IO) {
            if (!channel.customVideoId.isNullOrBlank()) {
                return@withContext ResolvedYouTubeStream(
                    videoId = channel.customVideoId,
                    channelId = channel.knownChannelId,
                    fallbackDirectUrl = channel.youtubeLivePageUrl
                )
            }

            val pageUrl = channel.youtubeLivePageUrl
            var foundVideoId: String? = null
            var foundChannelId: String? = channel.knownChannelId

            try {
                val html = fetchPageHtml(pageUrl)
                if (html.isNotBlank()) {
                    // 1. Canonical watch URL is the cleanest indicator of an active live stream
                    val canonicalMatch = Regex(
                        """<link\s+rel="canonical"\s+href="https://www\.youtube\.com/watch\?v=([A-Za-z0-9_-]{11})""""
                    ).find(html)
                    foundVideoId = canonicalMatch?.groupValues?.getOrNull(1)

                    // 2. Fallback: extract from embedded ytInitialPlayerResponse JSON
                    if (foundVideoId == null) {
                        val liveVideoMatch = Regex(
                            """"videoId"\s*:\s*"([A-Za-z0-9_-]{11})"[^\}]{0,400}"isLive(?:Now|Content)?"\s*:\s*true"""
                        ).find(html)
                        foundVideoId = liveVideoMatch?.groupValues?.getOrNull(1)
                    }

                    // 3. Extract channel UC ID for /embed/live_stream?channel=... fallback
                    if (foundChannelId.isNullOrBlank()) {
                        val chMatch = Regex(
                            """"(?:externalId|channelId)"\s*:\s*"(UC[A-Za-z0-9_-]{20,})""""
                        ).find(html)
                        foundChannelId = chMatch?.groupValues?.getOrNull(1)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Live resolution warning for ${channel.nameAr}: ${e.localizedMessage}")
            }

            Log.d(
                TAG,
                "Resolved ${channel.nameAr}: videoId=$foundVideoId, channelId=$foundChannelId"
            )
            ResolvedYouTubeStream(
                videoId = foundVideoId,
                channelId = foundChannelId,
                fallbackDirectUrl = pageUrl
            )
        }

    /**
     * Parses any user-pasted YouTube URL, @handle, UC channel ID, or 11-char video ID.
     */
    fun parseUserInput(rawInput: String): Pair<String, String?> {
        val trimmed = rawInput.trim()
        // watch?v=XXXXXXXXXXX or youtu.be/XXXXXXXXXXX or /live/XXXXXXXXXXX
        val videoMatch = Regex(
            """(?:v=|youtu\.be/|/live/|/embed/)([A-Za-z0-9_-]{11})"""
        ).find(trimmed)
        if (videoMatch != null) {
            return trimmed to videoMatch.groupValues[1]
        }
        if (Regex("^[A-Za-z0-9_-]{11}$").matches(trimmed)) {
            return "https://www.youtube.com/watch?v=$trimmed" to trimmed
        }
        val handleMatch = Regex("""(@[A-Za-z0-9._-]+)""").find(trimmed)
        if (handleMatch != null) {
            return handleMatch.groupValues[1] to null
        }
        return trimmed to null
    }

    /**
     * Builds an HTML5 YouTube IFrame Player page hosted under https://www.youtube.com
     * with JS hooks for seamless Video <-> Radio Audio playback control.
     */
    fun buildPlayerHtml(resolved: ResolvedYouTubeStream): String {
        val embedSrc = when {
            !resolved.videoId.isNullOrBlank() ->
                "https://www.youtube.com/embed/${resolved.videoId}?autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=https://www.youtube.com"
            !resolved.channelId.isNullOrBlank() ->
                "https://www.youtube.com/embed/live_stream?channel=${resolved.channelId}&autoplay=1&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=https://www.youtube.com"
            else -> ""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                html, body {
                  margin: 0;
                  padding: 0;
                  width: 100%;
                  height: 100%;
                  background-color: #0A0F1C;
                  overflow: hidden;
                }
                iframe {
                  position: fixed;
                  top: 0;
                  left: 0;
                  width: 100%;
                  height: 100%;
                  border: 0;
                }
              </style>
            </head>
            <body>
              <iframe
                id="ytplayer"
                src="$embedSrc"
                allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                allowfullscreen>
              </iframe>
              <script>
                function postYtCommand(func, args) {
                  var iframe = document.getElementById('ytplayer');
                  if (!iframe || !iframe.contentWindow) return;
                  iframe.contentWindow.postMessage(JSON.stringify({
                    event: 'command',
                    func: func,
                    args: args || []
                  }), '*');
                }
                window.atheerPlay = function() { postYtCommand('playVideo'); };
                window.atheerPause = function() { postYtCommand('pauseVideo'); };
                window.atheerMute = function() { postYtCommand('mute'); };
                window.atheerUnmute = function() { postYtCommand('unMute'); };
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun fetchPageHtml(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
            setRequestProperty("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+667")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val sb = StringBuilder(65_536)
                    val buffer = CharArray(8192)
                    var total = 0
                    while (total < 350_000) {
                        val read = reader.read(buffer)
                        if (read <= 0) break
                        sb.append(buffer, 0, read)
                        total += read
                    }
                    sb.toString()
                }
            } else {
                ""
            }
        } finally {
            conn.disconnect()
        }
    }
}
