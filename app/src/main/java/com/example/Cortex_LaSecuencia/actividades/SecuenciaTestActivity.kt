package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
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
    private lateinit var txtIntento: TextView // Asegúrate de tener esto en tu XML o bórralo si no

    private val secuenciaGenerada = mutableListOf<Int>()
    private val secuenciaUsuario = mutableListOf<Int>()

    // CONFIGURACIÓN: La secuencia siempre es de 6 pasos
    private val LONGITUD_SECUENCIA = 6

    private var esTurnoDelUsuario = false
    private var testFinalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secuencia_test)

        txtInstruccion = findViewById(R.id.txt_instruccion_seq)
        // Vincula tus botones...
        botones = listOf(
            findViewById(R.id.btn_1), findViewById(R.id.btn_2), findViewById(R.id.btn_3),
            findViewById(R.id.btn_4), findViewById(R.id.btn_5), findViewById(R.id.btn_6),
            findViewById(R.id.btn_7), findViewById(R.id.btn_8), findViewById(R.id.btn_9)
        )

        val intento = CortexManager.obtenerIntentoActual("t2")
        txtInstruccion.text = "INTENTO $intento/2"

        configurarClicks()
        iniciarSentinelCamara()

        // Inicia el test de 6 pasos
        Handler(Looper.getMainLooper()).postDelayed({ iniciarSecuenciaFija() }, 1000)
    }

    private fun configurarClicks() {
        botones.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (esTurnoDelUsuario && !testFinalizado) {
                    iluminarBoton(index, true)
                    secuenciaUsuario.add(index)
                    verificarEntrada()
                }
            }
        }
    }

    private fun iniciarSecuenciaFija() {
        if (testFinalizado || isFinishing) return

        esTurnoDelUsuario = false
        secuenciaUsuario.clear()
        secuenciaGenerada.clear()

        // GENERAMOS LOS 6 PASOS DE UNA VEZ
        repeat(LONGITUD_SECUENCIA) {
            secuenciaGenerada.add(Random.nextInt(0, 9))
        }

        txtInstruccion.text = "OBSERVA LA SECUENCIA ($LONGITUD_SECUENCIA PASOS)"
        txtInstruccion.setTextColor(Color.CYAN)

        mostrarSecuenciaGenerada()
    }

    private fun mostrarSecuenciaGenerada() {
        val handler = Handler(Looper.getMainLooper())
        val tiempoMuestra = 600L
        val tiempoPausa = 200L
        val tiempoTotal = tiempoMuestra + tiempoPausa

        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                if (!testFinalizado) iluminarBoton(botonIndex, false)

                // Si es el último paso, damos el turno al usuario
                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        if (!testFinalizado) {
                            esTurnoDelUsuario = true
                            txtInstruccion.text = "TU TURNO: REPITE LOS 6 PASOS"
                            txtInstruccion.setTextColor(Color.WHITE)
                        }
                    }, tiempoTotal)
                }
            }, (i + 1) * tiempoTotal)
        }
    }

    private fun iluminarBoton(index: Int, esUsuario: Boolean) {
        val originalColor = Color.parseColor("#1E293B")
        val highlightColor = if (esUsuario) Color.parseColor("#F59E0B") else Color.parseColor("#00F0FF")

        botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(highlightColor)
        Handler(Looper.getMainLooper()).postDelayed({
            botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(originalColor)
        }, 500)
    }

    private fun verificarEntrada() {
        val pasoActual = secuenciaUsuario.size - 1

        // 1. Verificar si el botón presionado es el correcto
        if (secuenciaUsuario[pasoActual] != secuenciaGenerada[pasoActual]) {
            // Se equivocó en medio de la secuencia
            finalizarJuego(exito = false, pasosCorrectos = pasoActual)
            return
        }

        // 2. Verificar si completó toda la secuencia (6 pasos)
        if (secuenciaUsuario.size == LONGITUD_SECUENCIA) {
            finalizarJuego(exito = true, pasosCorrectos = LONGITUD_SECUENCIA)
        }
    }

    private fun finalizarJuego(exito: Boolean, pasosCorrectos: Int) {
        testFinalizado = true

        // Calculamos puntaje: (Aciertos / 6) * 100
        val puntaje = ((pasosCorrectos.toDouble() / LONGITUD_SECUENCIA.toDouble()) * 100).toInt()
        val intentoActual = CortexManager.obtenerIntentoActual("t2")

        // Guardar métricas
        val details = mapOf("pasos_correctos" to pasosCorrectos, "total_pasos" to LONGITUD_SECUENCIA)
        CortexManager.logPerformanceMetric("t2", puntaje, details)
        CortexManager.guardarPuntaje("t2", puntaje)

        // LÓGICA DE EXONERACIÓN (>80%)
        // 5 de 6 es 83%. 4 de 6 es 66%.
        // Por tanto, si acierta 5 o 6, pasa. Si acierta 4 o menos, repite.

        if (intentoActual == 1 && puntaje < 80) {
            // Falló mucho en el primer intento -> REPETIR
            mostrarDialogoFin(
                titulo = "SECUENCIA FALLIDA ❌",
                mensaje = "Acertaste $pasosCorrectos de $LONGITUD_SECUENCIA pasos.\nNota: $puntaje%\n\nNecesitas mejorar en el segundo intento.",
                esReintento = true
            )
        } else {
            // Aprobó (>80) O ya es el segundo intento -> TERMINAR
            val titulo = if (puntaje == 100) "¡MEMORIA PERFECTA! 🧠" else "TEST FINALIZADO"
            mostrarDialogoFin(
                titulo = titulo,
                mensaje = "Resultado: $pasosCorrectos de $LONGITUD_SECUENCIA pasos correctos.\nNota Final: $puntaje%",
                esReintento = false
            )
        }
    }

    private fun mostrarDialogoFin(titulo: String, mensaje: String, esReintento: Boolean) {
        if (isFinishing || isDestroyed) return

        val textoBoton = if (esReintento) "INTENTO 2" else "SIGUIENTE TEST"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate()
                } else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }

    // --- CÁMARA (Sin cambios importantes) ---
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null

    private fun iniciarSentinelCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                if (isFinishing || isDestroyed) return@addListener
                cameraProvider = cameraProviderFuture.get()
                val previewView = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderSeq)
                preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                imageAnalyzer = ImageAnalysis.Builder().build().also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        if (!isFinishing && !testFinalizado) analizarRostro(imageProxy) else imageProxy.close()
                    }
                }
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostro(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            FaceDetection.getClient().process(image)
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testFinalizado = true
        imageAnalyzer?.clearAnalyzer()
        cameraProvider?.unbindAll()
    }
}