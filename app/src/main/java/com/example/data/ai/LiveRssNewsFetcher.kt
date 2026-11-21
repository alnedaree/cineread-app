package com.example.data.ai

import android.util.Log
import android.util.Xml
import com.example.data.model.NewsCategory
import com.example.data.model.NewsSource
import com.example.data.model.RadioChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "LiveRssNewsFetcher"

data class RssNewsItem(
    val title: String,
    val description: String,
    val link: String,
    val sourceName: String,
    val pubDate: String
)

class LiveRssNewsFetcher {

    suspend fun fetchLiveNewsForChannel(
        channel: RadioChannel,
        selectedCategories: List<NewsCategory> = emptyList()
    ): List<RssNewsItem> =
        withContext(Dispatchers.IO) {
            val urls = buildFeedList(channel, selectedCategories)
            val perFeedCap = if (urls.size >= 4) 3 else 5
            val collected = mutableListOf<RssNewsItem>()
            val seenTitles = mutableSetOf<String>()

            for ((sourceLabel, feedUrl) in urls) {
                if (collected.size >= 14) break
                try {
                    val xml = downloadFeedXml(feedUrl)
                    if (xml.isNotBlank()) {
                        val parsed = parseRssXml(xml, fallbackSource = sourceLabel)
                        var addedForThisFeed = 0
                        for (item in parsed) {
                            val normalized = item.title.lowercase().trim()
                            if (normalized.length > 8 && seenTitles.add(normalized)) {
                                collected.add(item)
                                addedForThisFeed++
                                if (addedForThisFeed >= perFeedCap || collected.size >= 14) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RSS feed failed ($sourceLabel): ${e.localizedMessage}")
                }
            }
            collected
        }

    private fun buildFeedList(
        channel: RadioChannel,
        selectedCategories: List<NewsCategory>
    ): List<Pair<String, String>> {
        // On the main Breaking/General channel, prioritize the user's selected news categories!
        if (channel.id == "breaking_general" && selectedCategories.isNotEmpty()) {
            val categoryFeeds = selectedCategories.map { cat ->
                cat.nameAr to cat.rssFeedUrl
            }
            return categoryFeeds + feedUrlsForChannel(channel)
        }
        return feedUrlsForChannel(channel)
    }

    fun toNewsBrief(items: List<RssNewsItem>): GatheredNewsBrief {
        val sb = StringBuilder()
        val sources = mutableListOf<NewsSource>()
        val seenUrls = mutableSetOf<String>()

        items.forEachIndexed { idx, item ->
            sb.append("${idx + 1}. [${item.sourceName}] ${item.title}")
            if (item.description.isNotBlank()) {
                sb.append(" — ${item.description.take(240)}")
            }
            sb.append("\n")

            if (item.link.isNotBlank() && seenUrls.add(item.link)) {
                sources.add(
                    NewsSource(
                        title = "${item.sourceName}: ${item.title.take(50)}",
                        url = item.link,
                        domain = item.sourceName
                    )
                )
            }
        }
        return GatheredNewsBrief(
            summaryText = sb.toString().trim(),
            sources = sources.take(8)
        )
    }

    private fun feedUrlsForChannel(channel: RadioChannel): List<Pair<String, String>> {
        return when (channel.id) {
            "breaking_general" -> listOf(
                "أخبار جوجل" to "https://news.google.com/rss?hl=ar&gl=SA&ceid=SA:ar",
                "بي بي سي عربي" to "https://feeds.bbci.co.uk/arabic/rss.xml",
                "الجزيرة" to "https://www.aljazeera.net/aljazeerarss/a7c186be-1baa-4bd4-9d80-a84db769f779/73d0e1b4-532f-45ef-b135-bfdff8b8cab9"
            )
            "economy_markets" -> listOf(
                "أخبار الأعمال" to "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=ar&gl=SA&ceid=SA:ar",
                "بي بي سي اقتصاد" to "https://feeds.bbci.co.uk/arabic/business/rss.xml"
            )
            "tech_ai" -> listOf(
                "أخبار التقنية" to "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=ar&gl=SA&ceid=SA:ar",
                "بي بي سي علوم وتقنية" to "https://feeds.bbci.co.uk/arabic/scienceandtech/rss.xml"
            )
            "sports_live" -> listOf(
                "أخبار الرياضة" to "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=ar&gl=SA&ceid=SA:ar",
                "بي بي سي رياضة" to "https://feeds.bbci.co.uk/arabic/sports/rss.xml"
            )
            "science_culture" -> listOf(
                "أخبار العلوم" to "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=ar&gl=SA&ceid=SA:ar",
                "أخبار الصحة" to "https://news.google.com/rss/headlines/section/topic/HEALTH?hl=ar&gl=SA&ceid=SA:ar"
            )
            else -> {
                val query = URLEncoder.encode(channel.topicPrompt.take(80), "UTF-8")
                listOf(
                    "بحث الأخبار" to "https://news.google.com/rss/search?q=$query&hl=ar&gl=SA&ceid=SA:ar",
                    "أخبار جوجل" to "https://news.google.com/rss?hl=ar&gl=SA&ceid=SA:ar"
                )
            }
        }
    }

    private fun downloadFeedXml(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AtheerNewsRadio/1.0)")
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRssXml(xml: String, fallbackSource: String): List<RssNewsItem> {
        val items = mutableListOf<RssNewsItem>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var insideItem = false
        var title = ""
        var description = ""
        var link = ""
        var source = fallbackSource
        var pubDate = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < 10) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "item", "entry" -> {
                            insideItem = true
                            title = ""
                            description = ""
                            link = ""
                            source = fallbackSource
                            pubDate = ""
                        }
                        "title" -> if (insideItem) title = stripHtml(safeNextText(parser))
                        "description", "summary" -> if (insideItem) description = stripHtml(safeNextText(parser))
                        "link" -> if (insideItem) {
                            val href = parser.getAttributeValue(null, "href")
                            val txt = safeNextText(parser).trim()
                            link = txt.ifBlank { href.orEmpty() }
                        }
                        "source" -> if (insideItem) {
                            val srcTxt = stripHtml(safeNextText(parser))
                            if (srcTxt.isNotBlank()) source = srcTxt
                        }
                        "pubdate", "updated" -> if (insideItem) pubDate = safeNextText(parser).trim()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (insideItem && (parser.name.equals("item", true) || parser.name.equals("entry", true))) {
                        if (title.isNotBlank()) {
                            items.add(
                                RssNewsItem(
                                    title = title,
                                    description = description,
                                    link = link,
                                    sourceName = source,
                                    pubDate = pubDate
                                )
                            )
                        }
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try {
            parser.nextText().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun stripHtml(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
