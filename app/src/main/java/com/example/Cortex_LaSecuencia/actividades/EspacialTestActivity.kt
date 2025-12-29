package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class EspacialTestActivity : AppCompatActivity() {

    private lateinit var txtIntento: TextView
    private lateinit var txtInstruccion: TextView
    private lateinit var txtFlecha: TextView
    private lateinit var btnArriba: Button
    private lateinit var btnAbajo: Button
    private lateinit var btnIzquierda: Button
    private lateinit var btnDerecha: Button

    private var trials = 0
    private var score = 0
    private val MAX_TRIALS = 5
    private var correctDir = 0 // 0=arriba, 90=derecha, 180=abajo, 270=izquierda
    private var isBlue = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espacial_test)

        txtIntento = findViewById(R.id.lbl_t9)
        txtInstruccion = findViewById(R.id.t9_instruccion)
        txtFlecha = findViewById(R.id.t9_arrow)
        btnArriba = findViewById(R.id.btn_dir_up)
        btnAbajo = findViewById(R.id.btn_dir_down)
        btnIzquierda = findViewById(R.id.btn_dir_left)
        btnDerecha = findViewById(R.id.btn_dir_right)

        btnArriba.setOnClickListener { clickDirection(0) }
        btnDerecha.setOnClickListener { clickDirection(90) }
        btnAbajo.setOnClickListener { clickDirection(180) }
        btnIzquierda.setOnClickListener { clickDirection(270) }

        siguienteTrial()
    }

    private fun siguienteTrial() {
        if (trials >= MAX_TRIALS) {
            finalizarTest()
            return
        }

        val dirs = listOf(0, 90, 180, 270)
        val arrowDir = dirs[Random.nextInt(dirs.size)]
        isBlue = Random.nextBoolean()

        // Si es azul: dirección real, si es roja: dirección contraria
        correctDir = if (isBlue) {
            arrowDir
        } else {
            (arrowDir + 180) % 360
        }

        // Configurar flecha visual
        txtFlecha.text = when (arrowDir) {
            0 -> "↑"
            90 -> "→"
            180 -> "↓"
            270 -> "←"
            else -> "↑"
        }

        txtFlecha.setTextColor(if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#EF4444"))
        txtFlecha.rotation = 0f // La flecha ya está en la dirección correcta por el emoji

        trials++
    }

    private fun clickDirection(dir: Int) {
        if (dir == correctDir) {
            score++
        }
        siguienteTrial()
    }

    private fun finalizarTest() {
        val finalScore = (score * 100 / MAX_TRIALS).coerceIn(0, 100)
        CortexManager.guardarPuntaje("t9", finalScore)
        CortexManager.navegarAlSiguiente(this)
        finish()
    }
}

