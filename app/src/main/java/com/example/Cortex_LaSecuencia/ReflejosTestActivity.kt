package com.example.Cortex_LaSecuencia

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import kotlin.random.Random

class ReflejosTestActivity : AppCompatActivity() {

    private lateinit var targetCircle: View
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var txtInstruccion: TextView
    private var startTime: Long = 0
    private var aciertos = 0
    private var intentosTotales = 0
    private var testFinalizado = false
    private var tiempoAusenteMs: Long = 0
    private var ultimaVezVistoMs: Long = System.currentTimeMillis()
    private val LIMITE_AUSENCIA_MS = 5000 // 5 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)
        txtInstruccion = findViewById(R.id.txt_instruccion)

        iniciarSentinelCamara()
        iniciarTemporizador()

        targetCircle.setOnClickListener {
            if (!testFinalizado) {
                val reactionTime = System.currentTimeMillis() - startTime
                aciertos++
                intentosTotales++
                targetCircle.visibility = View.GONE
                esperarYAparecer()
            }
        }
    }

    private fun iniciarTemporizador() {
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Actualizamos el texto con el tiempo restante
                if (!testFinalizado) {
                    val segundos = millisUntilFinished / 1000
                    // Solo actualizamos si Sentinel no ha marcado error
                    if (txtInstruccion.text != "⚠️ ¡OPERADOR AUSENTE!") {
                        txtInstruccion.text = "TIEMPO: ${segundos}s | ACIERTOS: $aciertos"
                    }
                }
            }

            override fun onFinish() {
                finalizarPrueba()
            }
        }.start()
    }

    private fun iniciarSentinelCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            // ANALIZADOR DE ROSTRO (Sentinel)
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        analizarRostro(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Toast.makeText(this, "Error Sentinel", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostro(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !testFinalizado) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = FaceDetection.getClient()

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val tiempoActual = System.currentTimeMillis()

                    if (faces.isEmpty()) {
                        // El usuario NO está
                        tiempoAusenteMs += (tiempoActual - ultimaVezVistoMs)

                        txtInstruccion.text = "⚠️ ¡OPERADOR AUSENTE!\nSEGUNDOS: ${tiempoAusenteMs / 1000}/5"
                        txtInstruccion.setTextColor(Color.RED)
                        targetCircle.visibility = View.GONE

                        // VALIDACIÓN INDUSTRIAL: Si pasa de 5 segundos, reprobado.
                        if (tiempoAusenteMs >= LIMITE_AUSENCIA_MS) {
                            reprobarPorSeguridad("INCUMPLIMIENTO DE PROTOCOLO: OPERADOR AUSENTE")
                        }
                    } else {
                        // El usuario SÍ está
                        txtInstruccion.setTextColor(Color.WHITE)
                        // No reiniciamos el tiempoAusenteMs para que sea acumulativo (opcional)
                    }
                    ultimaVezVistoMs = tiempoActual
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun reprobarPorSeguridad(motivo: String) {
        testFinalizado = true
        targetCircle.visibility = View.GONE

        android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ EVALUACIÓN ANULADA")
            .setMessage(motivo)
            .setCancelable(false)
            .setPositiveButton("SALIR") { _, _ -> finish() }
            .show()
    }

    private fun finalizarPrueba() {
        testFinalizado = true
        targetCircle.visibility = View.GONE
        val score = if (intentosTotales > 0) (aciertos.toFloat() / intentosTotales * 100).toInt() else 0
        val resultado = if (score >= 70) "APTO ✅" else "NO APTO ❌"

        txtInstruccion.text = "TEST FINALIZADO\nSCORE: $score% | $resultado"
        txtInstruccion.textSize = 20f
        txtInstruccion.setTextColor(Color.CYAN)
    }

    private fun esperarYAparecer() {
        if (!testFinalizado) {
            Handler(Looper.getMainLooper()).postDelayed({ moverCirculo() }, Random.nextLong(1000, 2500))
        }
    }

    private fun moverCirculo() {
        if (!testFinalizado && txtInstruccion.text != "⚠️ ¡OPERADOR AUSENTE!") {
            val width = mainLayout.width - targetCircle.width
            val height = mainLayout.height - targetCircle.height
            if (width > 0 && height > 0) {
                targetCircle.x = Random.nextInt(0, width).toFloat()
                targetCircle.y = Random.nextInt(0, height).toFloat()
                targetCircle.visibility = View.VISIBLE
                startTime = System.currentTimeMillis()
                if (intentosTotales == 0) intentosTotales = 1 // Primer intento
            }
        }
    }
}