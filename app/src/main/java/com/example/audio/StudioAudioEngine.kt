package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.ai.SynthesizedAudioClip
import com.example.data.model.RadioAnchor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "StudioAudioEngine"
private const val SPECTRUM_BAR_COUNT = 22

class StudioAudioEngine(context: Context) {

    private val appContext = context.applicationContext

    private val _spectrumLevels = MutableStateFlow(List(SPECTRUM_BAR_COUNT) { 0.08f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    @Volatile
    private var currentTrack: AudioTrack? = null

    @Volatile
    private var isPausedFlag: Boolean = false

    @Volatile
    private var playbackSpeed: Float = 1.0f

    private var deviceTts: TextToSpeech? = null
    private var ttsReadyDeferred: CompletableDeferred<Boolean>? = null

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.85f, 1.5f)
        val track = currentTrack
        if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
            try {
                track.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to update live playback speed", e)
            }
        }
    }

    fun pause() {
        isPausedFlag = true
        try {
            currentTrack?.pause()
        } catch (_: Exception) {
        }
        try {
            deviceTts?.stop()
        } catch (_: Exception) {
        }
        resetSpectrum()
    }

    fun resume() {
        isPausedFlag = false
        try {
            val track = currentTrack
            if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
            }
        } catch (_: Exception) {
        }
    }

    fun stopCurrentAudio() {
        isPausedFlag = false
        val track = currentTrack
        currentTrack = null
        if (track != null) {
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING ||
                    track.playState == AudioTrack.PLAYSTATE_PAUSED
                ) {
                    track.pause()
                    track.flush()
                    track.stop()
                }
                track.release()
            } catch (_: Exception) {
            }
        }
        try {
            deviceTts?.stop()
        } catch (_: Exception) {
        }
        resetSpectrum()
    }

    suspend fun playBroadcastChime() = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 24000
            // 3-note warm newsroom marimba/bell chord (C5 -> E5 -> G5)
            val notes = listOf(523.25, 659.25, 783.99)
            val noteDurationSec = 0.16
            val tailSec = 0.34
            val totalSamples = ((notes.size * noteDurationSec + tailSec) * sampleRate).toInt()
            val pcm = ShortArray(totalSamples)

            notes.forEachIndexed { idx, freq ->
                val startSample = (idx * noteDurationSec * sampleRate).toInt()
                val noteLen = ((noteDurationSec + tailSec) * sampleRate).toInt()
                for (i in 0 until noteLen) {
                    val pos = startSample + i
                    if (pos >= totalSamples) break
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 6.5) * min(1.0, t / 0.008)
                    val fundamental = sin(2.0 * PI * freq * t)
                    val harmonic = 0.32 * sin(2.0 * PI * (freq * 2.0) * t)
                    val sample = (fundamental + harmonic) * env * 0.28
                    val mixed = (pcm[pos] / 32767.0 + sample).coerceIn(-0.95, 0.95)
                    pcm[pos] = (mixed * 32767.0).toInt().toShort()
                }
            }

            val bytes = ByteArray(pcm.size * 2)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm)
            playPcmClip(
                SynthesizedAudioClip(
                    audioBytes = bytes,
                    sampleRateHz = sampleRate,
                    mimeType = "audio/L16;codec=pcm;rate=$sampleRate"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Broadcast chime skipped", e)
        }
    }

    suspend fun playPcmClip(clip: SynthesizedAudioClip) = withContext(Dispatchers.IO) {
        stopCurrentAudio()

        val parsed = unwrapWavIfNeeded(clip.audioBytes, clip.sampleRateHz)
        val pcmBytes = parsed.pcmBytes
        val sampleRate = parsed.sampleRateHz
        val channelMask = if (parsed.channels == 2) {
            AudioFormat.CHANNEL_OUT_STEREO
        } else {
            AudioFormat.CHANNEL_OUT_MONO
        }

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        currentTrack = track
        try {
            if (playbackSpeed != 1.0f) {
                try {
                    track.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                } catch (_: Exception) {
                }
            }
            track.play()

            val chunkSize = 2048
            var offset = 0
            var phase = 0f

            while (offset < pcmBytes.size && isActive && currentTrack === track) {
                while (isPausedFlag && isActive && currentTrack === track) {
                    resetSpectrum()
                    delay(80)
                }
                if (!isActive || currentTrack !== track) break

                val toWrite = min(chunkSize, pcmBytes.size - offset)
                val written = track.write(pcmBytes, offset, toWrite, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break

                val rms = computeRmsEnergy(pcmBytes, offset, written)
                phase += 0.45f
                updateReactiveSpectrum(rms, phase)
                offset += written
            }

            // Allow hardware buffer to drain cleanly
            if (isActive && currentTrack === track) {
                delay(180)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack playback error", e)
            throw e
        } finally {
            resetSpectrum()
            if (currentTrack === track) {
                currentTrack = null
            }
            try {
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                }
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun speakWithDeviceFallback(anchor: RadioAnchor, arabicText: String): Boolean =
        withContext(Dispatchers.Main) {
            val ready = ensureDeviceTtsReady()
            if (!ready) return@withContext false

            val tts = deviceTts ?: return@withContext false
            val done = CompletableDeferred<Boolean>()
            val utteranceId = "atheer_${UUID.randomUUID()}"

            tts.setPitch(anchor.fallbackPitch)
            tts.setSpeechRate(anchor.fallbackRate * playbackSpeed)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) done.complete(true)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId) done.complete(false)
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId) done.complete(false)
                }
            })

            val params = Bundle()
            val speakResult = tts.speak(arabicText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (speakResult != TextToSpeech.SUCCESS) {
                return@withContext false
            }

            // Drive gentle animated waveform while device TTS speaks
            var tick = 0f
            try {
                while (!done.isCompleted && isActive) {
                    tick += 0.35f
                    updateReactiveSpectrum(0.42f, tick)
                    delay(70)
                }
                done.await()
            } catch (e: CancellationException) {
                tts.stop()
                throw e
            } finally {
                resetSpectrum()
            }
        }

    private suspend fun ensureDeviceTtsReady(): Boolean {
        if (deviceTts != null && ttsReadyDeferred?.isCompleted == true) {
            return ttsReadyDeferred?.await() == true
        }
        val deferred = CompletableDeferred<Boolean>()
        ttsReadyDeferred = deferred
        deviceTts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val arabicLocales = listOf(
                    Locale("ar"),
                    Locale("ar", "SA"),
                    Locale("ar", "EG"),
                    Locale("ar", "AE")
                )
                var ready = false
                for (loc in arabicLocales) {
                    val res = deviceTts?.setLanguage(loc)
                    if (res != TextToSpeech.LANG_MISSING_DATA &&
                        res != TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        ready = true
                        break
                    }
                }
                deferred.complete(ready)
            } else {
                deferred.complete(false)
            }
        }
        return deferred.await()
    }

    fun release() {
        stopCurrentAudio()
        try {
            deviceTts?.shutdown()
            deviceTts = null
        } catch (_: Exception) {
        }
    }

    private fun computeRmsEnergy(bytes: ByteArray, offset: Int, length: Int): Float {
        var sumSq = 0.0
        var samples = 0
        var i = offset
        val end = min(bytes.size - 1, offset + length - 1)
        while (i < end) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val normalized = sample / 32768.0
            sumSq += normalized * normalized
            samples++
            i += 2
        }
        if (samples == 0) return 0.08f
        val rms = sqrt(sumSq / samples).toFloat()
        return (rms * 3.8f).coerceIn(0.08f, 1.0f)
    }

    private fun updateReactiveSpectrum(rms: Float, phase: Float) {
        val center = SPECTRUM_BAR_COUNT / 2f
        val bars = List(SPECTRUM_BAR_COUNT) { idx ->
            val dist = abs(idx - center) / center
            val bell = (1f - dist * 0.65f).coerceAtLeast(0.2f)
            val wave = 0.65f + 0.35f * sin(phase + idx * 0.55f).toFloat()
            (rms * bell * wave).coerceIn(0.08f, 1.0f)
        }
        _spectrumLevels.value = bars
    }

    private fun resetSpectrum() {
        _spectrumLevels.value = List(SPECTRUM_BAR_COUNT) { 0.08f }
    }

    private data class UnwrappedPcm(
        val pcmBytes: ByteArray,
        val sampleRateHz: Int,
        val channels: Int
    )

    private fun unwrapWavIfNeeded(raw: ByteArray, fallbackRate: Int): UnwrappedPcm {
        if (raw.size > 44 &&
            raw[0] == 'R'.code.toByte() &&
            raw[1] == 'I'.code.toByte() &&
            raw[2] == 'F'.code.toByte() &&
            raw[3] == 'F'.code.toByte()
        ) {
            val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
            val channels = buf.getShort(22).toInt().coerceIn(1, 2)
            val sampleRate = buf.getInt(24).takeIf { it in 8000..96000 } ?: fallbackRate
            // Locate "data" subchunk
            var pos = 12
            while (pos + 8 < raw.size) {
                val id = String(raw, pos, 4, Charsets.US_ASCII)
                val size = buf.getInt(pos + 4)
                if (id == "data") {
                    val dataStart = pos + 8
                    val dataEnd = min(raw.size, dataStart + size.coerceAtLeast(0))
                    return UnwrappedPcm(
                        pcmBytes = raw.copyOfRange(dataStart, dataEnd),
                        sampleRateHz = sampleRate,
                        channels = channels
                    )
                }
                pos += 8 + size.coerceAtLeast(0)
            }
            return UnwrappedPcm(raw.copyOfRange(44, raw.size), sampleRate, channels)
        }
        return UnwrappedPcm(raw, fallbackRate, 1)
    }
}
