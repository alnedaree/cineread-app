package com.example.data.ai

import android.util.Log
import com.example.data.model.BroadcastEpisode
import com.example.data.model.BroadcastStyle
import com.example.data.model.DialogueTurn
import com.example.data.model.NewsCategory
import com.example.data.model.NewsSource
import com.example.data.model.RadioCatalog
import com.example.data.model.RadioChannel
import com.example.data.model.SegmentType
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "NewsroomAiService"

// High-volume free-tier friendly primary model, with cross-model fallback on quota/rate-limit errors
private const val PRIMARY_PRODUCER_MODEL = "gemini-3.5-flash-lite"
private const val FALLBACK_PRODUCER_MODEL = "gemini-3.8-flash"

data class GatheredNewsBrief(
    val summaryText: String,
    val sources: List<NewsSource>
)

class NewsroomAiService {

    private val rssFetcher = LiveRssNewsFetcher()

    // Executive Radio Producer returning strict structured JSON for multi-anchor broadcast
    private val broadcastSchema: Schema = Schema.obj(
        properties = mapOf(
            "programTitleAr" to Schema.string(
                description = "Catchy Arabic radio news program title for this live hour"
            ),
            "breakingTickerHeadlines" to Schema.array(
                items = Schema.string(),
                description = "4 to 5 short, punchy Arabic breaking-news ticker headlines"
            ),
            "turns" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "segmentIndex" to Schema.integer(description = "0-based segment index"),
                        "segmentTitleAr" to Schema.string(description = "Arabic title of the broadcast segment"),
                        "segmentType" to Schema.string(
                            description = "One of: INTRO, BREAKING, DIALOGUE, ROUNDUP"
                        ),
                        "anchorId" to Schema.string(
                            description = "One of the provided anchor IDs: tariq, layla, or karim"
                        ),
                        "textAr" to Schema.string(
                            description = "Natural spoken Arabic radio broadcast line (2 to 4 crisp sentences, ready for TTS voice reading, no bullet symbols or markdown)"
                        ),
                        "emotionCue" to Schema.string(
                            description = "Short Arabic vocal delivery cue like حاسم وواضح or حواري دافئ"
                        ),
                        "headlineTag" to Schema.string(
                            description = "Short 3-6 word Arabic topic tag for the on-screen teleprompter banner"
                        )
                    )
                ),
                description = "4 alternating radio turns divided naturally between the channel's anchors"
            )
        )
    )

    private val primaryStructuredProducer by lazy {
        buildProducerModel(PRIMARY_PRODUCER_MODEL, withSchema = true)
    }

    private val fallbackStructuredProducer by lazy {
        buildProducerModel(FALLBACK_PRODUCER_MODEL, withSchema = true)
    }

    private val fallbackPlainJsonProducer by lazy {
        buildProducerModel(PRIMARY_PRODUCER_MODEL, withSchema = false)
    }

    private fun buildProducerModel(modelName: String, withSchema: Boolean): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.6f
                responseMimeType = "application/json"
                if (withSchema) {
                    responseSchema = broadcastSchema
                }
            }
        )
    }

    suspend fun gatherLiveNews(
        channel: RadioChannel,
        selectedCategories: List<NewsCategory> = emptyList(),
        isBreakingFlash: Boolean,
        previousHeadlines: List<String> = emptyList()
    ): GatheredNewsBrief {
        // Primary path: Live RSS headlines directly from Arabic/Global news channels & chosen categories (0 AI quota used!)
        val rssItems = try {
            val fetched = rssFetcher.fetchLiveNewsForChannel(channel, selectedCategories)
            if (previousHeadlines.isNotEmpty()) {
                val filtered = fetched.filterNot { item ->
                    previousHeadlines.any { prev -> item.title.contains(prev.take(18), ignoreCase = true) }
                }
                filtered.ifEmpty { fetched }
            } else {
                fetched
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "RSS fetch failed: ${e.localizedMessage}")
            emptyList()
        }

        if (rssItems.isNotEmpty()) {
            Log.d(TAG, "Gathered ${rssItems.size} live RSS news items for ${channel.nameAr}")
            return rssFetcher.toNewsBrief(rssItems)
        }

        // If RSS returned empty, do NOT burn an extra Gemini RPM call just to summarize;
        // pass the topic & date context directly into the single Producer call!
        val dateStr = SimpleDateFormat("EEEE، d MMMM yyyy - HH:mm", Locale("ar")).format(Date())
        val categoriesTopic = if (channel.id == "breaking_general" && selectedCategories.isNotEmpty()) {
            selectedCategories.joinToString("، ") { "${it.nameAr} (${it.queryTopicAr})" }
        } else {
            channel.topicPrompt
        }
        return GatheredNewsBrief(
            summaryText = "التاريخ: $dateStr\nأعد نشرة إذاعية شاملة ومحدثة تغطي أبرز المستجدات اليوم في: $categoriesTopic.",
            sources = emptyList()
        )
    }

    suspend fun produceBroadcastEpisode(
        channel: RadioChannel,
        selectedCategories: List<NewsCategory> = emptyList(),
        style: BroadcastStyle,
        newsBrief: GatheredNewsBrief,
        isBreakingFlash: Boolean
    ): BroadcastEpisode {
        val activeAnchors = channel.anchorIds.map { RadioCatalog.anchorById(it) }
        val anchorDescriptions = activeAnchors.joinToString("\n") { a ->
            "- anchorId=\"${a.id}\" : ${a.nameAr} (${a.roleAr})"
        }

        val categoryFocusLine = if (selectedCategories.isNotEmpty()) {
            "التصنيفات المفضلة لدى المستمع (أعطها الأولوية وغطّها في فقرات النشرة): ${selectedCategories.joinToString("، ") { it.nameAr }}."
        } else {
            ""
        }

        val styleInstructions = when (style) {
            BroadcastStyle.NEWSROOM_FORMAL ->
                "الأسلوب: نشرة إخبارية إذاعية رسمية رصينة. جمل واضحة، انتقالات احترافية بين المذيعين، تركيز على الدقة والخبر العاجل."
            BroadcastStyle.PODCAST_DIALOGUE ->
                "الأسلوب: برنامج راديو بودكاست إخباري تفاعلي شيّق. يتبادل المذيعون الحديث بتلقائية راقية، يطرح أحدهم سؤالاً تحليلياً ويجيب الآخر، مع حيوية إذاعية."
            BroadcastStyle.RAPID_FLASH ->
                "الأسلوب: موجز أخبار عاجل وسريع الإيقاع. فقرات قصيرة ومباشرة ومكثفة تغطي أكبر قدر من المستجدات في وقت قياسي."
        }

        val turnCountInstruction = if (isBreakingFlash) {
            "أنشئ 3 مداخلات (turns) سريعة وعاجلة تبدأ بتنبيه عاجل وتغطي التطورات فوراً."
        } else {
            "أنشئ 4 مداخلات (turns) غنية ومتكاملة موزعة بالتناوب بين المذيعين المتاحين: (1) INTRO استهلال وعناوين الساعة، (2) BREAKING أهم الأخبار العاجلة، (3) DIALOGUE حوار الأستوديو والتحليل، (4) ROUNDUP موجز ختامي ومتابعة."
        }

        val prompt = """
        أنت المنتج التنفيذي ومعد البرامج الإذاعية في شبكة "أثير الأخبار".
        القناة: ${channel.nameAr} (${channel.frequencyLabel})
        $categoryFocusLine
        المذيعون المشاركون في هذه الحلقة (استخدم فقط قيم anchorId التالية):
        $anchorDescriptions

        $styleInstructions
        $turnCountInstruction

        قواعد صارمة لكتابة النص الإذاعي الصوتي (textAr):
        - اكتب بلغة عربية فصحى معاصرة سلسة ومناسبة تماماً للقراءة الصوتية المباشرة عبر Google AI Voice.
        - لا تستخدم علامات النجمة (*) أو الهاشتاغ (#) أو الرموز التعبيرية أو الأقواس الإخراجية داخل textAr أبداً.
        - اجعل كل مداخلة بين 30 و 55 كلمة (3 إلى 4 جمل إذاعية واضحة وغنية بالمعلومة) حتى يكون البرنامج متماسكاً وممتعاً.
        - احرص على ذكر اسم القناة "${channel.nameAr}" في المداخلة الأولى، واستخدم انتقالات طبيعية بين المذيعين بأسمائهم.
        - يجب أن تكون قيمة segmentType واحدة فقط من: INTRO أو BREAKING أو DIALOGUE أو ROUNDUP.

        المادة الإخبارية المرصودة اليوم لبناء البرنامج عليها:
        ${newsBrief.summaryText}
        """.trimIndent()

        val rawJson = generateWithQuotaAwareFallback(prompt, activeAnchors.first().id)

        val cleanedJson = extractJsonObject(rawJson)
        val root = JSONObject(cleanedJson)
        val programTitleAr = root.optString(
            "programTitleAr",
            if (isBreakingFlash) "نشرة عاجلة — ${channel.nameAr}" else "البث المباشر — ${channel.nameAr}"
        )

        val headlines = mutableListOf<String>()
        root.optJSONArray("breakingTickerHeadlines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotBlank()) headlines.add(item)
            }
        }

        val allowedIds = activeAnchors.map { it.id }.toSet()
        val turns = mutableListOf<DialogueTurn>()
        val turnsArr = root.optJSONArray("turns")
            ?: throw IllegalStateException("تعذر قراءة فقرات البرنامج الإذاعي.")

        for (i in 0 until turnsArr.length()) {
            val obj = turnsArr.getJSONObject(i)
            val rawAnchor = obj.optString("anchorId", activeAnchors.first().id)
            val resolvedAnchor = if (rawAnchor in allowedIds) rawAnchor else activeAnchors[i % activeAnchors.size].id
            val cleanText = obj.optString("textAr", "")
                .replace("*", "")
                .replace("#", "")
                .trim()
            if (cleanText.isNotBlank()) {
                turns.add(
                    DialogueTurn(
                        id = "t_${UUID.randomUUID().toString().take(8)}_$i",
                        segmentIndex = obj.optInt("segmentIndex", i),
                        turnIndex = turns.size,
                        segmentTitleAr = obj.optString("segmentTitleAr", "فقرة إخبارية"),
                        segmentType = SegmentType.fromString(obj.optString("segmentType", "DIALOGUE")),
                        anchorId = resolvedAnchor,
                        textAr = cleanText,
                        emotionCue = obj.optString("emotionCue", "إذاعي متزن"),
                        headlineTag = obj.optString("headlineTag", channel.categoryAr)
                    )
                )
            }
        }

        if (turns.isEmpty()) {
            throw IllegalStateException("لم تتوفر فقرات جاهزة للبث، يرجى المحاولة مرة أخرى.")
        }

        return BroadcastEpisode(
            id = "ep_${System.currentTimeMillis()}",
            channelId = channel.id,
            channelNameAr = channel.nameAr,
            channelFrequency = channel.frequencyLabel,
            programTitleAr = programTitleAr,
            broadcastStyle = style,
            generatedAtMillis = System.currentTimeMillis(),
            breakingTickerHeadlines = headlines.ifEmpty {
                listOf("متابعة مباشرة لآخر التطورات على ${channel.nameAr}")
            },
            turns = turns,
            sources = newsBrief.sources,
            isBreakingFlash = isBreakingFlash
        )
    }

    private suspend fun generateWithQuotaAwareFallback(prompt: String, defaultAnchorId: String): String {
        // Attempt 1: High-volume gemini-3.5-flash-lite with structured JSON schema (single request)
        val firstError: Exception = try {
            val response = withTimeout(55_000L) {
                primaryStructuredProducer.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) return text
            IllegalStateException("لم يُرجع الذكاء الاصطناعي نص البرنامج.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary producer timed out", e)
            throw IllegalStateException("استغرق إعداد البرنامج وقتاً أطول من المعتاد، يرجى المحاولة مجدداً.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Primary model ($PRIMARY_PRODUCER_MODEL) failed: ${e.localizedMessage}")
            e
        }

        // If primary failed due to quota/rate-limit/model-availability, switch to alternate model bucket (gemini-3.8-flash)
        // If primary failed due to schema strictness (400), try plain JSON mode on primary model
        val msg = firstError.localizedMessage.orEmpty()
        val isQuotaOrRateLimit = msg.contains("quota", ignoreCase = true) ||
            msg.contains("rate", ignoreCase = true) ||
            msg.contains("429", ignoreCase = true) ||
            msg.contains("resource_exhausted", ignoreCase = true) ||
            msg.contains("not found", ignoreCase = true)

        return try {
            if (isQuotaOrRateLimit) {
                Log.d(TAG, "Switching to alternate model bucket ($FALLBACK_PRODUCER_MODEL)")
                val fallbackResp = withTimeout(55_000L) {
                    fallbackStructuredProducer.generateContent(prompt)
                }
                fallbackResp.text?.trim().takeIf { !it.isNullOrBlank() } ?: throw firstError
            } else {
                val jsonPrompt = """
                $prompt

                أرجع كائن JSON فقط بهذا الشكل الدقيق دون أي نص إضافي:
                {
                  "programTitleAr": "عنوان البرنامج",
                  "breakingTickerHeadlines": ["خبر 1", "خبر 2", "خبر 3", "خبر 4"],
                  "turns": [
                    {
                      "segmentIndex": 0,
                      "segmentTitleAr": "استهلال وعناوين الساعة",
                      "segmentType": "INTRO",
                      "anchorId": "$defaultAnchorId",
                      "textAr": "نص المداخلة الإذاعية هنا",
                      "emotionCue": "إذاعي متزن",
                      "headlineTag": "عناوين الساعة"
                    }
                  ]
                }
                """.trimIndent()
                val retryResp = withTimeout(55_000L) {
                    fallbackPlainJsonProducer.generateContent(jsonPrompt)
                }
                retryResp.text?.trim().takeIf { !it.isNullOrBlank() } ?: throw firstError
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fallback producer also failed: ${e.localizedMessage}", e)
            throw e
        }
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }
}
