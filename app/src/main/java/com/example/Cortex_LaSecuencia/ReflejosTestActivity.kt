package com.example.Cortex_LaSecuencia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random

class ReflejosTestActivity : AppCompatActivity() {

    private lateinit var targetCircle: View
    private lateinit var mainLayout: ConstraintLayout
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        mainLayout = findViewById(R.id.test_layout)
        targetCircle = findViewById(R.id.target_circle)

        // Al tocar el círculo, se oculta y aparece en otro lado
        targetCircle.setOnClickListener {
            val reactionTime = System.currentTimeMillis() - startTime
            Toast.makeText(this, "Reflejo: ${reactionTime}ms", Toast.LENGTH_SHORT).show()
            targetCircle.visibility = View.GONE
            esperarYAparecer()
        }

        esperarYAparecer()
    }

    private fun esperarYAparecer() {
        // Espera un tiempo aleatorio entre 1 y 3 segundos
        val delay = Random.nextLong(1000, 3000)

        Handler(Looper.getMainLooper()).postDelayed({
            moverCirculo()
        }, delay)
    }

    private fun moverCirculo() {
        // Calcular posiciones aleatorias dentro de la pantalla
        val width = mainLayout.width - targetCircle.width
        val height = mainLayout.height - targetCircle.height

        if (width > 0 && height > 0) {
            targetCircle.x = Random.nextInt(0, width).toFloat()
            targetCircle.y = Random.nextInt(0, height).toFloat()

            targetCircle.visibility = View.VISIBLE
            startTime = System.currentTimeMillis()
        }
    }
}