package com.example.Cortex_LaSecuencia.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import java.util.*
import kotlin.math.sin
import kotlin.math.PI

object AudioManager {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val sampleRate = 44100

    fun inicializar(context: Context) {
        // Inicializar TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(1.0f)
            }
        }

        isInitialized = true
    }

    fun hablar(texto: String) {
        if (!isInitialized || tts == null) return

        // Limpiar texto (remover emojis y HTML como en el código original)
        val cleanText = texto
            .replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"), "") // Emojis
            .replace("*", "")
            .replace(Regex("<[^>]*>"), " ")

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "cortex_tts")
    }

    fun reproducirSonido(tipo: TipoSonido) {
        if (!isInitialized) return

        // Generar sonido programáticamente (como en HTML con Web Audio API)
        when (tipo) {
            TipoSonido.CLICK -> {
                // Sonido click: 800Hz -> 300Hz (como en HTML)
                generarTono(800.0, 300.0, 0.1)
            }
            TipoSonido.ERROR -> {
                // Sonido error: 150Hz -> 100Hz (como en HTML)
                generarTono(150.0, 100.0, 0.2)
            }
        }
    }

    private fun generarTono(frecuenciaInicial: Double, frecuenciaFinal: Double, duracion: Double) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val numSamples = (sampleRate * duracion).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val frecuencia = frecuenciaInicial + (frecuenciaFinal - frecuenciaInicial) * (i.toDouble() / numSamples)
                val amplitud = 0.1 * (1.0 - t / duracion) // Fade out
                samples[i] = (sin(2.0 * PI * frecuencia * t) * amplitud * Short.MAX_VALUE).toInt().toShort()
            }

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Detener después de la duración
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                audioTrack.stop()
                audioTrack.release()
            }, (duracion * 1000).toLong())
        } catch (e: Exception) {
            // Ignorar errores de audio
        }
    }

    fun detener() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    enum class TipoSonido {
        CLICK,
        ERROR
    }
}

