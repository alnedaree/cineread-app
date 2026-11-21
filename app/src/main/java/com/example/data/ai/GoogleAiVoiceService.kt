package com.example.data.ai

import android.util.Log
import com.example.data.model.RadioAnchor
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GoogleAiVoiceService"
private const val GEMINI_TTS_MODEL = "gemini-3.1-flash-tts-preview"

data class SynthesizedAudioClip(
    val audioBytes: ByteArray,
    val sampleRateHz: Int = 24000,
    val mimeType: String = "audio/L16;codec=pcm;rate=24000"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynthesizedAudioClip) return false
        return sampleRateHz == other.sampleRateHz &&
            mimeType == other.mimeType &&
            audioBytes.contentEquals(other.audioBytes)
    }

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + sampleRateHz
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

class GoogleAiVoiceService {

    // Lazily cached GenerativeModel per Google AI Voice profile
    private val voiceModels = ConcurrentHashMap<String, GenerativeModel>()

    @OptIn(PublicPreviewAPI::class)
    private fun getVoiceModel(voiceName: String): GenerativeModel {
        return voiceModels.getOrPut(voiceName) {
            Firebase.ai.generativeModel(
                modelName = GEMINI_TTS_MODEL,
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                    speechConfig = SpeechConfig(voice = Voice(voiceName))
                }
            )
        }
    }

    suspend fun synthesizeBroadcastLine(
        anchor: RadioAnchor,
        arabicText: String,
        emotionCue: String = ""
    ): SynthesizedAudioClip {
        val model = getVoiceModel(anchor.googleVoiceName)
        val styleNote = if (emotionCue.isNotBlank()) {
            "${anchor.speakingStyleInstruction} Delivery mood: $emotionCue."
        } else {
            anchor.speakingStyleInstruction
        }

        val prompt = """
        Say in a clear, natural Arabic radio news broadcast voice ($emotionCue):
        $arabicText
        """.trimIndent()

        try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }

            val output = ByteArrayOutputStream()
            var detectedMime = "audio/L16;codec=pcm;rate=24000"

            for (candidate in response.candidates) {
                for (part in candidate.content.parts) {
                    if (part is InlineDataPart &&
                        (part.mimeType.startsWith("audio/", ignoreCase = true) || part.mimeType.isBlank())
                    ) {
                        if (part.mimeType.isNotBlank()) {
                            detectedMime = part.mimeType
                        }
                        output.write(part.inlineData)
                    }
                }
            }

            val bytes = output.toByteArray()
            if (bytes.isEmpty()) {
                val partTypes = response.candidates.flatMap { it.content.parts }
                    .joinToString { it::class.java.simpleName }
                Log.e(TAG, "Empty audio from $GEMINI_TTS_MODEL; candidates=${response.candidates.size}, parts=[$partTypes]")
                throw IllegalStateException("لم يُرجع محرك Google AI Voice بيانات صوتية لهذه الفقرة.")
            }

            val sampleRate = parseSampleRateFromMime(detectedMime)
            Log.d(TAG, "Synthesized ${bytes.size} bytes via $GEMINI_TTS_MODEL (${anchor.googleVoiceName}, $detectedMime, ${sampleRate}Hz)")
            return SynthesizedAudioClip(
                audioBytes = bytes,
                sampleRateHz = sampleRate,
                mimeType = detectedMime
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Google AI Voice synthesis timed out for ${anchor.googleVoiceName}", e)
            throw IllegalStateException("انتهت مهلة توليد الصوت عبر Google AI Voice.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google AI Voice synthesis failed for ${anchor.googleVoiceName}", e)
            throw e
        }
    }

    private fun parseSampleRateFromMime(mimeType: String): Int {
        val match = Regex("rate=(\\d+)").find(mimeType)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24000
    }
}
