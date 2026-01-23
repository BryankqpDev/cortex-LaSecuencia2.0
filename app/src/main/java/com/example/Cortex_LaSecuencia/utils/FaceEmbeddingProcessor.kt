package com.example.Cortex_LaSecuencia.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.security.MessageDigest

object FaceEmbeddingProcessor {

    /**
     * Genera un hash único de las características faciales
     * Retorna un String de 64 caracteres que representa la "huella facial"
     */
    fun generarHashFacial(
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()

        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onError("No se detectó ningún rostro")
                    return@addOnSuccessListener
                }

                val face = faces[0] // Tomar el primer rostro detectado

                // Extraer características faciales
                val caracteristicas = StringBuilder()

                // 1. Posición y tamaño del rostro
                caracteristicas.append("${face.boundingBox.left},")
                caracteristicas.append("${face.boundingBox.top},")
                caracteristicas.append("${face.boundingBox.width()},")
                caracteristicas.append("${face.boundingBox.height()},")

                // 2. Rotación de la cabeza
                caracteristicas.append("${face.headEulerAngleY},")
                caracteristicas.append("${face.headEulerAngleZ},")
                caracteristicas.append("${face.headEulerAngleX},")

                // 3. Probabilidades de expresiones
                face.smilingProbability?.let { caracteristicas.append("$it,") }
                face.leftEyeOpenProbability?.let { caracteristicas.append("$it,") }
                face.rightEyeOpenProbability?.let { caracteristicas.append("$it,") }

                // 4. Landmarks (puntos faciales)
                face.allLandmarks.forEach { landmark ->
                    caracteristicas.append("${landmark.position.x},${landmark.position.y},")
                }

                // Generar hash SHA-256 de las características
                val hashFacial = generarSHA256(caracteristicas.toString())

                onSuccess(hashFacial)
            }
            .addOnFailureListener { e ->
                onError("Error al procesar rostro: ${e.localizedMessage}")
            }
    }

    /**
     * Genera un hash SHA-256 a partir de un string
     * Retorna un string hexadecimal de 64 caracteres
     */
    private fun generarSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Genera un ID numérico único basado en las características faciales
     * Retorna un Long (número entero grande)
     */
    fun generarIdNumerico(bitmap: Bitmap, onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        generarHashFacial(bitmap,
            onSuccess = { hash ->
                // Tomar los primeros 16 caracteres del hash y convertir a Long
                val idNumerico = hash.substring(0, 16).toLong(16)
                onSuccess(idNumerico)
            },
            onError = onError
        )
    }
}