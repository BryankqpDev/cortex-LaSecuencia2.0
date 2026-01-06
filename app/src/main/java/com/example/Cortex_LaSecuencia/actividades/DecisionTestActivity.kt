package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class DecisionTestActivity : AppCompatActivity() {

    private lateinit var txtIntento: TextView
    private lateinit var txtRegla: TextView
    private lateinit var card1: CardView
    private lateinit var card2: CardView
    private lateinit var txtCard1: TextView
    private lateinit var txtCard2: TextView

    private var trials = 0
    private var score = 0
    private val MAX_TRIALS = 8

    // Lógica del juego
    private var rule = "MAYOR" // "MAYOR" o "MENOR"
    private var isBlue = true
    private var num1 = 0
    private var num2 = 0

    // Control de flujo
    private var intentoActual = 1
    private var clicksBloqueados = false // Evita doble toque
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decision_test)

        txtIntento = findViewById(R.id.lbl_t10)
        txtRegla = findViewById(R.id.t10_rule)
        card1 = findViewById(R.id.t10_card1)
        card2 = findViewById(R.id.t10_card2)
        txtCard1 = findViewById(R.id.txt_card1)
        txtCard2 = findViewById(R.id.txt_card2)

        // 1. Obtener y mostrar intento
        intentoActual = CortexManager.obtenerIntentoActual("t10")
        txtIntento.text = "INTENTO $intentoActual/2"

        // Pasamos la carta completa al click listener para cambiarle el color
        card1.setOnClickListener { clickCard(0, card1) }
        card2.setOnClickListener { clickCard(1, card2) }

        siguienteTrial()
    }

    private fun siguienteTrial() {
        if (trials >= MAX_TRIALS) {
            finalizarTest()
            return
        }

        clicksBloqueados = false

        // Decidir regla (AZUL=MAYOR, NARANJA=MENOR)
        isBlue = Random.nextBoolean()
        rule = if (isBlue) "MAYOR" else "MENOR"

        // Actualizar UI de regla
        txtRegla.text = if (isBlue) "AZUL = TOCA EL MAYOR" else "NARANJA = TOCA EL MENOR"
        txtRegla.setTextColor(if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#F59E0B"))

        // Generar números diferentes
        num1 = Random.nextInt(1, 100)
        num2 = Random.nextInt(1, 100)
        while (num1 == num2) {
            num2 = Random.nextInt(1, 100)
        }

        txtCard1.text = num1.toString()
        txtCard2.text = num2.toString()

        // Restaurar color base de las cartas según la regla actual
        val cardColor = if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#F59E0B")
        card1.setCardBackgroundColor(cardColor)
        card2.setCardBackgroundColor(cardColor)
    }

    private fun clickCard(index: Int, cardView: CardView) {
        if (clicksBloqueados) return
        clicksBloqueados = true // Bloquear hasta siguiente ronda

        val chosen = if (index == 0) num1 else num2

        // Lógica de validación
        val isCorrect = when (rule) {
            "MAYOR" -> chosen == maxOf(num1, num2)
            "MENOR" -> chosen == minOf(num1, num2)
            else -> false
        }

        if (isCorrect) {
            score++
            // Feedback Visual: Verde
            cardView.setCardBackgroundColor(Color.parseColor("#10B981"))
        } else {
            // Feedback Visual: Rojo
            cardView.setCardBackgroundColor(Color.parseColor("#EF4444"))
        }

        trials++

        // Pequeña pausa de 400ms para que el usuario vea si acertó
        handler.postDelayed({
            siguienteTrial()
        }, 400)
    }

    private fun finalizarTest() {
        // Cálculo: (Aciertos / 8) * 100
        // 7 de 8 es 87.5 (Pasa). 6 de 8 es 75 (Repite).
        val finalScore = ((score.toDouble() / MAX_TRIALS) * 100).toInt()

        // Guardar métricas
        val details = mapOf("aciertos" to score, "total_trials" to MAX_TRIALS)
        CortexManager.logPerformanceMetric("t10", finalScore, details)
        CortexManager.guardarPuntaje("t10", finalScore)

        // --- LÓGICA DE EXONERACIÓN ---
        if (intentoActual == 1 && finalScore < 80) {
            mostrarDialogoFin(finalScore, esReintento = true)
        } else {
            mostrarDialogoFin(finalScore, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(puntaje: Int, esReintento: Boolean) {
        if (isFinishing) return

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "DECISIÓN INCORRECTA ⚠️"
            mensaje = "Acertaste $score de $MAX_TRIALS.\nNota: $puntaje%\n\nDebes prestar más atención a la regla de color (Azul=Mayor, Naranja=Menor). Intento 2."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (puntaje >= 80) "RAZONAMIENTO FLUIDO 🧠" else "TEST FINALIZADO"
            mensaje = "Resultado: $score de $MAX_TRIALS decisiones correctas.\nNota Final: $puntaje%"
            textoBoton = "FINALIZAR EVALUACIÓN" // Al ser el último test (T10), solemos ir al reporte
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate()
                } else {
                    // Aquí llamamos a navegarAlSiguiente.
                    // Si CortexManager sabe que es el último, debería llevar a la pantalla de Resultados Finales.
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }
}