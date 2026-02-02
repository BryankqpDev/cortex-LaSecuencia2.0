package com.example.Cortex_LaSecuencia.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.Cortex_LaSecuencia.LivenessDetector
import com.example.Cortex_LaSecuencia.MultiFrameLivenessResult
import com.example.Cortex_LaSecuencia.processors.FaceNetProcessor
import com.example.Cortex_LaSecuencia.processors.ValidationResult

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class BiometricValidator(private val context: Context) {

    private val livenessDetector by lazy { LivenessDetector(context) }
    private val faceNetProcessor by lazy { FaceNetProcessor(context) }
    private val firestore = FirebaseFirestore.getInstance()

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun usuarioTieneRegistro(dni: String): Boolean {
        return try {
            val doc = firestore.collection("usuarios").document(dni)
                .collection("biometria").document("embeddings").get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e("BiometricValidator", "Error verificando registro: ${e.message}")
            false
        }
    }

    suspend fun validateCaptureQuality(bitmap: Bitmap): CaptureQualityResult {
        return try {
            val input = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(input).await()

            if (faces.isEmpty()) return CaptureQualityResult.NoFaceDetected
            if (faces.size > 1) return CaptureQualityResult.MultipleFacesDetected(faces.size)

            val face = faces.first()
            val issues = mutableListOf<String>()

            if (face.boundingBox.width() < 150) issues.add("Acércate más a la cámara")
            if (abs(face.headEulerAngleY) > 15f) issues.add("Mira de frente")
            if ((face.leftEyeOpenProbability ?: 1f) < 0.2f) issues.add("Abre los ojos")

            if (issues.isEmpty()) CaptureQualityResult.Valid(face)
            else CaptureQualityResult.PoorQuality(issues)
        } catch (e: Exception) {
            CaptureQualityResult.Error(e.message ?: "Error desconocido")
        }
    }

    suspend fun enrollUser(
        dni: String,
        frames: List<Bitmap>,
        verificarLiveness: Boolean = true
    ): EnrollmentResult {
        Log.d("BiometricValidator", "Iniciando registro para DNI: $dni")

        try {
            // 1. Recortar rostros
            val faces = frames.mapNotNull { frame ->
                val result = validateCaptureQuality(frame)
                if (result is CaptureQualityResult.Valid) cropFace(frame, result.face.boundingBox) else null
            }

            if (faces.isEmpty()) return EnrollmentResult.Failed("No se detectó rostro válido")

            // 2. Liveness Check
            if (verificarLiveness) {
                Log.d("BiometricValidator", "Ejecutando prueba de vida...")
                val livenessResult = livenessDetector.validateMultipleFrames(faces)
                if (livenessResult is MultiFrameLivenessResult.Failed) {
                    return EnrollmentResult.SpoofDetected("Posible suplantación detectada")
                }
            }

            // 3. Generar Embeddings
            Log.d("BiometricValidator", "Generando huella facial...")
            val embeddings = faceNetProcessor.generateMultipleEmbeddings(faces)

            // Convertir a Mapa para Firestore
            val embeddingsMap = embeddings.mapIndexed { index, floatArray ->
                index.toString() to floatArray.toList()
            }.toMap()

            val data = hashMapOf(
                "embeddings" to embeddingsMap,
                "timestamp" to Timestamp.now(),
                "model" to "facenet_512",
                "device" to android.os.Build.MODEL
            )

            // 4. Guardar en Firebase
            Log.d("BiometricValidator", "Subiendo a Firebase...")
            firestore.collection("usuarios").document(dni)
                .collection("biometria").document("embeddings")
                .set(data).await()

            Log.d("BiometricValidator", "✅ Registro completado con éxito")
            return EnrollmentResult.Success(dni, embeddings.size)

        } catch (e: Exception) {
            Log.e("BiometricValidator", "❌ Error en enrollUser: ${e.message}")
            return EnrollmentResult.Failed("Error crítico: ${e.message}")
        }
    }

    suspend fun authenticateUser(
        dni: String,
        frames: List<Bitmap>,
        verificarLiveness: Boolean = true
    ): AuthenticationResult {
        Log.d("BiometricValidator", "Iniciando autenticación para DNI: $dni")

        try {
            val faces = frames.mapNotNull { frame ->
                val result = validateCaptureQuality(frame)
                if (result is CaptureQualityResult.Valid) cropFace(frame, result.face.boundingBox) else null
            }

            if (faces.isEmpty()) return AuthenticationResult.Failed("No se detectó rostro")

            if (verificarLiveness) {
                val livenessResult = livenessDetector.validateMultipleFrames(faces)
                if (livenessResult is MultiFrameLivenessResult.Failed) {
                    return AuthenticationResult.SpoofDetected("Prueba de vida fallida")
                }
            }

            // Descargar embeddings
            val doc = firestore.collection("usuarios").document(dni)
                .collection("biometria").document("embeddings").get().await()

            if (!doc.exists()) return AuthenticationResult.UserNotEnrolled(dni)

            // ✅ CORRECCIÓN: Leer el Mapa en lugar de la Lista
            val storedMap = doc.get("embeddings") as? Map<String, List<Double>>
                ?: return AuthenticationResult.Failed("Formato de datos inválido en servidor")

            val storedEmbeddings = storedMap.values.map { list -> 
                list.map { it.toFloat() }.toFloatArray() 
            }

            val currentEmbedding = faceNetProcessor.generateEmbedding(faces.first())
            val matchResult = faceNetProcessor.validateAgainstMultiple(currentEmbedding, storedEmbeddings)

            return when (matchResult) {
                is ValidationResult.Match -> AuthenticationResult.Success(dni, matchResult.confidence)
                is ValidationResult.ProbableMatch -> AuthenticationResult.ProbableMatch(dni, matchResult.confidence)
                is ValidationResult.NoMatch -> AuthenticationResult.NoMatch(dni, matchResult.maxSimilarity)
            }
        } catch (e: Exception) {
            Log.e("BiometricValidator", "❌ Error en autenticación: ${e.message}")
            return AuthenticationResult.Failed("Fallo de conexión: ${e.message}")
        }
    }

    private fun cropFace(bitmap: Bitmap, rect: android.graphics.Rect): Bitmap {
        val padding = (rect.width() * 0.2f).toInt()
        val left = (rect.left - padding).coerceAtLeast(0)
        val top = (rect.top - padding).coerceAtLeast(0)
        val width = (rect.width() + padding * 2).coerceAtMost(bitmap.width - left)
        val height = (rect.height() + padding * 2).coerceAtMost(bitmap.height - top)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun cleanup() {
        livenessDetector.close()
        faceNetProcessor.close()
    }
}

sealed class CaptureQualityResult {
    data class Valid(val face: Face) : CaptureQualityResult()
    data class PoorQuality(val issues: List<String>) : CaptureQualityResult()
    object NoFaceDetected : CaptureQualityResult()
    data class MultipleFacesDetected(val count: Int) : CaptureQualityResult()
    data class Error(val msg: String) : CaptureQualityResult()
}

sealed class EnrollmentResult {
    data class Success(val dni: String, val count: Int) : EnrollmentResult()
    data class Failed(val reason: String) : EnrollmentResult()
    data class SpoofDetected(val reason: String) : EnrollmentResult()
}

sealed class AuthenticationResult {
    data class Success(val dni: String, val confidence: Float) : AuthenticationResult()
    data class ProbableMatch(val dni: String, val confidence: Float) : AuthenticationResult()
    data class NoMatch(val dni: String, val sim: Float) : AuthenticationResult()
    data class UserNotEnrolled(val dni: String) : AuthenticationResult()
    data class Failed(val reason: String) : AuthenticationResult()
    data class SpoofDetected(val reason: String) : AuthenticationResult()
}
