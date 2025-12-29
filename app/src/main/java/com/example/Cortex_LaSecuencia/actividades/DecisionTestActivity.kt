package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
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
    private var rule = "MAYOR" // "MAYOR" o "MENOR"
    private var isBlue = true
    private var num1 = 0
    private var num2 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decision_test)

        txtIntento = findViewById(R.id.lbl_t10)
        txtRegla = findViewById(R.id.t10_rule)
        card1 = findViewById(R.id.t10_card1)
        card2 = findViewById(R.id.t10_card2)
        txtCard1 = findViewById(R.id.txt_card1)
        txtCard2 = findViewById(R.id.txt_card2)

        card1.setOnClickListener { clickCard(0) }
        card2.setOnClickListener { clickCard(1) }

        siguienteTrial()
    }

    private fun siguienteTrial() {
        if (trials >= MAX_TRIALS) {
            finalizarTest()
            return
        }

        // Decidir regla (AZUL=MAYOR, NARANJA=MENOR)
        isBlue = Random.nextBoolean()
        rule = if (isBlue) "MAYOR" else "MENOR"

        // Actualizar UI de regla
        txtRegla.text = if (isBlue) "AZUL = MAYOR (+)" else "NARANJA = MENOR (-)"
        txtRegla.setTextColor(if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#F59E0B"))

        // Generar números diferentes
        num1 = Random.nextInt(100)
        num2 = Random.nextInt(100)
        while (num1 == num2) {
            num2 = Random.nextInt(100)
        }

        txtCard1.text = num1.toString()
        txtCard2.text = num2.toString()

        // Color de las cartas según regla
        val cardColor = if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#F59E0B")
        card1.setCardBackgroundColor(cardColor)
        card2.setCardBackgroundColor(cardColor)

        trials++
    }

    private fun clickCard(index: Int) {
        val chosen = if (index == 0) num1 else num2
        val correct = when (rule) {
            "MAYOR" -> chosen == maxOf(num1, num2)
            "MENOR" -> chosen == minOf(num1, num2)
            else -> false
        }

        if (correct) {
            score++
        }

        siguienteTrial()
    }

    private fun finalizarTest() {
        val finalScore = (score * 100 / MAX_TRIALS).coerceIn(0, 100)
        CortexManager.guardarPuntaje("t10", finalScore)
        CortexManager.navegarAlSiguiente(this)
        finish()
    }
}

