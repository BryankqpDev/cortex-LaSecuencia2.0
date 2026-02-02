package com.example.Cortex_LaSecuencia

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

/**
 * LivenessDetector - Detector de vida para prevenir ataques de suplantación
 *
 * Detecta si el rostro es:
 * - ✅ Una persona real (live)
 * - ❌ Una foto impresa
 * - ❌ Una pantalla mostrando un video
 * - ❌ Una máscara 3D
 */
class LivenessDetector (context: Context) {

    companion object {
        // Asegúrate de tener este archivo en assets/
        private const val MODEL_FILE = "silent_face_anti_spoofing.tflite"
        private const val INPUT_SIZE = 80 // Modelo usa 80x80

        // Umbrales de decisión
        private const val REAL_THRESHOLD = 0.85f // Score > 0.85 = persona real
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.90f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.15f
    }

    private var interpreter: Interpreter? = null
    private val imageProcessor: ImageProcessor

    init {
        try {
            // Cargar modelo
            val model = FileUtil.loadMappedFile(context, MODEL_FILE)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(model, options)

            // Pipeline de preprocesamiento
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f)) // [0, 255] -> [0, 1]
                .build()

        } catch (e: Exception) {
            e.printStackTrace()
            // Importante: Manejar si el archivo .tflite no existe
            throw RuntimeException("Error al inicializar LivenessDetector. Verifica que '$MODEL_FILE' esté en la carpeta assets. Error: ${e.message}")
        }
    }

    /**
     * Detecta si el rostro en la imagen es una persona real
     * @param faceBitmap Imagen del rostro recortado
     */
    fun detectLiveness(faceBitmap: Bitmap): LivenessResult {
        try {
            // 1. Preprocesar
            var tensorImage = TensorImage.fromBitmap(faceBitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 2. Inferencia
            val output = Array(1) { FloatArray(1) }
            interpreter?.run(tensorImage.buffer, output)

            val livenessScore = output[0][0]

            // 3. Clasificar resultado
            return classifyLiveness(livenessScore)

        } catch (e: Exception) {
            e.printStackTrace()
            return LivenessResult.Error("Error en detección: ${e.message}")
        }
    }

    /**
     * Valida liveness en múltiples frames (Más robusto)
     */
    fun validateMultipleFrames(
        faceBitmaps: List<Bitmap>,
        minPassRate: Float = 0.8f
    ): MultiFrameLivenessResult {
        val results = faceBitmaps.map { detectLiveness(it) }

        val realCount = results.count { it is LivenessResult.Real }
        val totalCount = results.size
        val passRate = realCount.toFloat() / totalCount

        val averageScore = results
            .filterIsInstance<LivenessResult.Real>()
            .map { it.score }
            .average()
            .toFloat()

        return if (passRate >= minPassRate) {
            MultiFrameLivenessResult.Passed(
                passRate = passRate,
                averageScore = averageScore,
                framesAnalyzed = totalCount
            )
        } else {
            MultiFrameLivenessResult.Failed(
                passRate = passRate,
                framesAnalyzed = totalCount,
                failedFrames = totalCount - realCount
            )
        }
    }

    private fun classifyLiveness(score: Float): LivenessResult {
        return when {
            score >= HIGH_CONFIDENCE_THRESHOLD -> {
                LivenessResult.Real(score, LivenessConfidence.HIGH)
            }
            score >= REAL_THRESHOLD -> {
                LivenessResult.Real(score, LivenessConfidence.MEDIUM)
            }
            score <= LOW_CONFIDENCE_THRESHOLD -> {
                LivenessResult.Fake(score, LivenessConfidence.HIGH, SpoofType.PRINTED_PHOTO)
            }
            else -> {
                LivenessResult.Fake(score, LivenessConfidence.MEDIUM, SpoofType.SCREEN_REPLAY)
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

// --- Clases de Datos y Enums ---

sealed class LivenessResult {
    data class Real(val score: Float, val confidence: LivenessConfidence) : LivenessResult()
    data class Fake(val score: Float, val confidence: LivenessConfidence, val suspectedType: SpoofType) : LivenessResult()
    data class Error(val message: String) : LivenessResult()
}

// Al final de LivenessDetector.kt
sealed class MultiFrameLivenessResult {
    data class Passed(val passRate: Float, val averageScore: Float, val framesAnalyzed: Int) : MultiFrameLivenessResult()
    data class Failed(val passRate: Float, val framesAnalyzed: Int, val failedFrames: Int) : MultiFrameLivenessResult()
}

enum class LivenessConfidence { HIGH, MEDIUM, LOW }

enum class SpoofType { PRINTED_PHOTO, SCREEN_REPLAY, MASK_3D, UNKNOWN }

data class LivenessConfig(
    val realThreshold: Float = 0.85f,
    val minFrames: Int = 3,
    val minPassRate: Float = 0.8f,
    val requireHighConfidence: Boolean = false
) {
    companion object {
        val STANDARD = LivenessConfig()
        val HIGH_SECURITY = LivenessConfig(0.90f, 5, 0.9f, true)
        val PERMISSIVE = LivenessConfig(0.75f, 2, 0.7f, false)
    }
}