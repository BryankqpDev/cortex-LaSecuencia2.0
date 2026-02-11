package com.example.Cortex_LaSecuencia.utils

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Cortex_LaSecuencia.CortexManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection

abstract class TestBaseActivity : AppCompatActivity() {

    protected var testId: String = ""
    protected var testFinalizado = false
    protected var fueInterrumpido = false

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private var camera: Camera? = null

    protected var estaEnPausaPorAusencia = false
    private val handlerAusencia = Handler(Looper.getMainLooper())

    private var overlayAlerta: LinearLayout? = null
    private var txtContadorAlerta: TextView? = null
    protected var penalizacionPorAusencia = 0

    private var estaProcesandoFrame = false

    // ✅ NUEVO: Flag para saber si es un reintento
    private var esReintento = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testId = obtenerTestId()

        // ✅ NUEVO: Detectar si es un reintento
        esReintento = CortexManager.obtenerIntentoActual(testId) == 2

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!testFinalizado) {
                    Toast.makeText(this@TestBaseActivity, "Termina la prueba antes de salir", Toast.LENGTH_SHORT).show()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    protected fun configurarSentinel(previewView: PreviewView, statusText: TextView?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return
        }

        // ✅ CRÍTICO: Si es reintento, esperar 300ms antes de inicializar cámara
        // Esto permite que la instancia anterior libere completamente los recursos
        val delayInicio = if (esReintento) 300L else 0L

        Handler(Looper.getMainLooper()).postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            inicializarCamara(previewView, statusText)
        }, delayInicio)
    }

    private fun inicializarCamara(previewView: PreviewView, statusText: TextView?) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                if (isFinishing || isDestroyed) return@addListener

                cameraProvider = cameraProviderFuture.get()

                // ✅ Desvincular todos los casos de uso ANTES de configurar nuevos
                cameraProvider?.unbindAll()

                // ✅ Configurar Preview con optimizaciones
                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // ✅ Configurar ImageAnalysis con resolución optimizada
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                            // ✅ Prevenir procesamiento concurrente
                            if (!testFinalizado && !estaProcesandoFrame) {
                                analizarRostroSentinel(imageProxy, statusText)
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                // ✅ Vincular casos de uso y obtener el CameraControl
                camera = cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalyzer
                )

                // ✅ CRÍTICO: Activar auto-enfoque continuo y exposición automática
                camera?.cameraControl?.apply {
                    // Resetear zoom para evitar configuraciones previas
                    try {
                        setLinearZoom(0f)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Cancelar cualquier bloqueo de enfoque/exposición previo
                    cancelFocusAndMetering()

                    // CameraX maneja auto-focus y auto-exposure por defecto,
                    // pero aseguramos que no haya interferencias previas
                }

                // ✅ Configurar info de exposición/ISO si es necesario (opcional)
                camera?.cameraInfo?.exposureState?.let { exposureState ->
                    // Log para debug (opcional)
                    // Log.d("TestBase", "Exposure compensation: ${exposureState.exposureCompensationIndex}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al inicializar cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analizarRostroSentinel(imageProxy: ImageProxy, statusText: TextView?) {
        estaProcesandoFrame = true

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            FaceDetection.getClient().process(image)
                .addOnSuccessListener { faces ->
                    if (testFinalizado) return@addOnSuccessListener
                    gestionarAusencia(faces.isEmpty(), statusText)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    estaProcesandoFrame = false
                }
        } else {
            imageProxy.close()
            estaProcesandoFrame = false
        }
    }

    private fun gestionarAusencia(ausente: Boolean, statusText: TextView?) {
        if (ausente && !estaEnPausaPorAusencia) {
            estaEnPausaPorAusencia = true
            mostrarOverlayAlerta()
            onTestPaused()
            iniciarCuentaRegresivaAusencia()
        } else if (!ausente && estaEnPausaPorAusencia) {
            estaEnPausaPorAusencia = false
            ocultarOverlayAlerta()
            onTestResumed()
            handlerAusencia.removeCallbacksAndMessages(null)
        }
    }

    private fun mostrarOverlayAlerta() {
        if (overlayAlerta != null) return

        overlayAlerta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#CC000000"))
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            addView(TextView(context).apply {
                text = "⚠️ ROSTRO NO DETECTADO"
                setTextColor(Color.RED)
                textSize = 28f
                gravity = Gravity.CENTER
                setPadding(20, 40, 20, 10)
            })

            txtContadorAlerta = TextView(context).apply {
                text = "5"
                setTextColor(Color.WHITE)
                textSize = 70f
                gravity = Gravity.CENTER
            }
            addView(txtContadorAlerta)

            addView(TextView(context).apply {
                text = "Regresa a la cámara\nEvita penalizaciones por distracción"
                setTextColor(Color.LTGRAY)
                textSize = 16f
                gravity = Gravity.CENTER
            })
        }
        (window.decorView as ViewGroup).addView(overlayAlerta)
    }

    private fun ocultarOverlayAlerta() {
        overlayAlerta?.let {
            (window.decorView as ViewGroup).removeView(it)
            overlayAlerta = null
            txtContadorAlerta = null
        }
    }

    private fun iniciarCuentaRegresivaAusencia() {
        var segundosRestantes = 5
        val runnable = object : Runnable {
            override fun run() {
                if (estaEnPausaPorAusencia) {
                    txtContadorAlerta?.text = segundosRestantes.toString()
                    if (segundosRestantes <= 0) {
                        penalizacionPorAusencia += 10
                        Toast.makeText(this@TestBaseActivity, "⚠️ Distracción prolongada: -10 pts", Toast.LENGTH_SHORT).show()
                        segundosRestantes = 5
                        handlerAusencia.postDelayed(this, 1000)
                    } else {
                        segundosRestantes--
                        handlerAusencia.postDelayed(this, 1000)
                    }
                }
            }
        }
        handlerAusencia.post(runnable)
    }

    private fun anularIntentoPorSeguridad(motivo: String) {
        if (testFinalizado) return
        testFinalizado = true
        ocultarOverlayAlerta()
        CortexManager.guardarPuntaje(testId, 0)
        if (CortexManager.obtenerIntentoActual(testId) == 1) {
            Toast.makeText(this, "Prueba anulada: $motivo", Toast.LENGTH_LONG).show()
            recreate()
        } else {
            CortexManager.navegarAlSiguiente(this)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!testFinalizado) fueInterrumpido = true

        // ✅ CRÍTICO: Pausar el análisis correctamente
        imageAnalyzer?.clearAnalyzer()

        // ✅ NUEVO: Desvincular la cámara para liberar recursos
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (fueInterrumpido && !testFinalizado) {
            fueInterrumpido = false
            anularIntentoPorSeguridad("Interrupción externa")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testFinalizado = true
        estaProcesandoFrame = false

        // ✅ Limpiar todos los recursos
        handlerAusencia.removeCallbacksAndMessages(null)
        imageAnalyzer?.clearAnalyzer()

        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // ✅ CRÍTICO: Dar tiempo al sistema para liberar antes de anular referencias
        Handler(Looper.getMainLooper()).postDelayed({
            cameraProvider = null
            camera = null
            preview = null
            imageAnalyzer = null
        }, 100)

        ocultarOverlayAlerta()
    }

    abstract fun obtenerTestId(): String
    open fun onTestPaused() {}
    open fun onTestResumed() {}
}