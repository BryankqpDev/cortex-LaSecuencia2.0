package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
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
    private var intentosPermitidos = 2

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
        if (testFinalizado || isFinishing) return

        esTurnoDelUsuario = false
        secuenciaUsuario.clear()
        secuenciaGenerada.add(Random.nextInt(0, 4))

        txtInstruccion.text = "NIVEL $nivelActual: OBSERVA"
        txtInstruccion.setTextColor(Color.CYAN)

        val handler = Handler(Looper.getMainLooper())
        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                if (!testFinalizado) iluminarBoton(botonIndex)
                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        if (!testFinalizado) {
                            esTurnoDelUsuario = true
                            txtInstruccion.text = "RECOPIE LA SECUENCIA"
                            txtInstruccion.setTextColor(Color.WHITE)
                        }
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

        // 1. ERROR DEL USUARIO
        if (secuenciaUsuario[indexUltimoIntento] != secuenciaGenerada[indexUltimoIntento]) {
            intentosPermitidos--
            if (intentosPermitidos > 0) {
                txtInstruccion.text = "❌ ERROR. QUEDAN $intentosPermitidos INTENTOS"
                txtInstruccion.setTextColor(Color.YELLOW)
                secuenciaUsuario.clear()
                Handler(Looper.getMainLooper()).postDelayed({ repetirNivelActual() }, 2000)
            } else {
                reprobarPorError("DEMASIADOS ERRORES COGNITIVOS")
            }
            return
        }

        // 2. ÉXITO EN LA SECUENCIA
        if (secuenciaUsuario.size == secuenciaGenerada.size) {
            nivelActual++
            intentosPermitidos = 2

            if (nivelActual > 5) {
                finalizarConExito()
            } else {
                txtInstruccion.text = "¡BIEN HECHO! SIGUIENTE..."
                txtInstruccion.setTextColor(Color.GREEN)
                Handler(Looper.getMainLooper()).postDelayed({ iniciarSiguienteNivel() }, 1500)
            }
        }
    }

    private fun repetirNivelActual() {
        if (testFinalizado) return
        esTurnoDelUsuario = false
        txtInstruccion.text = "OBSERVA DE NUEVO"
        txtInstruccion.setTextColor(Color.CYAN)
        // ... Lógica de repetición (simplificada para el ejemplo) ...
        iniciarSiguienteNivel() // Truco: Reiniciamos la visualización del nivel actual
    }

    private fun iniciarSentinelCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
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
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) { }
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
                    } else if (secuenciaGenerada.isEmpty()) {
                        iniciarSiguienteNivel()
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else imageProxy.close()
    }

    private fun finalizarConExito() {
        if (testFinalizado) return
        testFinalizado = true

        // --- BLINDAJE ANTI-CRASH ---
        if (isFinishing || isDestroyed) return

        // 1. Guardamos puntaje ANTES del diálogo por seguridad
        CortexManager.guardarPuntaje("t2", 100)

        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("¡MEMORIA VALIDADA! 🧠✅")
                .setMessage("Nivel 2 completado.\nSiguiente: Anticipación (T3).")
                .setCancelable(false)
                .setPositiveButton("SIGUIENTE NIVEL") { _, _ ->
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
                .show()
        } catch (e: Exception) {
            // Si el diálogo falla, nos vamos directo al T3
            CortexManager.navegarAlSiguiente(this)
            finish()
        }
    }

    private fun reprobarPorError(motivo: String) {
        if (testFinalizado) return
        testFinalizado = true

        // Guardamos puntaje bajo
        CortexManager.guardarPuntaje("t2", 0)

        if (isFinishing || isDestroyed) return

        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("NIVEL 2: FALLIDO ❌")
                .setMessage("$motivo\n\n¿Deseas reintentar?")
                .setCancelable(false)
                .setPositiveButton("REINTENTAR") { _, _ ->
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
                .setNegativeButton("SALIR") { _, _ ->
                    finishAffinity()
                }
                .show()
        } catch (e: Exception) {
            finish()
        }
    }
}