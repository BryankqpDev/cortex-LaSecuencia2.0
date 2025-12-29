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
    private val TOTAL_RONDAS = 12 // Un poco más largo para medir consistencia

    private var aciertos = 0
    private var erroresImpulso = 0 // Tocar naranja
    private var erroresOmision = 0 // No tocar azul

    private var esAzul = false
    private var esperandoRespuesta = false
    private var respondioEnEstaRonda = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impulso_test)

        cardEstimulo = findViewById(R.id.card_estimulo)
        txtIcono = findViewById(R.id.txt_icono_central)
        txtFeedback = findViewById(R.id.txt_feedback)
        layoutRoot = findViewById(R.id.layout_root)

        // Configurar clic en TODA la pantalla o en el círculo
        layoutRoot.setOnClickListener { procesarToque() }
        cardEstimulo.setOnClickListener { procesarToque() }

        // Iniciar tras 1 seg
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

        // 1. Decidir Color (Lógica HTML: Math.random() > 0.3 es GO)
        // Significa ~70% Azul, ~30% Naranja
        esAzul = random.nextFloat() > 0.3

        if (esAzul) {
            // AZUL = GO 🔵
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#3B82F6"))
            txtIcono.text = "🔵" // Gem
        } else {
            // NARANJA = NO GO ⛔
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#F59E0B"))
            txtIcono.text = "✖️" // Times
        }

        txtFeedback.text = "..."

        // 2. Tiempo límite (Ritmo rápido, ~900ms según HTML)
        handler.postDelayed({
            if (!isFinishing) {
                evaluarFinDeRonda()
            }
        }, 900)
    }

    private fun procesarToque() {
        if (!esperandoRespuesta || respondioEnEstaRonda) return

        respondioEnEstaRonda = true

        if (esAzul) {
            // CORRECTO: Tocó azul
            aciertos++
            txtFeedback.text = "¡BIEN!"
            txtFeedback.setTextColor(Color.GREEN)
        } else {
            // ERROR: Tocó naranja (Impulso)
            erroresImpulso++
            txtFeedback.text = "¡ERROR! NO TOQUES"
            txtFeedback.setTextColor(Color.RED)
        }
    }

    private fun evaluarFinDeRonda() {
        // Si se acabó el tiempo y era AZUL pero no tocó -> Omisión
        if (!respondioEnEstaRonda && esAzul) {
            erroresOmision++
            txtFeedback.text = "¡TARDAS!"
            txtFeedback.setTextColor(Color.LTGRAY)
        }

        // Limpiar visualmente (breve pausa en negro/gris)
        cardEstimulo.setCardBackgroundColor(Color.parseColor("#1E293B"))
        txtIcono.text = ""
        esperandoRespuesta = false

        // Siguiente ronda
        handler.postDelayed({
            siguienteEstimulo()
        }, 200) // Breve pausa entre estímulos
    }

    private fun finalizarPrueba() {
        if (isFinishing) return

        // Cálculo de nota
        // Penalizamos mucho los impulsos (tocar naranja)
        // HTML Logic: let s=100-(t7False*20)-((5-Math.min(5,t7Hits))*10);
        // Adaptación:
        var penalizacion = (erroresImpulso * 20) + (erroresOmision * 10)
        var notaFinal = 100 - penalizacion
        if (notaFinal < 0) notaFinal = 0

        CortexManager.guardarPuntaje("t7", notaFinal)

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