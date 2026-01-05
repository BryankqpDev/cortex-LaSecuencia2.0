package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
    private var clicsErroneos = 0
    private var intentoActual = 1 // Variable para controlar el intento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escaneo_test)

        txtObjetivo = findViewById(R.id.txt_objetivo)
        gridNumeros = findViewById(R.id.grid_numeros)

        // Obtenemos el intento (1 o 2)
        intentoActual = CortexManager.obtenerIntentoActual("t6")

        // Opcional: Mostrar el intento en la barra de título o agregar un TextView
        title = "ESCANEO VISUAL - INTENTO $intentoActual/2"

        iniciarPrueba()
    }

    private fun iniciarPrueba() {
        numeroObjetivo = Random.nextInt(10, 100)

        // Mostramos el objetivo. Si quieres, puedes concatenar el intento aquí también si no tienes otro lugar
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
            vista.setBackgroundColor(Color.parseColor("#10B981")) // Verde
            vista.setTextColor(Color.BLACK)
            calcularPuntajeYFinalizar()
        } else {
            clicsErroneos++
            vista.setBackgroundColor(Color.parseColor("#EF4444")) // Rojo
            vista.alpha = 0.5f // Desvanecer error
        }
    }

    private fun calcularPuntajeYFinalizar() {
        val duracion = System.currentTimeMillis() - tiempoInicio

        // 1. Penalización por Tiempo:
        // Base: 2000ms (2s) es aceptable. Por cada 50ms extra, baja 1 punto.
        val penalizacionTiempo = if (duracion > 2000) ((duracion - 2000) / 50).toInt() else 0

        // 2. Penalización por Errores:
        // Cada error resta 10 puntos (para evitar adivinanzas)
        val penalizacionErrores = clicsErroneos * 10

        val puntaje = (100 - penalizacionTiempo - penalizacionErrores).coerceIn(0, 100)

        // Registro de métricas
        val details = mapOf(
            "tiempo_total_ms" to duracion,
            "clics_erroneos" to clicsErroneos
        )
        CortexManager.logPerformanceMetric("t6", puntaje, details)
        CortexManager.guardarPuntaje("t6", puntaje)

        // --- LÓGICA DE EXONERACIÓN ---
        if (intentoActual == 1 && puntaje < 80) {
            // Reprobó intento 1 -> Manda repetir
            mostrarDialogoFin(puntaje, duracion, esReintento = true)
        } else {
            // Aprobó intento 1 O es intento 2 -> Siguiente
            mostrarDialogoFin(puntaje, duracion, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(puntaje: Int, tiempoMs: Long, esReintento: Boolean) {
        if (isFinishing) return

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "INTENTO FALLIDO ⚠️"
            mensaje = "Tiempo: ${tiempoMs}ms\nErrores: $clicsErroneos\nNota: $puntaje%\n\nNecesitas ser más rápido y preciso. Tienes un segundo intento."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (puntaje >= 80) "ESCANEO ÓPTIMO ✅" else "TEST FINALIZADO"
            mensaje = "Tiempo Final: ${tiempoMs}ms\nErrores: $clicsErroneos\nNota Final: $puntaje%"
            textoBoton = "SIGUIENTE TEST"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate() // Recarga la actividad para el segundo intento
                } else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }
}