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

    // Variables de Seguridad Sentinel
    private var tiempoAusenteMs: Long = 0
    private var ultimaVezVistoMs: Long = System.currentTimeMillis()
    private val LIMITE_AUSENCIA_MS = 5000 // 5 segundos permitidos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)
        txtInstruccion = findViewById(R.id.txt_instruccion)

        // Iniciamos el hardware y el reloj
        iniciarSentinelCamara()
        iniciarTemporizador()

        targetCircle.setOnClickListener {
            if (!testFinalizado) {
                val reactionTime = System.currentTimeMillis() - startTime
                aciertos++
                // Ocultamos y esperamos al siguiente
                targetCircle.visibility = View.GONE
                esperarYAparecer()
            }
        }
    }

    private fun iniciarTemporizador() {
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!testFinalizado) {
                    val segundos = millisUntilFinished / 1000
                    // Solo actualizamos el tiempo si el usuario está presente
                    if (!txtInstruccion.text.contains("AUSENTE")) {
                        txtInstruccion.text = "TIEMPO: ${segundos}s | ACIERTOS: $aciertos"
                    }
                }
            }
            override fun onFinish() {
                if (!testFinalizado) finalizarPrueba()
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
                Toast.makeText(this, "Error Sentinel Hardware", Toast.LENGTH_SHORT).show()
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
                        // El usuario NO está frente a la cámara
                        tiempoAusenteMs += (tiempoActual - ultimaVezVistoMs)
                        txtInstruccion.text = "⚠️ ¡OPERADOR AUSENTE!\nSEGUNDOS: ${tiempoAusenteMs / 1000}/5"
                        txtInstruccion.setTextColor(Color.RED)
                        targetCircle.visibility = View.GONE

                        if (tiempoAusenteMs >= LIMITE_AUSENCIA_MS) {
                            reprobarPorSeguridad("INCUMPLIMIENTO DE PROTOCOLO: OPERADOR AUSENTE")
                        }
                    } else {
                        // El usuario SÍ está presente
                        txtInstruccion.setTextColor(Color.WHITE)

                        // LÓGICA DE ARRANQUE: Si es el inicio, lanzamos la primera bola
                        if (targetCircle.visibility == View.GONE && intentosTotales == 0) {
                            intentosTotales = 1
                            esperarYAparecer()
                        }
                    }
                    ultimaVezVistoMs = tiempoActual
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun esperarYAparecer() {
        if (!testFinalizado) {
            // Tiempo de espera aleatorio entre círculos
            Handler(Looper.getMainLooper()).postDelayed({
                moverCirculo()
            }, Random.nextLong(800, 2000))
        }
    }

    private fun moverCirculo() {
        // Solo aparece si el test sigue activo y el usuario está presente
        if (!testFinalizado && !txtInstruccion.text.contains("AUSENTE")) {
            val width = mainLayout.width - targetCircle.width
            val height = mainLayout.height - targetCircle.height

            if (width > 0 && height > 0) {
                targetCircle.x = Random.nextInt(0, width).toFloat()
                targetCircle.y = Random.nextInt(0, height).toFloat()
                targetCircle.visibility = View.VISIBLE
                startTime = System.currentTimeMillis()
                intentosTotales++
            }
        }
    }

    private fun finalizarPrueba() {
        testFinalizado = true
        targetCircle.visibility = View.GONE

        // Cálculo basado en tu lógica original de HTML
        val score = if (intentosTotales > 1) (aciertos.toFloat() / (intentosTotales - 1) * 100).toInt() else 0
        val resultado = if (score >= 70) "APTO ✅" else "NO APTO ❌"

        mostrarResultadoFinal("TEST FINALIZADO\nSCORE: $score% | $resultado")
    }

    private fun reprobarPorSeguridad(motivo: String) {
        testFinalizado = true
        targetCircle.visibility = View.GONE
        mostrarResultadoFinal(motivo)
    }

    private fun mostrarResultadoFinal(mensaje: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("SISTEMA CORTEX: RESULTADO")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("FINALIZAR") { _, _ -> finish() }
            .show()
    }
}