package com.example.Cortex_LaSecuencia.actividades

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
import com.example.Cortex_LaSecuencia.R
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

    // --- NUEVA VARIABLE PARA EL PROMEDIO ---
    private var sumaTiemposReaccion: Long = 0

    // Variables de Seguridad Sentinel
    private var usuarioPresente = false
    private var tiempoInicioAusencia: Long = 0
    private val LIMITE_AUSENCIA_MS = 5000L
    private var juegoIniciado = false

    private val handler = Handler(Looper.getMainLooper())
    private var proximoCirculoRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)
        txtInstruccion = findViewById(R.id.txt_instruccion)

        targetCircle.visibility = View.GONE
        txtInstruccion.text = "Posiciona tu rostro frente a la cámara..."

        iniciarSentinelCamara()

        targetCircle.setOnClickListener {
            if (!testFinalizado) {
                val reactionTime = System.currentTimeMillis() - startTime
                aciertos++

                // GUARDAMOS EL TIEMPO (Esto quita la advertencia de 'unused variable')
                sumaTiemposReaccion += reactionTime

                txtInstruccion.text = "¡RÁPIDO! ${reactionTime}ms"
                txtInstruccion.setTextColor(Color.GREEN)

                targetCircle.visibility = View.GONE

                // CORRECCIÓN: Usamos el nombre correcto de la función
                programarSiguienteCirculo()
            }
        }
    }

    private fun iniciarTemporizador() {
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!testFinalizado && juegoIniciado && usuarioPresente) {
                    val segundos = millisUntilFinished / 1000
                    txtInstruccion.text = "TIEMPO: ${segundos}s | ACIERTOS: $aciertos"
                    txtInstruccion.setTextColor(Color.WHITE)
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
                        if (usuarioPresente) {
                            usuarioPresente = false
                            tiempoInicioAusencia = tiempoActual
                            targetCircle.visibility = View.GONE
                            cancelarProximoCirculo()
                        }
                        val tiempoAusente = tiempoActual - tiempoInicioAusencia
                        val segundosAusente = tiempoAusente / 1000
                        txtInstruccion.text = "⚠️ ¡OPERADOR AUSENTE!\nSEGUNDOS: $segundosAusente/5"
                        txtInstruccion.setTextColor(Color.RED)
                        if (tiempoAusente >= LIMITE_AUSENCIA_MS) {
                            reprobarPorSeguridad("INCUMPLIMIENTO DE PROTOCOLO:\nOPERADOR AUSENTE MÁS DE 5 SEGUNDOS")
                        }
                    } else {
                        if (!usuarioPresente) {
                            usuarioPresente = true
                            tiempoInicioAusencia = 0
                            if (!juegoIniciado) {
                                juegoIniciado = true
                                iniciarTemporizador()
                                programarSiguienteCirculo()
                            } else {
                                programarSiguienteCirculo()
                            }
                        }
                        if (juegoIniciado) txtInstruccion.setTextColor(Color.WHITE)
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else imageProxy.close()
    }

    private fun programarSiguienteCirculo() {
        if (!testFinalizado && usuarioPresente && juegoIniciado) {
            cancelarProximoCirculo()
            val delay = Random.nextLong(800, 2000)
            proximoCirculoRunnable = Runnable { mostrarCirculo() }
            handler.postDelayed(proximoCirculoRunnable!!, delay)
        }
    }

    private fun cancelarProximoCirculo() {
        proximoCirculoRunnable?.let {
            handler.removeCallbacks(it)
            proximoCirculoRunnable = null
        }
    }

    private fun mostrarCirculo() {
        if (!testFinalizado && usuarioPresente && juegoIniciado) {
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

        val score = if (intentosTotales > 1) (aciertos.toFloat() / (intentosTotales - 1) * 100).toInt() else 0

        // CÁLCULO DEL PROMEDIO FINAL
        val promedioMs = if (aciertos > 0) sumaTiemposReaccion / aciertos else 0

        if (score >= 70) {
            android.app.AlertDialog.Builder(this)
                .setTitle("¡NIVEL 1 SUPERADO! ✅")
                .setMessage("Score: $score%\nReacción Promedio: ${promedioMs}ms\n\n¿Proceder al Nivel 2?")
                .setCancelable(false)
                .setPositiveButton("INICIAR NIVEL 2") { _, _ ->
                    val intent = android.content.Intent(this, SecuenciaTestActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .show()
        } else {
            mostrarResultadoFinal("SCORE: $score% | PROMEDIO: ${promedioMs}ms\nESTADO: NO APTO ❌")
        }
    }

    private fun reprobarPorSeguridad(motivo: String) {
        testFinalizado = true
        cancelarProximoCirculo()
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

    override fun onDestroy() {
        super.onDestroy()
        cancelarProximoCirculo()
    }
}