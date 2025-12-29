package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class RastreoTestActivity : AppCompatActivity() {

    private lateinit var trackArea: FrameLayout
    private lateinit var txtMensaje: TextView
    private lateinit var btnConfirmar: Button
    private lateinit var txtIntento: TextView

    private val balls = mutableListOf<BallView>()
    private val targetIndices = mutableListOf<Int>() // Índices de las 2 bolas azules
    private val selectedIndices = mutableListOf<Int>()
    private var animationActive = false
    private var testFinalizado = false

    private val handler = Handler(Looper.getMainLooper())
    private var animationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rastreo_test)

        trackArea = findViewById(R.id.track_area)
        txtMensaje = findViewById(R.id.t8_msg)
        btnConfirmar = findViewById(R.id.t8_btn)
        txtIntento = findViewById(R.id.lbl_t8)

        btnConfirmar.setOnClickListener {
            if (!testFinalizado) verificarSeleccion()
        }

        iniciarTest()
    }

    private fun iniciarTest() {
        txtMensaje.text = "Memorice los 2 azules..."
        btnConfirmar.visibility = View.GONE
        selectedIndices.clear()
        targetIndices.clear()
        balls.clear()
        trackArea.removeAllViews()

        // Esperar a que el layout esté medido
        trackArea.post {
            // Crear 5 bolas
            for (i in 0 until 5) {
                val ball = BallView(this, i)
                ball.x = Random.nextFloat() * 0.8f + 0.1f // 10% a 90%
                ball.y = Random.nextFloat() * 0.8f + 0.1f
                ball.vx = (Random.nextFloat() - 0.5f) * 0.035f // Velocidad normalizada
                ball.vy = (Random.nextFloat() - 0.5f) * 0.035f
                
                val size = (40 * resources.displayMetrics.density).toInt()
                val params = FrameLayout.LayoutParams(size, size)
                params.leftMargin = (ball.x * trackArea.width).toInt()
                params.topMargin = (ball.y * trackArea.height).toInt()
                ball.layoutParams = params
                
                balls.add(ball)
                trackArea.addView(ball)
            }

            // Las primeras 2 son azules (targets)
            targetIndices.add(0)
            targetIndices.add(1)
            balls[0].setColor(Color.parseColor("#3B82F6")) // Azul
            balls[1].setColor(Color.parseColor("#3B82F6")) // Azul

            // Esperar 2 segundos antes de empezar animación
            handler.postDelayed({
                // Volver todas blancas
                balls.forEach { it.setColor(Color.WHITE) }
                txtMensaje.text = "Rastreando..."
                animationActive = true
                iniciarAnimacion()
            }, 2000)
        }

    }

    private fun iniciarAnimacion() {
        var steps = 0
        animationRunnable = object : Runnable {
            override fun run() {
                if (!animationActive || testFinalizado) return

                // Mover todas las bolas
                balls.forEach { ball ->
                    ball.posx += ball.vx
                    ball.posy += ball.vy

                    // Rebotes en bordes
                    if (ball.x <= 0.05f || ball.x >= 0.95f) {
                        ball.vx *= -1f
                        ball.x = ball.x.coerceIn(0.05f, 0.95f)
                    }
                    if (ball.y <= 0.05f || ball.y >= 0.95f) {
                        ball.vy *= -1f
                        ball.y = ball.y.coerceIn(0.05f, 0.95f)
                    }

                    // Actualizar posición
                    val params = ball.layoutParams as FrameLayout.LayoutParams
                    params.leftMargin = (ball.x * trackArea.width).toInt()
                    params.topMargin = (ball.y * trackArea.height).toInt()
                    ball.layoutParams = params
                }

                steps++
                if (steps > 300) { // ~3 segundos de animación
                    animationActive = false
                    txtMensaje.text = "Toque los 2 objetivos:"
                    habilitarSeleccion()
                } else {
                    handler.postDelayed(this, 16) // ~60 FPS
                }
            }
        }
        handler.post(animationRunnable!!)
    }

    private fun habilitarSeleccion() {
        balls.forEachIndexed { index, ball ->
            ball.setOnClickListener {
                if (testFinalizado || selectedIndices.size >= 2) return@setOnClickListener
                
                if (selectedIndices.contains(index)) return@setOnClickListener

                selectedIndices.add(index)
                ball.setColor(Color.parseColor("#F59E0B")) // Naranja para seleccionado

                if (selectedIndices.size == 2) {
                    btnConfirmar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun verificarSeleccion() {
        testFinalizado = true
        animationActive = false
        animationRunnable?.let { handler.removeCallbacks(it) }

        var hits = 0
        selectedIndices.forEach { selected ->
            if (targetIndices.contains(selected)) hits++
        }

        val score = when {
            hits == 2 -> 100
            hits == 1 -> 50
            else -> 0
        }

        CortexManager.guardarPuntaje("t8", score)
        CortexManager.navegarAlSiguiente(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        animationActive = false
        animationRunnable?.let { handler.removeCallbacks(it) }
    }

    // Clase interna para las bolas
    private class BallView(context: android.content.Context, val ballIndex: Int) : View(context) {
        var posx: Float = 0f
        var posy: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f

        init {
            setBackgroundColor(Color.WHITE)
            val size = (40 * context.resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        fun setColor(color: Int) {
            setBackgroundColor(color)
        }
    }
}

