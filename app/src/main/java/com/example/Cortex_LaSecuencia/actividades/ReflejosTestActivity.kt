package com.example.Cortex_LaSecuencia.actividades

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.AudioManager.TipoSonido
import com.example.Cortex_LaSecuencia.utils.ScoreOverlay
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
    
    // Variables de cámara para liberación correcta
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)
        txtInstruccion = findViewById(R.id.txt_instruccion)

        targetCircle.visibility = View.GONE
        
        // Verificar permisos antes de iniciar cámara (como en HTML)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            txtInstruccion.text = "Posiciona tu rostro frente a la cámara..."
            iniciarSentinelCamara()
        } else {
            txtInstruccion.text = "⚠️ Permiso de cámara requerido"
            txtInstruccion.setTextColor(Color.RED)
            // Solicitar permisos
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }

        targetCircle.setOnClickListener {
            if (!testFinalizado) {
                val reactionTime = System.currentTimeMillis() - startTime
                aciertos++

                // GUARDAMOS EL TIEMPO
                sumaTiemposReaccion += reactionTime

                txtInstruccion.text = "¡RÁPIDO! ${reactionTime}ms"
                txtInstruccion.setTextColor(Color.GREEN)

                // Sonido de éxito
                AudioManager.reproducirSonido(TipoSonido.CLICK)

                targetCircle.visibility = View.GONE

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
        // Verificar permisos nuevamente antes de iniciar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                if (isFinishing || isDestroyed) return@addListener
                
                cameraProvider = cameraProviderFuture.get()
                val previewView = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
                
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
                txtInstruccion.text = "Sentinel activado. Esperando detección de rostro..."
                txtInstruccion.setTextColor(Color.WHITE)
            } catch (e: Exception) {
                Toast.makeText(this, "Error Sentinel Hardware: ${e.message}", Toast.LENGTH_SHORT).show()
                txtInstruccion.text = "Error al iniciar cámara"
                txtInstruccion.setTextColor(Color.RED)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                txtInstruccion.text = "Posiciona tu rostro frente a la cámara..."
                txtInstruccion.setTextColor(Color.WHITE)
                iniciarSentinelCamara()
            } else {
                txtInstruccion.text = "⚠️ Permiso denegado. No se puede continuar sin cámara."
                txtInstruccion.setTextColor(Color.RED)
                AlertDialog.Builder(this)
                    .setTitle("Cámara Requerida")
                    .setMessage("Sentinel necesita acceso a la cámara. Por favor, otorga el permiso en Configuración.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostro(imageProxy: ImageProxy) {
        // Verificar que la Activity sigue activa
        if (isFinishing || isDestroyed || testFinalizado) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = FaceDetection.getClient()

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (isFinishing || isDestroyed || testFinalizado) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }
                    
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
                .addOnFailureListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
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
        val promedioMs = if (aciertos > 0) sumaTiemposReaccion / aciertos else 0

        // Usar sistema de intentos (como en HTML: nextRound)
        val intentoActual = CortexManager.obtenerIntentoActual("t1")
        val esPrimerIntento = intentoActual == 1

        if (esPrimerIntento) {
            if (score >= 95) {
                // Excelente, pasa directo
                CortexManager.guardarPuntaje("t1", score, true)
                ScoreOverlay.mostrar(this, score, "¡EXCELENTE! 😎✅") {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            } else {
                // Necesita segundo intento
                CortexManager.guardarPuntaje("t1", score, true)
                ScoreOverlay.mostrar(this, score, "INTENTO 1 REGISTRADO.") {
                    // Reiniciar para segundo intento
                    testFinalizado = false
                    aciertos = 0
                    intentosTotales = 0
                    sumaTiemposReaccion = 0
                    juegoIniciado = false
                    usuarioPresente = false
                    txtInstruccion.text = "INTENTO 2/2 - Posiciona tu rostro..."
                    iniciarTemporizador()
                }
            }
        } else {
            // Segundo intento - promediar
            CortexManager.guardarPuntaje("t1", score, true)
            val puntajeTemporal = CortexManager.obtenerPuntajeTemporal("t1") ?: score
            val promedio = (puntajeTemporal + score) / 2
            CortexManager.guardarPuntaje("t1", promedio, true)
            
            ScoreOverlay.mostrar(this, promedio, "MÓDULO FINALIZADO.") {
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
        }
    }

    private fun reprobarPorSeguridad(motivo: String) {
        testFinalizado = true
        cancelarProximoCirculo()
        targetCircle.visibility = View.GONE
        AudioManager.reproducirSonido(TipoSonido.ERROR)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("PRUEBA ANULADA")
            .setMessage("Se perdió el contacto visual.\nEvaluación cancelada por seguridad.")
            .setCancelable(false)
            .setPositiveButton("VOLVER A EMPEZAR") { _, _ -> 
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        testFinalizado = true
        cancelarProximoCirculo()
        
        // Liberar cámara correctamente
        imageAnalyzer?.clearAnalyzer()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignorar errores al desvincular
        }
        imageAnalyzer = null
        preview = null
        cameraProvider = null
    }
    
    override fun onPause() {
        super.onPause()
        // Pausar análisis cuando la Activity no está visible para evitar procesamiento innecesario
        if (!testFinalizado) {
            imageAnalyzer?.clearAnalyzer()
        }
    }
}