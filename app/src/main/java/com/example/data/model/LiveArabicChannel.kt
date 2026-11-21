package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class LiveMediaMode(val id: String) {
    VIDEO("video"),
    RADIO_AUDIO("radio_audio");

    companion object {
        fun fromId(id: String?): LiveMediaMode =
            entries.firstOrNull { it.id == id } ?: VIDEO
    }
}

enum class ArabicChannelRegion(val labelAr: String) {
    ALL("الكل"),
    PAN_ARAB("إخبارية عربية"),
    YEMEN("قنوات يمنية"),
    GLOBAL_ARABIC("دولية واقتصاد")
}

data class LiveArabicChannel(
    val id: String,
    val nameAr: String,
    val shortBadgeAr: String,
    val taglineAr: String,
    val countryAr: String,
    val region: ArabicChannelRegion,
    val youtubeHandle: String,
    val knownChannelId: String? = null,
    val customVideoId: String? = null,
    val accentIndex: Int = 0,
    val isCustom: Boolean = false
) {
    val youtubeLivePageUrl: String
        get() = when {
            !customVideoId.isNullOrBlank() -> "https://www.youtube.com/watch?v=$customVideoId"
            youtubeHandle.startsWith("http", ignoreCase = true) -> youtubeHandle
            youtubeHandle.startsWith("@") -> "https://www.youtube.com/$youtubeHandle/live"
            !knownChannelId.isNullOrBlank() -> "https://www.youtube.com/channel/$knownChannelId/live"
            else -> "https://www.youtube.com/@$youtubeHandle/live"
        }
}

object LiveArabicChannelsCatalog {
    val officialChannels: List<LiveArabicChannel> = listOf(
        LiveArabicChannel(
            id = "yt_aljazeera",
            nameAr = "قناة الجزيرة",
            shortBadgeAr = "الجزيرة",
            taglineAr = "بث مباشر على مدار الساعة للأخبار العاجلة والتغطيات العربية والعالمية",
            countryAr = "قطر • عربي",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@aljazeera",
            knownChannelId = "UCfiwzLy-8yKzIbsmZTzxDgw",
            accentIndex = 1
        ),
        LiveArabicChannel(
            id = "yt_aljazeera_mubasher",
            nameAr = "الجزيرة مباشر",
            shortBadgeAr = "مباشر",
            taglineAr = "نقل حي ومفتوح للمؤتمرات والأحداث الميدانية والملفات الساخنة",
            countryAr = "قطر • مباشر",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@aljazeeramubasher",
            accentIndex = 0
        ),
        LiveArabicChannel(
            id = "yt_alarabiya",
            nameAr = "قناة العربية",
            shortBadgeAr = "العربية",
            taglineAr = "تغطية إخبارية شاملة ونشرات سياسية واقتصادية مباشرة",
            countryAr = "السعودية • عربي",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@AlArabiya",
            accentIndex = 2
        ),
        LiveArabicChannel(
            id = "yt_alhadath",
            nameAr = "قناة الحدث",
            shortBadgeAr = "الحدث",
            taglineAr = "متابعة لحظية للأخبار العاجلة والتطورات الميدانية في المنطقة",
            countryAr = "السعودية • عاجل",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@AlHadath",
            accentIndex = 0
        ),
        LiveArabicChannel(
            id = "yt_skynews_arabia",
            nameAr = "سكاي نيوز عربية",
            shortBadgeAr = "سكاي",
            taglineAr = "أخبار العالم والشرق الأوسط والرياضة والاقتصاد ببث حي مستمر",
            countryAr = "الإمارات • عربي",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@SkyNewsArabia",
            accentIndex = 2
        ),
        LiveArabicChannel(
            id = "yt_yemen_shabab",
            nameAr = "قناة يمن شباب",
            shortBadgeAr = "يمن شباب",
            taglineAr = "بث مباشر لتغطية مستجدات الشأن اليمني ونشرات الأخبار والبرامج",
            countryAr = "اليمن",
            region = ArabicChannelRegion.YEMEN,
            youtubeHandle = "@TVyemenshabab",
            accentIndex = 0
        ),
        LiveArabicChannel(
            id = "yt_belqees",
            nameAr = "قناة بلقيس",
            shortBadgeAr = "بلقيس",
            taglineAr = "صوت اليمنيين — تغطيات ميدانية ونشرات إخبارية يمنية مباشرة",
            countryAr = "اليمن",
            region = ArabicChannelRegion.YEMEN,
            youtubeHandle = "@BelqeesTV",
            accentIndex = 1
        ),
        LiveArabicChannel(
            id = "yt_almahriah",
            nameAr = "قناة المهرية",
            shortBadgeAr = "المهرية",
            taglineAr = "أخبار اليمن والمحافظات والبرامج الحوارية المباشرة",
            countryAr = "اليمن",
            region = ArabicChannelRegion.YEMEN,
            youtubeHandle = "@AlmahriahTV",
            accentIndex = 3
        ),
        LiveArabicChannel(
            id = "yt_asharq",
            nameAr = "الشرق للأخبار",
            shortBadgeAr = "الشرق",
            taglineAr = "تغطية معمّقة للأخبار السياسية والاقتصاد والأسواق العالمية",
            countryAr = "اقتصاد وسياسة",
            region = ArabicChannelRegion.GLOBAL_ARABIC,
            youtubeHandle = "@AsharqNews",
            accentIndex = 1
        ),
        LiveArabicChannel(
            id = "yt_alaraby",
            nameAr = "التلفزيون العربي",
            shortBadgeAr = "العربي",
            taglineAr = "نشرات إخبارية وتحليلات سياسية وتقارير ميدانية على مدار الساعة",
            countryAr = "عربي • دولي",
            region = ArabicChannelRegion.PAN_ARAB,
            youtubeHandle = "@AlarabyTv_News",
            accentIndex = 4
        ),
        LiveArabicChannel(
            id = "yt_france24",
            nameAr = "فرانس 24 عربي",
            shortBadgeAr = "F24",
            taglineAr = "الأخبار الدولية والأوروبية والعربية على الهواء مباشرة باللغة العربية",
            countryAr = "دولي • عربي",
            region = ArabicChannelRegion.GLOBAL_ARABIC,
            youtubeHandle = "@France24_ar",
            accentIndex = 2
        )
    )

    fun serializeCustomList(channels: List<LiveArabicChannel>): String {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("nameAr", ch.nameAr)
                    put("shortBadgeAr", ch.shortBadgeAr)
                    put("taglineAr", ch.taglineAr)
                    put("countryAr", ch.countryAr)
                    put("region", ch.region.name)
                    put("youtubeHandle", ch.youtubeHandle)
                    put("knownChannelId", ch.knownChannelId.orEmpty())
                    put("customVideoId", ch.customVideoId.orEmpty())
                    put("accentIndex", ch.accentIndex)
                }
            )
        }
        return arr.toString()
    }

    fun deserializeCustomList(raw: String): List<LiveArabicChannel> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LiveArabicChannel(
                    id = obj.getString("id"),
                    nameAr = obj.getString("nameAr"),
                    shortBadgeAr = obj.optString("shortBadgeAr", "مباشر"),
                    taglineAr = obj.optString("taglineAr", "قناة يوتيوب إخبارية مباشرة"),
                    countryAr = obj.optString("countryAr", "مخصصة"),
                    region = ArabicChannelRegion.entries.firstOrNull {
                        it.name == obj.optString("region")
                    } ?: ArabicChannelRegion.PAN_ARAB,
                    youtubeHandle = obj.optString("youtubeHandle", ""),
                    knownChannelId = obj.optString("knownChannelId").ifBlank { null },
                    customVideoId = obj.optString("customVideoId").ifBlank { null },
                    accentIndex = obj.optInt("accentIndex", 0),
                    isCustom = true
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
