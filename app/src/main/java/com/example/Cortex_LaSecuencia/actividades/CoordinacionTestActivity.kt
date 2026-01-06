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
    private lateinit var lblIntento: TextView

    // Variables del juego
    private var hitsCount = 0
    private val TARGET_HITS = 5
    private var startTime: Long = 0
    private var gameStarted = false
    private var intentoActual = 1

    private val random = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)
        lblIntento = findViewById(R.id.lbl_t10)

        intentoActual = CortexManager.obtenerIntentoActual("t4")
        lblIntento.text = "INTENTO $intentoActual/2"

        containerJuego.post { startTest() }
    }

    private fun startTest() {
        hitsCount = 0
        gameStarted = false
        containerJuego.removeAllViews()
        lblIntento.text = "INTENTO $intentoActual/2"
        txtContador.text = "PREPÁRATE..."

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                txtContador.text = if (segundos > 0) "Comienza en... $segundos" else "¡YA!"
            }

            override fun onFinish() {
                gameStarted = true
                startTime = System.currentTimeMillis()
                updateProgressText()
                spawnDot()
            }
        }.start()
    }

    private fun spawnDot() {
        if (!gameStarted || hitsCount >= TARGET_HITS) return

        val areaWidth = containerJuego.width
        val areaHeight = containerJuego.height

        if (areaWidth == 0 || areaHeight == 0) {
            containerJuego.post { spawnDot() }
            return
        }

        val dotSizePx = (50 * resources.displayMetrics.density).toInt()
        val dot = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dotSizePx, dotSizePx).apply {
                leftMargin = random.nextInt((areaWidth - dotSizePx).coerceAtLeast(1))
                topMargin = random.nextInt((areaHeight - dotSizePx).coerceAtLeast(1))
            }
            setBackgroundResource(R.drawable.circulo_amarillo)
            setOnClickListener { onDotClicked(this) }
        }
        containerJuego.addView(dot)
    }

    private fun onDotClicked(dot: View) {
        if (!gameStarted) return

        hitsCount++
        updateProgressText()
        containerJuego.removeView(dot)

        if (hitsCount >= TARGET_HITS) {
            finishAttempt()
        } else {
            spawnDot()
        }
    }

    private fun updateProgressText() {
        lblIntento.text = "INTENTO $intentoActual/2"
        txtContador.text = "ATRAPADOS: $hitsCount / $TARGET_HITS"
    }

    // --- AQUÍ ESTÁ LA LÓGICA DE EXONERACIÓN CORREGIDA ---
    private fun finishAttempt() {
        gameStarted = false
        val totalTime = System.currentTimeMillis() - startTime
        val score = calculateScore(totalTime)

        // Guardado de métricas
        val details = mapOf("tiempo_total_ms" to totalTime, "velocidad_media" to totalTime/TARGET_HITS)
        CortexManager.logPerformanceMetric("t4", score, details)
        CortexManager.guardarPuntaje("t4", score)

        // Lógica de decisión: ¿Repite o Avanza?
        if (intentoActual == 1 && score < 80) {
            // Reprobó el primer intento -> REPETIR
            showFinalDialog(score, totalTime, esReintento = true)
        } else {
            // Aprobó (>80) o es el segundo intento -> AVANZAR
            showFinalDialog(score, totalTime, esReintento = false)
        }
    }

    private fun calculateScore(timeMs: Long): Int {
        // Base: 3000ms (3 segundos) para 5 toques es lo ideal (100 pts)
        // Penalización: Pierdes 1 punto por cada 50ms extra.
        // Si tardas 4000ms -> Pierdes 20 pts -> Nota 80 (Límite para aprobar)
        val baseTime = 3000L
        if (timeMs <= baseTime) return 100
        val penalty = ((timeMs - baseTime) / 50).toInt()
        return (100 - penalty).coerceIn(0, 100)
    }

    // --- DIALOGO INTELIGENTE QUE MANEJA EL REINTENTO ---
    private fun showFinalDialog(score: Int, timeMs: Long, esReintento: Boolean) {
        if (isFinishing) return // Evita errores si la app se cerró

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "MUY LENTO ⚠️"
            mensaje = "Tiempo: ${timeMs}ms\nNota: $score%\n\nNecesitas ser más rápido (mínimo 80%) para aprobar. Tienes un segundo intento."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if(score >= 80) "¡RÁPIDO Y PRECISO! ⚡" else "TEST FINALIZADO"
            mensaje = "Tiempo: ${timeMs}ms\nNota Final: $score%"
            textoBoton = "SIGUIENTE TEST"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate() // Reinicia la Activity para el intento 2
                } else {
                    CortexManager.navegarAlSiguiente(this) // Se va a la siguiente pantalla
                    finish()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameStarted = false
    }
}