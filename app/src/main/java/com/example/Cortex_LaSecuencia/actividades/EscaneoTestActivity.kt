package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class EscaneoTestActivity : AppCompatActivity() {

    private lateinit var txtObjetivo: TextView
    private lateinit var gridNumeros: GridLayout

    private var numeroObjetivo = 0
    private var tiempoInicio: Long = 0
    private var juegoActivo = true
    private var clicsErroneos = 0 // ✅ NUEVO: Contador de errores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo_test)

        txtObjetivo = findViewById(R.id.txt_objetivo)
        gridNumeros = findViewById(R.id.grid_numeros)

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
                setOnClickListener { verificarSeleccion(this, num) }
            }
            gridNumeros.addView(celda)
        }
    }

    private fun verificarSeleccion(vista: TextView, numeroSeleccionado: Int) {
        if (!juegoActivo) return

        if (numeroSeleccionado == numeroObjetivo) {
            juegoActivo = false
            vista.setBackgroundColor(Color.parseColor("#10B981"))
            vista.setTextColor(Color.BLACK)
            calcularPuntajeYFinalizar()
        } else {
            clicsErroneos++ // ✅ INCREMENTA CONTADOR DE ERRORES
            vista.setBackgroundColor(Color.parseColor("#EF4444"))
            vista.alpha = 0.5f
            // El juego continúa, el usuario debe seguir buscando
        }
    }

    private fun calcularPuntajeYFinalizar() {
        val duracion = System.currentTimeMillis() - tiempoInicio
        val penalizacion = if (duracion > 1500) ((duracion - 1500) / 50).toInt() else 0
        val puntaje = (100 - penalizacion).coerceIn(0, 100)

        // --- ✅ REGISTRO DE MÉTRICAS DETALLADO ---
        val details = mapOf(
            "tiempo_total_ms" to duracion,
            "clics_erroneos" to clicsErroneos
        )
        CortexManager.logPerformanceMetric("t6", puntaje, details)

        finalizarActivity(puntaje, duracion)
    }

    private fun finalizarActivity(puntaje: Int, tiempoMs: Long) {
        if (isFinishing) return
        CortexManager.guardarPuntaje("t6", puntaje)

        val mensaje = if (puntaje >= 75) "¡Velocidad Óptima!" else "Reacción lenta."

        AlertDialog.Builder(this)
            .setTitle("ESCANEO COMPLETADO")
            .setMessage("Tiempo: ${tiempoMs}ms\nErrores: $clicsErroneos\nNota: $puntaje%")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}