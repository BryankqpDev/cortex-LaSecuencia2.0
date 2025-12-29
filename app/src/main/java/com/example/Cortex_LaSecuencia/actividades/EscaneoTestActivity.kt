package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class EscaneoTestActivity : AppCompatActivity() {

    private lateinit var txtObjetivo: TextView
    private lateinit var gridNumeros: GridLayout

    private var numeroObjetivo = 0
    private var tiempoInicio: Long = 0
    private var juegoActivo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo_test)

        txtObjetivo = findViewById(R.id.txt_objetivo)
        gridNumeros = findViewById(R.id.grid_numeros)

        iniciarPrueba()
    }

    private fun iniciarPrueba() {
        // 1. Definir el objetivo (10 a 99)
        numeroObjetivo = Random.nextInt(10, 100)
        txtObjetivo.text = numeroObjetivo.toString()

        // 2. Preparar la lista de números (1 objetivo + 15 distractores)
        val listaNumeros = mutableListOf<Int>()
        listaNumeros.add(numeroObjetivo)

        while (listaNumeros.size < 16) {
            val distractor = Random.nextInt(10, 100)
            // Evitar duplicar el objetivo en los distractores
            if (distractor != numeroObjetivo) {
                listaNumeros.add(distractor)
            }
        }

        // Mezclar para que el objetivo aparezca en cualquier lugar
        listaNumeros.shuffle()

        // 3. Llenar la cuadrícula visualmente
        llenarGrid(listaNumeros)

        // 4. Iniciar cronómetro
        tiempoInicio = System.currentTimeMillis()
    }

    private fun llenarGrid(numeros: List<Int>) {
        gridNumeros.removeAllViews()

        for (num in numeros) {
            val celda = TextView(this)

            // Estilo visual de cada celda (replicando el CSS .cell)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8) // Gap entre celdas

            celda.layoutParams = params
            celda.text = num.toString()
            celda.textSize = 24f
            celda.setTextColor(Color.WHITE)
            celda.gravity = Gravity.CENTER
            celda.setBackgroundColor(Color.parseColor("#1E293B")) // Fondo oscuro azulado

            // Evento de clic
            celda.setOnClickListener {
                verificarSeleccion(celda, num)
            }

            gridNumeros.addView(celda)
        }
    }

    private fun verificarSeleccion(vista: TextView, numeroSeleccionado: Int) {
        if (!juegoActivo) return

        if (numeroSeleccionado == numeroObjetivo) {
            // ¡CORRECTO!
            juegoActivo = false
            vista.setBackgroundColor(Color.parseColor("#10B981")) // Verde
            vista.setTextColor(Color.BLACK)

            calcularPuntajeYFinalizar()
        } else {
            // ERROR (Distractor)
            // Solo feedback visual, no termina el juego (el usuario debe seguir buscando)
            vista.setBackgroundColor(Color.parseColor("#EF4444")) // Rojo
            vista.alpha = 0.5f
        }
    }

    private fun calcularPuntajeYFinalizar() {
        val tiempoFinal = System.currentTimeMillis()
        val duracion = tiempoFinal - tiempoInicio

        // Fórmula de tu HTML: Math.round(100-((Date.now()-st-1500)/50))
        // Base: 1500ms (1.5s). Por cada 50ms extra, baja 1 punto.
        var puntaje = 100
        if (duracion > 1500) {
            val penalizacion = ((duracion - 1500) / 50).toInt()
            puntaje = 100 - penalizacion
        }

        // Límites (0 a 100)
        if (puntaje < 0) puntaje = 0
        if (puntaje > 100) puntaje = 100

        finalizarActivity(puntaje, duracion)
    }

    private fun finalizarActivity(puntaje: Int, tiempoMs: Long) {
        if (isFinishing) return

        CortexManager.guardarPuntaje("t6", puntaje)

        val mensaje = if (puntaje >= 75) "¡Velocidad Óptima!" else "Reacción lenta."

        AlertDialog.Builder(this)
            .setTitle("ESCANEO COMPLETADO")
            .setMessage("Tiempo: ${tiempoMs}ms\nNota: $puntaje%\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}