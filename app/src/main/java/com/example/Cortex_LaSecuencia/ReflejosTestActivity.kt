package com.example.Cortex_LaSecuencia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random
// Agrega estos imports al principio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

class ReflejosTestActivity : AppCompatActivity() {

    private lateinit var targetCircle: View
    private lateinit var mainLayout: ConstraintLayout
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)

        // 1. Iniciamos la vigilancia de Sentinel
        iniciarSentinelCamara()

        // 2. IMPORTANTE: Iniciamos el ciclo del juego
        esperarYAparecer()

        // 3. Lógica completa del botón para que el círculo salte
        targetCircle.setOnClickListener {
            val reactionTime = System.currentTimeMillis() - startTime
            Toast.makeText(this, "Reflejo: ${reactionTime}ms", Toast.LENGTH_SHORT).show()

            targetCircle.visibility = View.GONE
            esperarYAparecer() // Vuelve a lanzarlo después de tocarlo
        }
    }

    private fun esperarYAparecer() {
        // Espera un tiempo aleatorio entre 1 y 3 segundos
        val delay = Random.nextLong(1000, 3000)

        Handler(Looper.getMainLooper()).postDelayed({
            moverCirculo()
        }, delay)
    }

    private fun moverCirculo() {
        // Calcular posiciones aleatorias dentro de la pantalla
        val width = mainLayout.width - targetCircle.width
        val height = mainLayout.height - targetCircle.height

        if (width > 0 && height > 0) {
            targetCircle.x = Random.nextInt(0, width).toFloat()
            targetCircle.y = Random.nextInt(0, height).toFloat()

            targetCircle.visibility = View.VISIBLE
            startTime = System.currentTimeMillis()
        }
    }
    private fun iniciarSentinelCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configuramos la Preview
            val preview = Preview.Builder().build().also {
                // VINCULACIÓN CRUCIAL:
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                // Unimos la cámara al ciclo de vida de la actividad
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                // Si hay un error, lo veremos en un Toast
                Toast.makeText(this, "Error de cámara: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}