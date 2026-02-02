package com.example.Cortex_LaSecuencia.processors

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

class FaceNetProcessor(context: Context) {

    companion object {
        private const val MODEL_FILE = "facenet_512.tflite"
        private const val INPUT_SIZE = 160
        private const val EMBEDDING_SIZE = 512
        private const val MEAN = 127.5f
        private const val STD = 127.5f
    }

    private var interpreter: Interpreter? = null
    private val imageProcessor: ImageProcessor

    init {
        try {
            val model = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(model, options)

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(MEAN, STD))
                .build()
        } catch (e: Exception) {
            throw RuntimeException("Error iniciando FaceNet: ${e.message}. Verifica que el archivo .tflite esté en assets/")
        }
    }

    fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(faceBitmap))
        val embedding = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter?.run(tensorImage.buffer, embedding)
        return normalizeL2(embedding[0])
    }

    fun generateMultipleEmbeddings(faceBitmaps: List<Bitmap>): List<FloatArray> {
        return faceBitmaps.map { generateEmbedding(it) }
    }

    private fun normalizeL2(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        return embedding.map { it / norm }.toFloatArray()
    }

    fun validateAgainstMultiple(
        newEmbedding: FloatArray,
        referenceEmbeddings: List<FloatArray>,
        avgThreshold: Float = 0.75f,
        maxThreshold: Float = 0.80f
    ): ValidationResult {
        val similarities = referenceEmbeddings.map { calculateSimilarity(newEmbedding, it) }
        val avg = similarities.average().toFloat()
        val max = similarities.maxOrNull() ?: 0f

        return when {
            max >= maxThreshold && avg >= avgThreshold -> ValidationResult.Match(max, avg)
            max >= 0.70f && avg >= 0.65f -> ValidationResult.ProbableMatch(max, true)
            else -> ValidationResult.NoMatch(max)
        }
    }

    private fun calculateSimilarity(e1: FloatArray, e2: FloatArray): Float {
        return e1.zip(e2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

// Clases de resultado para FaceNet
// Al final de FaceNetProcessor.kt
sealed class ValidationResult {
    data class Match(val confidence: Float, val averageConfidence: Float) : ValidationResult()
    data class ProbableMatch(val confidence: Float, val requiresSecondaryAuth: Boolean) : ValidationResult()
    data class NoMatch(val maxSimilarity: Float) : ValidationResult()
}