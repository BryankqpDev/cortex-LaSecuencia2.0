package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.logic.AdaptiveScoring
import com.example.Cortex_LaSecuencia.logic.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TEST DE ESCANEO VISUAL (t6) - VERSIÓN RANDOMIZADA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Cambios implementados:
 * ✅ Scoring adaptativo con tiempo base variable
 * ✅ Factor de penalización ajustable
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class EscaneoTestActivity : TestBaseActivity() {

    private lateinit var txtObjetivo: TextView
    private lateinit var gridNumeros: GridLayout
    private lateinit var txtFeedback: TextView

    private var numeroObjetivo = 0
    private var tiempoInicio: Long = 0
    private var juegoActivo = true
    private var clicsErroneos = 0

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PARÁMETROS ALEATORIOS DE ESTA SESIÓN
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var sessionParams: TestSessionParams.EscaneoParams

    override fun obtenerTestId(): String = "t6"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo_test)

        txtObjetivo = findViewById(R.id.txt_objetivo)
        gridNumeros = findViewById(R.id.grid_numeros)
        txtFeedback = findViewById(R.id.txt_instruccion)

        // ═══════════════════════════════════════════════════════════════════
        // ✅ GENERAR PARÁMETROS ÚNICOS PARA ESTA EJECUCIÓN
        // ═══════════════════════════════════════════════════════════════════
        sessionParams = TestSessionParams.generarEscaneoParams()
        TestSessionParams.registrarParametros("t6", sessionParams)

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        iniciarPrueba()
    }

    private fun iniciarPrueba() {
        numeroObjetivo = Random.nextInt(10, 100)
        txtObjetivo.text = numeroObjetivo.toString()

        val listaNumeros = mutableListOf(numeroObjetivo)
        while (listaNumeros.size < 16) {
            val distractor = Random.nextInt(10, 100)
            if (distractor != numeroObjetivo) listaNumeros.add(distractor)
        }
        listaNumeros.shuffle()

        llenarGrid(listaNumeros)
        tiempoInicio = System.currentTimeMillis()
    }

    private fun llenarGrid(numeros: List<Int>) {
        gridNumeros.removeAllViews()
        for (num in numeros) {
            val celda = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                text = num.toString()
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#1E293B"))
                setOnClickListener { if (juegoActivo && !testFinalizado) verificarSeleccion(this, num) }
            }
            gridNumeros.addView(celda)
        }
    }

    private fun verificarSeleccion(vista: TextView, numeroSeleccionado: Int) {
        if (numeroSeleccionado == numeroObjetivo) {
            juegoActivo = false
            vista.setBackgroundColor(Color.parseColor("#10B981"))
            vista.setTextColor(Color.BLACK)
            calcularPuntajeYFinalizar()
        } else {
            clicsErroneos++
            vista.setBackgroundColor(Color.parseColor("#EF4444"))
            vista.alpha = 0.5f
        }
    }

    private fun calcularPuntajeYFinalizar() {
        testFinalizado = true
        val duracion = System.currentTimeMillis() - tiempoInicio

        // ═══════════════════════════════════════════════════════════════════
        // ✅ USAR SCORING ADAPTATIVO
        // ═══════════════════════════════════════════════════════════════════
        val puntajeBase = AdaptiveScoring.calcularPuntajeEscaneo(duracion, sessionParams)
        val puntajeFinal = AdaptiveScoring.aplicarPenalizacionAusencia(puntajeBase, penalizacionPorAusencia)

        val details = mapOf(
            "tiempo_total_ms" to duracion,
            "clics_erroneos" to clicsErroneos,
            "penaliz_ausencia" to penalizacionPorAusencia,
            "tiempo_base_config" to sessionParams.tiempoBaseMs,
            "factor_penalizacion" to sessionParams.factorPenalizacion
        )
        CortexManager.logPerformanceMetric("t6", puntajeFinal, details)
        CortexManager.guardarPuntaje("t6", puntajeFinal)

        finalizarActivity(puntajeFinal, duracion)
    }

    private fun finalizarActivity(puntaje: Int, tiempoMs: Long) {
        if (isFinishing) return
        val mensaje = if (puntaje >= 75) "¡Velocidad Óptima!" else "Reacción lenta."

        AlertDialog.Builder(this)
            .setTitle("ESCANEO COMPLETADO")
            .setMessage("Tiempo: ${tiempoMs}ms\nNota: $puntaje%\nPenalización ausencia: -$penalizacionPorAusencia pts\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onTestPaused() {
        juegoActivo = false
        gridNumeros.visibility = View.INVISIBLE
        txtObjetivo.text = "PAUSA"
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            juegoActivo = true
            gridNumeros.visibility = View.VISIBLE
            txtObjetivo.text = numeroObjetivo.toString()
        }
    }
}