package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

enum class BroadcastStyle(val id: String) {
    NEWSROOM_FORMAL("formal"),
    PODCAST_DIALOGUE("podcast"),
    RAPID_FLASH("rapid");

    companion object {
        fun fromId(id: String?): BroadcastStyle =
            entries.firstOrNull { it.id == id } ?: PODCAST_DIALOGUE
    }
}

enum class SegmentType(val labelAr: String) {
    INTRO("استهلال وعناوين الساعة"),
    BREAKING("خبر عاجل وتحليل"),
    DIALOGUE("حوار الأستوديو"),
    ROUNDUP("موجز ومتابعة");

    companion object {
        fun fromString(raw: String): SegmentType = when (raw.uppercase()) {
            "INTRO" -> INTRO
            "BREAKING" -> BREAKING
            "DIALOGUE" -> DIALOGUE
            "ROUNDUP" -> ROUNDUP
            else -> DIALOGUE
        }
    }
}

enum class VoiceEngineSource {
    GOOGLE_AI_VOICE,
    DEVICE_TTS_FALLBACK
}

data class RadioAnchor(
    val id: String,
    val nameAr: String,
    val roleAr: String,
    val bioAr: String,
    val googleVoiceName: String,
    val voiceDescriptorAr: String,
    val speakingStyleInstruction: String,
    @DrawableRes val avatarRes: Int,
    val fallbackPitch: Float = 1.0f,
    val fallbackRate: Float = 1.0f
)

data class RadioChannel(
    val id: String,
    val nameAr: String,
    val frequencyLabel: String,
    val taglineAr: String,
    val categoryAr: String,
    val topicPrompt: String,
    val anchorIds: List<String>,
    val accentIndex: Int = 0,
    val isCustom: Boolean = false
)

data class NewsCategory(
    val id: String,
    val nameAr: String,
    val subtitleAr: String,
    val queryTopicAr: String,
    val rssFeedUrl: String,
    val accentIndex: Int,
    val iconKey: String
)

data class DialogueTurn(
    val id: String,
    val segmentIndex: Int,
    val turnIndex: Int,
    val segmentTitleAr: String,
    val segmentType: SegmentType,
    val anchorId: String,
    val textAr: String,
    val emotionCue: String,
    val headlineTag: String
)

data class NewsSource(
    val title: String,
    val url: String,
    val domain: String = ""
)

data class BroadcastEpisode(
    val id: String,
    val channelId: String,
    val channelNameAr: String,
    val channelFrequency: String,
    val programTitleAr: String,
    val broadcastStyle: BroadcastStyle,
    val generatedAtMillis: Long,
    val breakingTickerHeadlines: List<String>,
    val turns: List<DialogueTurn>,
    val sources: List<NewsSource>,
    val isBreakingFlash: Boolean = false
)

data class DailyScheduleSlot(
    val timeLabel: String,
    val programNameAr: String,
    val descriptionAr: String,
    val hostNamesAr: String,
    val categoryAr: String,
    val isLiveNow: Boolean
)

object RadioCatalog {
    val anchors: List<RadioAnchor> = listOf(
        RadioAnchor(
            id = "tariq",
            nameAr = "طارق المنصور",
            roleAr = "كبير المذيعين • الأخبار العاجلة",
            bioAr = "صوت إذاعي رصين يقود النشرات الرئيسية ويدير الحوارات الإخبارية العاجلة بدقة ووضوح.",
            googleVoiceName = "Charon",
            voiceDescriptorAr = "صوت إذاعي عميق ومتزن (Charon)",
            speakingStyleInstruction = "Read in clear, authoritative Modern Standard Arabic radio news anchor tone, steady broadcast pacing, warm studio resonance.",
            avatarRes = R.drawable.img_anchor_tariq,
            fallbackPitch = 0.90f,
            fallbackRate = 0.98f
        ),
        RadioAnchor(
            id = "layla",
            nameAr = "ليلى العتيبي",
            roleAr = "محاورة ومراسلة التقارير الخاصة",
            bioAr = "متخصصة في صياغة التقارير المعمقة وربط الأحداث الدولية بأسلوب بودكاست حيوي وقريب للمستمع.",
            googleVoiceName = "Kore",
            voiceDescriptorAr = "صوت نسائي واضح وحيوي (Kore)",
            speakingStyleInstruction = "Read in articulate, engaging Modern Standard Arabic female broadcast journalist voice, expressive studio podcast delivery.",
            avatarRes = R.drawable.img_anchor_layla,
            fallbackPitch = 1.12f,
            fallbackRate = 1.02f
        ),
        RadioAnchor(
            id = "karim",
            nameAr = "كريم الشامي",
            roleAr = "محلل الاقتصاد والتقنية والرياضة",
            bioAr = "يقرأ الأرقام والأسواق وأحدث قفزات الذكاء الاصطناعي والرياضة العالمية بإيقاع عصري سريع.",
            googleVoiceName = "Puck",
            voiceDescriptorAr = "صوت تحليلي عصري ونشيط (Puck)",
            speakingStyleInstruction = "Read in energetic, crisp Modern Standard Arabic radio analyst voice, dynamic modern tech and business podcast style.",
            avatarRes = R.drawable.img_anchor_karim,
            fallbackPitch = 1.02f,
            fallbackRate = 1.05f
        )
    )

    fun anchorById(id: String): RadioAnchor =
        anchors.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: anchors.first()

    val defaultSelectedCategoryIds: Set<String> = setOf(
        "yemen_news",
        "politics",
        "tech",
        "sports"
    )

    val newsCategories: List<NewsCategory> = listOf(
        NewsCategory(
            id = "yemen_news",
            nameAr = "أخبار اليمن",
            subtitleAr = "المستجدات الميدانية والسياسية والاقتصادية والخدمية في اليمن",
            queryTopicAr = "أخبار اليمن اليوم العاجلة والسياسية والاقتصادية",
            rssFeedUrl = "https://news.google.com/rss/search?q=%D8%A3%D8%AE%D8%A8%D8%A7%D8%B1+%D8%A7%D9%84%D9%8A%D9%85%D9%86&hl=ar&gl=YE&ceid=YE:ar",
            accentIndex = 0,
            iconKey = "yemen"
        ),
        NewsCategory(
            id = "politics",
            nameAr = "السياسة",
            subtitleAr = "التطورات الدبلوماسية والقرارات الدولية والملفات الساخنة",
            queryTopicAr = "أخبار السياسة العربية والدولية العاجلة اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/WORLD?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 1,
            iconKey = "politics"
        ),
        NewsCategory(
            id = "tech",
            nameAr = "التقنية والذكاء",
            subtitleAr = "الذكاء الاصطناعي والهواتف والشركات التقنية والأمن السيبراني",
            queryTopicAr = "أخبار التكنولوجيا والذكاء الاصطناعي اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 2,
            iconKey = "tech"
        ),
        NewsCategory(
            id = "sports",
            nameAr = "الرياضة",
            subtitleAr = "مباريات كرة القدم والدوريات العربية والأوروبية والانتقالات",
            queryTopicAr = "أخبار الرياضة وكرة القدم اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 3,
            iconKey = "sports"
        ),
        NewsCategory(
            id = "economy",
            nameAr = "الاقتصاد والأسواق",
            subtitleAr = "النفط والذهب والعملات والبنوك المركزية وأسواق المال",
            queryTopicAr = "أخبار الاقتصاد والأسواق والنفط والذهب اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 1,
            iconKey = "economy"
        ),
        NewsCategory(
            id = "arab_world",
            nameAr = "الشؤون العربية",
            subtitleAr = "متابعة شاملة لأبرز الأحداث في الخليج والعالم العربي",
            queryTopicAr = "أهم أخبار الخليج والشرق الأوسط والعالم العربي اليوم",
            rssFeedUrl = "https://feeds.bbci.co.uk/arabic/middleeast/rss.xml",
            accentIndex = 0,
            iconKey = "arab"
        ),
        NewsCategory(
            id = "science_health",
            nameAr = "العلوم والصحة",
            subtitleAr = "الاكتشافات الطبية والفضاء والتطورات العلمية الحديثة",
            queryTopicAr = "أخبار العلوم والفضاء والصحة والطب اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 2,
            iconKey = "science"
        ),
        NewsCategory(
            id = "culture_society",
            nameAr = "الثقافة والمجتمع",
            subtitleAr = "تقارير ثقافية وقصص إنسانية وترندات ومواضيع مجتمعية",
            queryTopicAr = "أخبار الثقافة والفنون والمجتمع والقصص الملهمة اليوم",
            rssFeedUrl = "https://news.google.com/rss/headlines/section/topic/ENTERTAINMENT?hl=ar&gl=SA&ceid=SA:ar",
            accentIndex = 4,
            iconKey = "culture"
        )
    )

    fun categoriesByIds(ids: Set<String>): List<NewsCategory> {
        val resolved = newsCategories.filter { it.id in ids }
        return resolved.ifEmpty { newsCategories.filter { it.id in defaultSelectedCategoryIds } }
    }

    val defaultChannels: List<RadioChannel> = listOf(
        RadioChannel(
            id = "breaking_general",
            nameAr = "أثير العاجلة",
            frequencyLabel = "98.4 FM",
            taglineAr = "تغطية فورية لأهم الأخبار العاجلة والتقارير العربية والعالمية على مدار الساعة",
            categoryAr = "أخبار عاجلة",
            topicPrompt = "أهم الأخبار العاجلة والتقارير اليومية السياسية والدولية والعربية الأبرز اليوم من مختلف وكالات وقنوات الأخبار",
            anchorIds = listOf("tariq", "layla", "karim"),
            accentIndex = 0
        ),
        RadioChannel(
            id = "economy_markets",
            nameAr = "أثير الاقتصاد",
            frequencyLabel = "101.2 FM",
            taglineAr = "الأسواق العالمية، النفط والذهب، البنوك المركزية، وأبرز صفقات الأعمال اليوم",
            categoryAr = "اقتصاد وأعمال",
            topicPrompt = "أحدث أخبار الاقتصاد العالمي والعربي اليوم، أسواق المال، أسعار النفط والذهب، التضخم، والاستثمار وريادة الأعمال",
            anchorIds = listOf("karim", "tariq"),
            accentIndex = 1
        ),
        RadioChannel(
            id = "tech_ai",
            nameAr = "أثير التقنية والذكاء",
            frequencyLabel = "104.6 FM",
            taglineAr = "نبض الذكاء الاصطناعي، الشركات التقنية الكبرى، الهواتف والابتكارات العلمية اليومية",
            categoryAr = "تقنية وAI",
            topicPrompt = "أحدث أخبار التكنولوجيا والذكاء الاصطناعي اليوم، إطلاقات الشركات التقنية الكبرى، الأمن السيبراني، والابتكارات العلمية",
            anchorIds = listOf("karim", "layla"),
            accentIndex = 2
        ),
        RadioChannel(
            id = "sports_live",
            nameAr = "أثير الرياضة",
            frequencyLabel = "95.8 FM",
            taglineAr = "نتائج المباريات، الدوريات الأوروبية والعربية، انتقالات اللاعبين وتحليل الأستوديو",
            categoryAr = "رياضة",
            topicPrompt = "أهم الأخبار الرياضية اليوم، نتائج وتحليلات مباريات كرة القدم الأوروبية والعربية والبطولات العالمية الكبرى",
            anchorIds = listOf("tariq", "karim"),
            accentIndex = 3
        ),
        RadioChannel(
            id = "science_culture",
            nameAr = "أثير المعرفة والثقافة",
            frequencyLabel = "91.0 FM",
            taglineAr = "تقارير صحية وعلمية وفضاء وقصص إنسانية وثقافية بأسلوب بودكاست هادئ",
            categoryAr = "علوم وثقافة",
            topicPrompt = "أحدث الاكتشافات العلمية، أخبار الفضاء، الصحة والطب، الثقافة والمجتمع والقصص الملهمة اليوم",
            anchorIds = listOf("layla", "tariq"),
            accentIndex = 4
        )
    )

    fun buildDailySchedule(): List<DailyScheduleSlot> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        data class RawSlot(
            val startHour: Int,
            val endHour: Int,
            val timeLabel: String,
            val title: String,
            val desc: String,
            val hosts: String,
            val cat: String
        )
        val raw = listOf(
            RawSlot(0, 6, "00:00 - 06:00", "حصاد الليل والعاجل الدولي", "متابعة متواصلة لتطورات الليل وافتتاح الأسواق الآسيوية", "طارق وليلى", "بث ليلي"),
            RawSlot(6, 10, "06:00 - 10:00", "صباح أثير الإخباري", "أبرز عناوين الصحف الصباحية والطقس والاقتصاد في جرعة مكثفة", "طارق وكريم", "صباحي"),
            RawSlot(10, 14, "10:00 - 14:00", "نبض الأسواق والتقنية", "حوار تحليلي حي حول الاقتصاد العالمي وقفزات الذكاء الاصطناعي", "كريم وليلى", "اقتصاد وتقنية"),
            RawSlot(14, 18, "14:00 - 18:00", "المنتصف: ملفات وتقارير", "قراءة معمّقة في أهم القضايا الساخنة وتقارير المراسلين", "طارق وليلى وكريم", "ملف خاص"),
            RawSlot(18, 22, "18:00 - 22:00", "المسائية وبودكاست الحدث", "نشرة المساء الرئيسية متبوعة بنقاش بودكاست شامل بين المذيعين", "طارق وليلى", "الرئيسية"),
            RawSlot(22, 24, "22:00 - 00:00", "أمسية الرياضة والعلوم", "خلاصة مباريات اليوم وأبرز الأخبار العلمية والثقافية", "كريم وليلى", "منوعات")
        )
        return raw.map { slot ->
            DailyScheduleSlot(
                timeLabel = slot.timeLabel,
                programNameAr = slot.title,
                descriptionAr = slot.desc,
                hostNamesAr = slot.hosts,
                categoryAr = slot.cat,
                isLiveNow = currentHour in slot.startHour until slot.endHour
            )
        }
    }
}

object RadioJsonSerializer {
    fun serializeEpisodePayload(episode: BroadcastEpisode): String {
        val root = JSONObject()
        val headlines = JSONArray()
        episode.breakingTickerHeadlines.forEach { headlines.put(it) }
        root.put("breakingTickerHeadlines", headlines)

        val turnsArr = JSONArray()
        episode.turns.forEach { turn ->
            turnsArr.put(
                JSONObject().apply {
                    put("id", turn.id)
                    put("segmentIndex", turn.segmentIndex)
                    put("turnIndex", turn.turnIndex)
                    put("segmentTitleAr", turn.segmentTitleAr)
                    put("segmentType", turn.segmentType.name)
                    put("anchorId", turn.anchorId)
                    put("textAr", turn.textAr)
                    put("emotionCue", turn.emotionCue)
                    put("headlineTag", turn.headlineTag)
                }
            )
        }
        root.put("turns", turnsArr)

        val sourcesArr = JSONArray()
        episode.sources.forEach { src ->
            sourcesArr.put(
                JSONObject().apply {
                    put("title", src.title)
                    put("url", src.url)
                    put("domain", src.domain)
                }
            )
        }
        root.put("sources", sourcesArr)
        root.put("isBreakingFlash", episode.isBreakingFlash)
        return root.toString()
    }

    fun deserializeEpisodePayload(
        id: String,
        channelId: String,
        channelNameAr: String,
        channelFrequency: String,
        programTitleAr: String,
        broadcastStyleId: String,
        generatedAtMillis: Long,
        payloadJson: String
    ): BroadcastEpisode {
        val root = JSONObject(payloadJson)
        val headlines = mutableListOf<String>()
        root.optJSONArray("breakingTickerHeadlines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val h = arr.optString(i)
                if (h.isNotBlank()) headlines.add(h)
            }
        }
        val turns = mutableListOf<DialogueTurn>()
        root.optJSONArray("turns")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                turns.add(
                    DialogueTurn(
                        id = obj.optString("id", "turn_$i"),
                        segmentIndex = obj.optInt("segmentIndex", 0),
                        turnIndex = obj.optInt("turnIndex", i),
                        segmentTitleAr = obj.optString("segmentTitleAr", "فقرة إخبارية"),
                        segmentType = SegmentType.fromString(obj.optString("segmentType", "DIALOGUE")),
                        anchorId = obj.optString("anchorId", "tariq"),
                        textAr = obj.optString("textAr", ""),
                        emotionCue = obj.optString("emotionCue", "متزن"),
                        headlineTag = obj.optString("headlineTag", "نشرة اليوم")
                    )
                )
            }
        }
        val sources = mutableListOf<NewsSource>()
        root.optJSONArray("sources")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                sources.add(
                    NewsSource(
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        domain = obj.optString("domain", "")
                    )
                )
            }
        }
        return BroadcastEpisode(
            id = id,
            channelId = channelId,
            channelNameAr = channelNameAr,
            channelFrequency = channelFrequency,
            programTitleAr = programTitleAr,
            broadcastStyle = BroadcastStyle.fromId(broadcastStyleId),
            generatedAtMillis = generatedAtMillis,
            breakingTickerHeadlines = headlines,
            turns = turns,
            sources = sources,
            isBreakingFlash = root.optBoolean("isBreakingFlash", false)
        )
    }

    fun serializeCustomChannels(channels: List<RadioChannel>): String {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("nameAr", ch.nameAr)
                    put("frequencyLabel", ch.frequencyLabel)
                    put("taglineAr", ch.taglineAr)
                    put("categoryAr", ch.categoryAr)
                    put("topicPrompt", ch.topicPrompt)
                    put("accentIndex", ch.accentIndex)
                    val aIds = JSONArray()
                    ch.anchorIds.forEach { aIds.put(it) }
                    put("anchorIds", aIds)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeCustomChannels(rawJson: String): List<RadioChannel> {
        if (rawJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val aIds = mutableListOf<String>()
                obj.optJSONArray("anchorIds")?.let { aArr ->
                    for (j in 0 until aArr.length()) {
                        aIds.add(aArr.getString(j))
                    }
                }
                RadioChannel(
                    id = obj.getString("id"),
                    nameAr = obj.getString("nameAr"),
                    frequencyLabel = obj.optString("frequencyLabel", "107.5 FM"),
                    taglineAr = obj.optString("taglineAr", ""),
                    categoryAr = obj.optString("categoryAr", "قناة مخصصة"),
                    topicPrompt = obj.optString("topicPrompt", ""),
                    anchorIds = if (aIds.isEmpty()) listOf("tariq", "layla") else aIds,
                    accentIndex = obj.optInt("accentIndex", 2),
                    isCustom = true
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
