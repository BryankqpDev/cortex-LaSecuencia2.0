package com.example.Cortex_LaSecuencia.actividades

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import java.util.Random

class CoordinacionTestActivity : AppCompatActivity() {

    private lateinit var containerJuego: FrameLayout
    private lateinit var txtContador: TextView

    private var puntosAtrapados = 0
    private val META_PUNTOS = 5
    private var tiempoInicio: Long = 0
    private var erroresDistractor = 0 // ✅ NUEVO: Contador de errores

    private val random = Random()
    private var juegoActivo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)

        txtContador.text = "¡ATRAPA 5 AMARILLAS!"

        Handler(Looper.getMainLooper()).postDelayed({ iniciarJuego() }, 1000)
    }

    private fun iniciarJuego() {
        juegoActivo = true
        tiempoInicio = System.currentTimeMillis()
        generarEntidad()
    }

    private fun generarEntidad() {
        if (!juegoActivo || isFinishing) return

        val view = View(this)
        val esObjetivo = random.nextBoolean() // 50% de ser objetivo

        if (esObjetivo) {
            view.setBackgroundResource(R.drawable.circulo_amarillo)
            view.setOnClickListener {
                if (!juegoActivo) return@setOnClickListener
                puntosAtrapados++
                txtContador.text = "ATRAPADOS: $puntosAtrapados / $META_PUNTOS"
                containerJuego.removeView(view)

                if (puntosAtrapados >= META_PUNTOS) {
                    finalizarJuego()
                } else {
                    generarEntidad()
                }
            }
        } else {
            view.setBackgroundResource(R.drawable.circulo_rojo)
            view.setOnClickListener {
                if (!juegoActivo) return@setOnClickListener
                erroresDistractor++ // ✅ INCREMENTA CONTADOR
                containerJuego.removeView(view) // El distractor también desaparece
                generarEntidad() // Genera el siguiente
            }
        }

        val sizePx = (60 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(sizePx, sizePx)
        val maxX = containerJuego.width - sizePx
        val maxY = containerJuego.height - sizePx

        if (maxX <= 0 || maxY <= 0) {
            Handler(Looper.getMainLooper()).postDelayed({ generarEntidad() }, 100)
            return
        }
        params.leftMargin = random.nextInt(maxX)
        params.topMargin = random.nextInt(maxY)

        view.layoutParams = params
        containerJuego.addView(view)
    }

    private fun finalizarJuego() {
        if (!juegoActivo) return
        juegoActivo = false

        val tiempoTotal = System.currentTimeMillis() - tiempoInicio
        val tiempoBase = 3000
        val penalizacion = if (tiempoTotal > tiempoBase) ((tiempoTotal - tiempoBase) / 50).toInt() else 0
        val puntaje = (100 - penalizacion).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t4", puntaje)

        // --- ✅ REGISTRO DE MÉTRICAS DETALLADO ---
        val details = mapOf(
            "tiempo_total_ms" to tiempoTotal,
            "errores_distractor" to erroresDistractor
        )
        CortexManager.logPerformanceMetric("t4", puntaje, details)

        if (isFinishing) return

        val mensaje = "Tiempo total: ${tiempoTotal}ms\nErrores: $erroresDistractor\nNota: $puntaje%"

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
        juegoActivo = false
    }
}