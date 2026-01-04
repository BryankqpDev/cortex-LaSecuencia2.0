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
            findViewById(R.id.btn_1), findViewById(R.id.btn_2), findViewById(R.id.btn_3),
            findViewById(R.id.btn_4), findViewById(R.id.btn_5), findViewById(R.id.btn_6),
            findViewById(R.id.btn_7), findViewById(R.id.btn_8), findViewById(R.id.btn_9)
        )

        configurarClicks()
        iniciarSentinelCamara()
        
        // Iniciar el primer nivel después de un breve retraso
        Handler(Looper.getMainLooper()).postDelayed({ iniciarSiguienteNivel() }, 1500)
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

    private fun iniciarSiguienteNivel() {
        if (testFinalizado || isFinishing) return

        esTurnoDelUsuario = false
        secuenciaUsuario.clear()
        // Añade un nuevo paso aleatorio a la secuencia (ahora hasta 9)
        secuenciaGenerada.add(Random.nextInt(0, 9))

        txtInstruccion.text = "NIVEL $nivelActual: OBSERVA"
        txtInstruccion.setTextColor(Color.CYAN)

        mostrarSecuenciaGenerada()
    }

    private fun mostrarSecuenciaGenerada() {
        val handler = Handler(Looper.getMainLooper())
        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                if (!testFinalizado) iluminarBoton(botonIndex, false)
                // Cuando la secuencia termina, da el turno al usuario
                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        if (!testFinalizado) {
                            esTurnoDelUsuario = true
                            txtInstruccion.text = "REPLICA LA SECUENCIA"
                            txtInstruccion.setTextColor(Color.WHITE)
                        }
                    }, 800)
                }
            }, (i + 1) * 1000L) // 1 segundo por paso
        }
    }

    private fun iluminarBoton(index: Int, esUsuario: Boolean) {
        val originalColor = Color.parseColor("#1E293B")
        val highlightColor = if (esUsuario) Color.parseColor("#F59E0B") else Color.parseColor("#00F0FF")
        
        botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(highlightColor)
        Handler(Looper.getMainLooper()).postDelayed({
            botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(originalColor)
        }, 400) // Iluminación más corta
    }

    private fun verificarEntrada() {
        val indexUltimoIntento = secuenciaUsuario.size - 1

        // Si el usuario se equivoca
        if (secuenciaUsuario[indexUltimoIntento] != secuenciaGenerada[indexUltimoIntento]) {
            gestionarFallo()
            return
        }

        // Si el usuario completa la secuencia del nivel actual
        if (secuenciaUsuario.size == secuenciaGenerada.size) {
            nivelActual++
            // Si supera el nivel 5, ha ganado
            if (nivelActual > 5) {
                finalizarConExito()
            } else {
                txtInstruccion.text = "¡BIEN! SIGUIENTE NIVEL..."
                txtInstruccion.setTextColor(Color.GREEN)
                esTurnoDelUsuario = false
                Handler(Looper.getMainLooper()).postDelayed({ iniciarSiguienteNivel() }, 1500)
            }
        }
    }

    private fun gestionarFallo() {
        testFinalizado = true
        CortexManager.guardarPuntaje("t2", 0) // Guardar puntaje de fallo
        val eraPrimerIntento = CortexManager.obtenerIntentoActual("t2") == 1

        if (eraPrimerIntento) {
            // Reinicia para el segundo intento sin mostrar diálogo
            recreate()
        } else {
            // Muestra el diálogo de fallo final en el segundo intento
            mostrarDialogoFalloFinal()
        }
    }

    private fun mostrarDialogoFalloFinal() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("MEMORIA: FALLIDO ❌")
            .setMessage("No se completó la secuencia. La evaluación continuará.")
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    private fun finalizarConExito() {
        if (testFinalizado) return
        testFinalizado = true
        CortexManager.guardarPuntaje("t2", 100)

        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("¡MEMORIA VALIDADA! 🧠✅")
            .setMessage("Nivel 2 completado con éxito.")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
    
    // --- CÓDIGO DE CÁMARA (Sin cambios) ---
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
                
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            if (!isFinishing && !isDestroyed && !testFinalizado) {
                                analizarRostro(imageProxy)
                            } else {
                                imageProxy.close()
                            }
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
                .addOnSuccessListener { faces -> { /* No-op */ } }
                .addOnCompleteListener { imageProxy.close() }
                .addOnFailureListener { imageProxy.close() }
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
    
    override fun onPause() {
        super.onPause()
        imageAnalyzer?.clearAnalyzer()
    }
}