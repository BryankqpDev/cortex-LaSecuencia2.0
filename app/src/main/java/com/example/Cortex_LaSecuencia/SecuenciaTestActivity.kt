package com.example.Cortex_LaSecuencia

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import kotlin.random.Random

class SecuenciaTestActivity : AppCompatActivity() {

    private lateinit var botones: List<View>
    private lateinit var txtInstruccion: TextView

    private val secuenciaGenerada = mutableListOf<Int>()
    private val secuenciaUsuario = mutableListOf<Int>()

    private var nivelActual = 1
    private var esTurnoDelUsuario = false
    private var testFinalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secuencia_test)

        txtInstruccion = findViewById(R.id.txt_instruccion_seq)
        botones = listOf(
            findViewById(R.id.btn_1),
            findViewById(R.id.btn_2),
            findViewById(R.id.btn_3),
            findViewById(R.id.btn_4)
        )

        configurarClicks()
        iniciarSentinelCamara()

        // El juego comenzará cuando Sentinel detecte el rostro
    }

    private fun configurarClicks() {
        botones.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (esTurnoDelUsuario && !testFinalizado) {
                    iluminarBoton(index)
                    secuenciaUsuario.add(index)
                    verificarEntrada()
                }
            }
        }
    }

    private fun iniciarSiguienteNivel() {
        esTurnoDelUsuario = false
        secuenciaUsuario.clear()
        secuenciaGenerada.add(Random.nextInt(0, 4))

        txtInstruccion.text = "NIVEL $nivelActual: OBSERVA"
        txtInstruccion.setTextColor(Color.CYAN)

        // Mostrar la secuencia con retraso entre botones
        val handler = Handler(Looper.getMainLooper())
        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                iluminarBoton(botonIndex)
                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        esTurnoDelUsuario = true
                        txtInstruccion.text = "RECOPIE LA SECUENCIA"
                        txtInstruccion.setTextColor(Color.WHITE)
                    }, 800)
                }
            }, (i + 1) * 1000L)
        }
    }

    private fun iluminarBoton(index: Int) {
        val originalColor = Color.parseColor("#1E293B")
        val highlightColor = Color.parseColor("#00F0FF")

        botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(highlightColor)
        Handler(Looper.getMainLooper()).postDelayed({
            botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(originalColor)
        }, 500)
    }

    private fun verificarEntrada() {
        val indexUltimoIntento = secuenciaUsuario.size - 1

        if (secuenciaUsuario[indexUltimoIntento] != secuenciaGenerada[indexUltimoIntento]) {
            reprobarPorError("SECUENCIA INCORRECTA")
            return
        }

        if (secuenciaUsuario.size == secuenciaGenerada.size) {
            nivelActual++
            if (nivelActual > 5) { // Meta: 5 niveles para aprobar
                finalizarConExito()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({ iniciarSiguienteNivel() }, 1500)
            }
        }
    }

    private fun iniciarSentinelCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderSeq).surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        analizarRostro(imageProxy)
                    }
                }
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostro(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !testFinalizado) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            FaceDetection.getClient().process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        txtInstruccion.text = "⚠️ DISTRACCIÓN DETECTADA"
                        txtInstruccion.setTextColor(Color.RED)
                        // Si se distrae durante la secuencia, falla
                        if (!esTurnoDelUsuario && secuenciaGenerada.isNotEmpty()) {
                            reprobarPorError("PROTOCOLO FALLIDO: PERDIDA DE ATENCIÓN")
                        }
                    } else if (secuenciaGenerada.isEmpty()) {
                        iniciarSiguienteNivel() // Arranca el juego al ver rostro
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else imageProxy.close()
    }

    private fun finalizarConExito() {
        testFinalizado = true
        mostrarMensajeFinal("VALIDACIÓN CORTEX COMPLETA ✅\nOperador Apto para Jornada.")
    }

    private fun reprobarPorError(motivo: String) {
        testFinalizado = true
        mostrarMensajeFinal("FALLO EN NIVEL 2 ❌\n$motivo")
    }

    private fun mostrarMensajeFinal(mensaje: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("RESULTADO FINAL")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("CERRAR") { _, _ -> finish() }
            .show()
    }
}