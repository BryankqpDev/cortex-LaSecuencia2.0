package com.example.Cortex_LaSecuencia.actividades

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.logic.AdaptiveScoring
import com.example.Cortex_LaSecuencia.logic.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import java.util.Random

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TEST DE COORDINACIÓN (t4) - VERSIÓN RANDOMIZADA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Cambios implementados:
 * ✅ Scoring adaptativo con tiempo base variable
 * ✅ Factor de dificultad que afecta la evaluación
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class CoordinacionTestActivity : TestBaseActivity() {

    private lateinit var containerJuego: FrameLayout
    private lateinit var txtContador: TextView

    private var hitsCount = 0
    private val TARGET_HITS = 5
    private var startTime: Long = 0
    private var gameStarted = false
    private var intentoActual = 1

    private val random = Random()
    private var timerInicio: CountDownTimer? = null

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PARÁMETROS ALEATORIOS DE ESTA SESIÓN
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var sessionParams: TestSessionParams.CoordinacionParams

    override fun obtenerTestId(): String = "t4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)

        intentoActual = CortexManager.obtenerIntentoActual("t4")

        // ═══════════════════════════════════════════════════════════════════
        // ✅ GENERAR PARÁMETROS ÚNICOS PARA ESTA EJECUCIÓN
        // ═══════════════════════════════════════════════════════════════════
        sessionParams = TestSessionParams.generarCoordinacionParams()
        TestSessionParams.registrarParametros("t4", sessionParams)

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        containerJuego.post { startTest() }
    }

    private fun startTest() {
        hitsCount = 0
        gameStarted = false
        containerJuego.removeAllViews()
        updateProgressText()

        timerInicio = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!testFinalizado) {
                    val segundos = millisUntilFinished / 1000
                    txtContador.text = if (segundos > 0) "Prepárate... $segundos" else "¡YA!"
                }
            }

            override fun onFinish() {
                if (!testFinalizado) {
                    gameStarted = true
                    startTime = System.currentTimeMillis()
                    updateProgressText()
                    spawnDot()
                }
            }
        }.start()
    }

    private fun spawnDot() {
        if (!gameStarted || hitsCount >= TARGET_HITS || testFinalizado) return

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
        if (!gameStarted || testFinalizado) return

        hitsCount++
        updateProgressText()
        containerJuego.removeView(dot)

        if (hitsCount >= TARGET_HITS) {
            finishAttempt()
        } else {
            spawnDot()
        }
    }

    private fun finishAttempt() {
        gameStarted = false
        testFinalizado = true
        val totalTime = System.currentTimeMillis() - startTime

        // ═══════════════════════════════════════════════════════════════════
        // ✅ USAR SCORING ADAPTATIVO
        // ═══════════════════════════════════════════════════════════════════
        val scoreBase = AdaptiveScoring.calcularPuntajeCoordinacion(totalTime, sessionParams)
        val scoreFinal = AdaptiveScoring.aplicarPenalizacionAusencia(scoreBase, penalizacionPorAusencia)

        val details = mapOf(
            "tiempo_total_ms" to totalTime,
            "penaliz_ausencia" to penalizacionPorAusencia,
            "tiempo_base_config" to sessionParams.tiempoBaseMs,
            "factor_dificultad" to sessionParams.factorDificultad
        )
        CortexManager.logPerformanceMetric("t4", scoreFinal, details)
        CortexManager.guardarPuntaje("t4", scoreFinal)

        if (intentoActual == 1 && scoreFinal < 80) {
            recreate()
        } else {
            showFinalDialog(scoreFinal, totalTime)
        }
    }

    private fun updateProgressText() {
        txtContador.text = "INTENTO $intentoActual/2 | ATRAPADOS: $hitsCount / $TARGET_HITS"
    }

    private fun showFinalDialog(score: Int, timeMs: Long) {
        val mensaje = "Tiempo: ${timeMs}ms\nNota: $score%\nPenalización ausencia: -$penalizacionPorAusencia pts"
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

    override fun onTestPaused() {
        gameStarted = false
        txtContador.text = "PAUSA POR AUSENCIA"
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            gameStarted = true
            updateProgressText()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerInicio?.cancel()
    }
}