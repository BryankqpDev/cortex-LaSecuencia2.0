package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import java.util.Random

class ImpulsoTestActivity : AppCompatActivity() {

    private lateinit var cardEstimulo: CardView
    private lateinit var txtIcono: TextView
    private lateinit var txtFeedback: TextView
    private lateinit var layoutRoot: View

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()

    private var rondaActual = 0
    private val TOTAL_RONDAS = 12

    private var erroresImpulso = 0 // Tocar naranja
    private var erroresOmision = 0 // No tocar azul
    private val tiemposDeReaccionGo = mutableListOf<Long>() // ✅ NUEVO: Lista para tiempos

    private var esAzul = false
    private var esperandoRespuesta = false
    private var respondioEnEstaRonda = false
    private var tiempoInicioEstimulo: Long = 0 // ✅ NUEVO: Para medir reacción

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impulso_test)

        cardEstimulo = findViewById(R.id.card_estimulo)
        txtIcono = findViewById(R.id.txt_icono_central)
        txtFeedback = findViewById(R.id.txt_feedback)
        layoutRoot = findViewById(R.id.layout_root)

        layoutRoot.setOnClickListener { procesarToque() }
        cardEstimulo.setOnClickListener { procesarToque() }

        handler.postDelayed({ siguienteEstimulo() }, 1000)
    }

    private fun siguienteEstimulo() {
        if (rondaActual >= TOTAL_RONDAS) {
            finalizarPrueba()
            return
        }

        rondaActual++
        esperandoRespuesta = true
        respondioEnEstaRonda = false
        esAzul = random.nextFloat() > 0.3
        
        if (esAzul) {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#3B82F6"))
            txtIcono.text = "🔵"
        } else {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#F59E0B"))
            txtIcono.text = "✖️"
        }
        
        tiempoInicioEstimulo = System.currentTimeMillis() // ✅ INICIA CRONÓMETRO
        txtFeedback.text = "..."

        handler.postDelayed({ if (!isFinishing) evaluarFinDeRonda() }, 900)
    }

    private fun procesarToque() {
        if (!esperandoRespuesta || respondioEnEstaRonda) return

        respondioEnEstaRonda = true

        if (esAzul) {
            // CORRECTO (GO): Tocó azul
            val tiempoReaccion = System.currentTimeMillis() - tiempoInicioEstimulo
            tiemposDeReaccionGo.add(tiempoReaccion) // ✅ GUARDA TIEMPO
            txtFeedback.text = "¡BIEN! (${tiempoReaccion}ms)"
            txtFeedback.setTextColor(Color.GREEN)
        } else {
            // ERROR (NO-GO): Tocó naranja
            erroresImpulso++
            txtFeedback.text = "¡ERROR! NO TOQUES"
            txtFeedback.setTextColor(Color.RED)
        }
    }

    private fun evaluarFinDeRonda() {
        if (!respondioEnEstaRonda && esAzul) {
            erroresOmision++
            txtFeedback.text = "¡TARDAS!"
            txtFeedback.setTextColor(Color.LTGRAY)
        }

        cardEstimulo.setCardBackgroundColor(Color.parseColor("#1E293B"))
        txtIcono.text = ""
        esperandoRespuesta = false

        handler.postDelayed({ siguienteEstimulo() }, 200)
    }

    private fun finalizarPrueba() {
        if (isFinishing) return

        val penalizacion = (erroresImpulso * 20) + (erroresOmision * 10)
        val notaFinal = (100 - penalizacion).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t7", notaFinal)

        // --- ✅ REGISTRO DE MÉTRICAS DETALLADO ---
        val details = mapOf(
            "tiempos_reaccion_ms" to tiemposDeReaccionGo,
            "errores_impulso" to erroresImpulso,
            "errores_omision" to erroresOmision
        )
        CortexManager.logPerformanceMetric("t7", notaFinal, details)

        val mensaje = "Impulsos fallidos: $erroresImpulso\nOmisiones: $erroresOmision"

        AlertDialog.Builder(this)
            .setTitle("CONTROL DE IMPULSO")
            .setMessage("Nota: $notaFinal%\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}