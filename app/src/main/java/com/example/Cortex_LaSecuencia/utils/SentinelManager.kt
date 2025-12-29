package com.example.Cortex_LaSecuencia.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.util.concurrent.Executor

class SentinelManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var previewView: PreviewView? = null
    private var isActive = false
    private var usuarioPresente = false
    private var tiempoInicioAusencia: Long = 0
    private val LIMITE_AUSENCIA_MS = 4000L // 4 segundos como en HTML
    private var onFaceDetected: ((Boolean) -> Unit)? = null
    private var onAbsenceTimeout: (() -> Unit)? = null
    private var statusTextView: TextView? = null

    fun iniciarSentinel(
        previewView: PreviewView,
        onFaceDetected: (Boolean) -> Unit,
        onAbsenceTimeout: () -> Unit,
        statusTextView: TextView? = null
    ) {
        this.previewView = previewView
        this.onFaceDetected = onFaceDetected
        this.onAbsenceTimeout = onAbsenceTimeout
        this.statusTextView = statusTextView

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                configurarCamara()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al iniciar Sentinel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun configurarCamara() {
        val provider = cameraProvider ?: return
        val previewView = this.previewView ?: return

        // Preview
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Image Analyzer
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    analizarRostro(imageProxy)
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalyzer
            )
            isActive = true
        } catch (e: Exception) {
            Toast.makeText(context, "Error al configurar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostro(imageProxy: ImageProxy) {
        // Verificar que Sentinel sigue activo
        if (!isActive) {
            imageProxy.close()
            return
        }

        // Verificar que el contexto sigue siendo válido
        if (context is androidx.lifecycle.LifecycleOwner) {
            val lifecycleOwner = context as androidx.lifecycle.LifecycleOwner
            if (lifecycleOwner.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED) {
                imageProxy.close()
                return
            }
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient()

        detector.process(image)
            .addOnSuccessListener { faces ->
                // Verificar nuevamente antes de procesar
                if (!isActive) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val tiempoActual = System.currentTimeMillis()

                if (faces.isEmpty()) {
                    // No se detecta rostro
                    if (usuarioPresente) {
                        usuarioPresente = false
                        tiempoInicioAusencia = tiempoActual
                        onFaceDetected?.invoke(false)
                        statusTextView?.text = "⚠️ ¡OPERADOR AUSENTE!"
                    } else {
                        val tiempoAusente = tiempoActual - tiempoInicioAusencia
                        val segundosAusente = tiempoAusente / 1000
                        statusTextView?.text = "⚠️ ¡OPERADOR AUSENTE!\nSEGUNDOS: $segundosAusente/4"

                        if (tiempoAusente >= LIMITE_AUSENCIA_MS) {
                            onAbsenceTimeout?.invoke()
                        }
                    }
                } else {
                    // Rostro detectado
                    if (!usuarioPresente) {
                        usuarioPresente = true
                        tiempoInicioAusencia = 0
                        onFaceDetected?.invoke(true)
                        statusTextView?.text = "✅ Rostro verificado"
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    fun detenerSentinel() {
        isActive = false
        
        // Limpiar analyzer primero para detener procesamiento
        imageAnalyzer?.clearAnalyzer()
        imageAnalyzer = null
        
        // Desvincular cámara
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignorar errores al desvincular
        }
        
        preview = null
        previewView = null
        cameraProvider = null
        
        // Limpiar callbacks
        onFaceDetected = null
        onAbsenceTimeout = null
        statusTextView = null
    }

    fun estaActivo(): Boolean = isActive
}

