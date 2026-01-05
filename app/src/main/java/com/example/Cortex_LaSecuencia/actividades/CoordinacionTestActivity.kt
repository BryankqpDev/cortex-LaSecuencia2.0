package com.example.Cortex_LaSecuencia.actividades

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import java.util.Random

class CoordinacionTestActivity : AppCompatActivity() {

    // Referencias UI
    private lateinit var containerJuego: FrameLayout
    private lateinit var txtContador: TextView
    private lateinit var lblIntento: TextView // 1. Nueva referencia para el XML corregido

    // Variables del juego
    private var hitsCount = 0
    private val TARGET_HITS = 5
    private var startTime: Long = 0
    private var gameStarted = false
    private var intentoActual = 1

    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el XML se llame exactamente así:
        setContentView(R.layout.activity_coordinacion_test)

        // Inicialización de Vistas
        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)
        lblIntento = findViewById(R.id.lbl_t10) // Vinculamos el ID del XML corregido

        // Obtener intento desde tu Manager
        intentoActual = CortexManager.obtenerIntentoActual("t4")

        // Seteamos el texto del intento inmediatamente
        lblIntento.text = "INTENTO $intentoActual/2"

        // Iniciar test
        containerJuego.post { startTest() }
    }

    private fun startTest() {
        hitsCount = 0
        gameStarted = false
        containerJuego.removeAllViews()

        // 2. Durante la cuenta regresiva, mostramos el mensaje en txtContador
        lblIntento.text = "INTENTO $intentoActual/2"

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                // Usamos el color blanco o llamativo para la cuenta regresiva
                txtContador.text = if (segundos > 0) "Prepárate... $segundos" else "¡YA!"
            }

            override fun onFinish() {
                gameStarted = true
                startTime = System.currentTimeMillis()
                updateProgressText() // Aquí cambiamos al texto de puntaje
                spawnDot()
            }
        }.start()
    }

    private fun spawnDot() {
        if (!gameStarted || hitsCount >= TARGET_HITS) return

        val areaWidth = containerJuego.width
        val areaHeight = containerJuego.height

        // Seguridad por si la vista no se ha medido aún
        if (areaWidth == 0 || areaHeight == 0) {
            containerJuego.post { spawnDot() }
            return
        }

        // Crear el punto dinámicamente
        val dotSizePx = (50 * resources.displayMetrics.density).toInt()
        val dot = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dotSizePx, dotSizePx).apply {
                // Posición aleatoria dentro de los límites
                leftMargin = random.nextInt((areaWidth - dotSizePx).coerceAtLeast(1))
                topMargin = random.nextInt((areaHeight - dotSizePx).coerceAtLeast(1))
            }
            // NOTA: Asegúrate de tener este drawable (ver abajo si te falta)
            setBackgroundResource(R.drawable.circulo_amarillo)
            setOnClickListener { onDotClicked(this) }
        }
        containerJuego.addView(dot)
    }

    private fun onDotClicked(dot: View) {
        if (!gameStarted) return

        hitsCount++
        updateProgressText() // Actualizamos contador
        containerJuego.removeView(dot)

        if (hitsCount >= TARGET_HITS) {
            finishAttempt()
        } else {
            spawnDot()
        }
    }

    // 3. Método actualizado para usar las dos etiquetas separadas del XML
    private fun updateProgressText() {
        lblIntento.text = "INTENTO $intentoActual/2"
        txtContador.text = "ATRAPADOS: $hitsCount / $TARGET_HITS"
    }

    private fun finishAttempt() {
        gameStarted = false
        val totalTime = System.currentTimeMillis() - startTime
        val score = calculateScore(totalTime)

        // Lógica de guardado
        val details = mapOf("tiempo_total_ms" to totalTime, "errores_distractor" to 0)
        CortexManager.logPerformanceMetric("t4", score, details)
        CortexManager.guardarPuntaje("t4", score)

        if (intentoActual == 1 && score < 80) {
            // Reinicia la actividad para el segundo intento
            recreate()
        } else {
            showFinalDialog(score, totalTime)
        }
    }

    private fun calculateScore(timeMs: Long): Int {
        val baseTime = 3000L // 3 segundos es el tiempo ideal
        if (timeMs <= baseTime) return 100
        val penalty = ((timeMs - baseTime) / 50).toInt()
        return (100 - penalty).coerceIn(0, 100)
    }

    private fun showFinalDialog(score: Int, timeMs: Long) {
        val mensaje = "Tiempo: ${timeMs}ms\nNota: $score%"
        AlertDialog.Builder(this)
            .setTitle("COORDINACIÓN COMPLETADA")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameStarted = false
    }
}