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

    private var erroresImpulso = 0 // Tocar naranja (Grave)
    private var erroresOmision = 0 // No tocar azul (Leve)
    private val tiemposDeReaccionGo = mutableListOf<Long>()

    private var esAzul = false
    private var esperandoRespuesta = false
    private var respondioEnEstaRonda = false
    private var tiempoInicioEstimulo: Long = 0

    private var intentoActual = 1 // Variable de control de intento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impulso_test)

        cardEstimulo = findViewById(R.id.card_estimulo)
        txtIcono = findViewById(R.id.txt_icono_central)
        txtFeedback = findViewById(R.id.txt_feedback)
        layoutRoot = findViewById(R.id.layout_root)

        // 1. Obtener intento actual
        intentoActual = CortexManager.obtenerIntentoActual("t7")

        // Opcional: Mostrar intento en título o feedback inicial
        title = "CONTROL DE IMPULSO - INTENTO $intentoActual/2"

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
        // 70% probabilidad de ser Azul (Go), 30% Naranja (No-Go)
        esAzul = random.nextFloat() > 0.3

        if (esAzul) {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#3B82F6")) // Azul
            txtIcono.text = "🔵"
        } else {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#F59E0B")) // Naranja
            txtIcono.text = "✖️"
        }

        tiempoInicioEstimulo = System.currentTimeMillis()
        txtFeedback.text = "..."

        // Ventana de respuesta de 900ms
        handler.postDelayed({ if (!isFinishing) evaluarFinDeRonda() }, 900)
    }

    private fun procesarToque() {
        if (!esperandoRespuesta || respondioEnEstaRonda) return

        respondioEnEstaRonda = true

        if (esAzul) {
            // CORRECTO (GO): Tocó azul
            val tiempoReaccion = System.currentTimeMillis() - tiempoInicioEstimulo
            tiemposDeReaccionGo.add(tiempoReaccion)
            txtFeedback.text = "¡BIEN! (${tiempoReaccion}ms)"
            txtFeedback.setTextColor(Color.GREEN)
        } else {
            // ERROR (NO-GO): Tocó naranja (Error de impulso)
            erroresImpulso++
            txtFeedback.text = "¡ERROR! NO TOQUES"
            txtFeedback.setTextColor(Color.RED)
        }
    }

    private fun evaluarFinDeRonda() {
        if (!respondioEnEstaRonda && esAzul) {
            // ERROR OMISIÓN: Era azul y no tocó
            erroresOmision++
            txtFeedback.text = "¡TARDAS!"
            txtFeedback.setTextColor(Color.LTGRAY)
        }

        // Reset visual
        cardEstimulo.setCardBackgroundColor(Color.parseColor("#1E293B"))
        txtIcono.text = ""
        esperandoRespuesta = false

        // Pausa entre estímulos (200ms)
        handler.postDelayed({ siguienteEstimulo() }, 200)
    }

    private fun finalizarPrueba() {
        if (isFinishing) return

        // CÁLCULO DE NOTA
        // Error Impulso (Tocar Naranja) penaliza 20 pts (Muy grave)
        // Error Omisión (No tocar Azul) penaliza 10 pts (Leve)
        val penalizacion = (erroresImpulso * 20) + (erroresOmision * 10)
        val notaFinal = (100 - penalizacion).coerceIn(0, 100)

        // MÉTRICAS
        val details = mapOf(
            "tiempos_reaccion_ms" to tiemposDeReaccionGo,
            "errores_impulso" to erroresImpulso,
            "errores_omision" to erroresOmision,
            "promedio_reaccion" to (if (tiemposDeReaccionGo.isNotEmpty()) tiemposDeReaccionGo.average() else 0)
        )
        CortexManager.logPerformanceMetric("t7", notaFinal, details)
        CortexManager.guardarPuntaje("t7", notaFinal)

        // --- LÓGICA DE EXONERACIÓN ---
        if (intentoActual == 1 && notaFinal < 80) {
            // Reprobó Intento 1 -> REPETIR
            mostrarDialogoFin(notaFinal, esReintento = true)
        } else {
            // Aprobó Intento 1 O es Intento 2 -> SIGUIENTE
            mostrarDialogoFin(notaFinal, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(nota: Int, esReintento: Boolean) {
        if (isFinishing) return

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "IMPULSO INESTABLE ⚠️"
            mensaje = "Errores Impulsivos: $erroresImpulso\nOmisiones: $erroresOmision\nNota: $nota%\n\nDebes controlarte mejor. Tienes un segundo intento."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (nota >= 80) "CONTROL EXCELENTE ✅" else "TEST FINALIZADO"
            mensaje = "Errores Impulsivos: $erroresImpulso\nOmisiones: $erroresOmision\nNota Final: $nota%"
            textoBoton = "SIGUIENTE TEST"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate() // Recarga la actividad
                } else {
                    CortexManager.navegarAlSiguiente(this) // Va al siguiente test (si hay) o finaliza flujo
                    finish()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiamos callbacks para evitar crashes al salir rápido
        handler.removeCallbacksAndMessages(null)
    }
}