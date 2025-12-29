package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private val targetIndices = mutableListOf<Int>()
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

        trackArea.post {
            val width = trackArea.width.toFloat()
            val height = trackArea.height.toFloat()
            val ballRadius = (20 * resources.displayMetrics.density) // Radio de la bola

            // Crear 5 bolas con posiciones y velocidades válidas
            for (i in 0 until 5) {
                val ball = BallView(this, i)

                // Posición inicial normalizada (0.15 a 0.85 para evitar bordes)
                ball.x = Random.nextFloat() * 0.7f + 0.15f
                ball.y = Random.nextFloat() * 0.7f + 0.15f

                // Velocidad normalizada (2-4% de la pantalla por frame)
                val speed = Random.nextFloat() * 0.02f + 0.02f
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                ball.vx = kotlin.math.cos(angle) * speed
                ball.vy = kotlin.math.sin(angle) * speed

                val size = (40 * resources.displayMetrics.density).toInt()
                val params = FrameLayout.LayoutParams(size, size)
                ball.layoutParams = params

                balls.add(ball)
                trackArea.addView(ball)

                // Actualizar posición visual inicial
                ball.updatePosition(width, height)
            }

            // Las primeras 2 son azules (targets)
            targetIndices.add(0)
            targetIndices.add(1)
            balls[0].setColor(Color.parseColor("#3B82F6"))
            balls[1].setColor(Color.parseColor("#3B82F6"))

            // Esperar 2.5 segundos antes de empezar animación
            handler.postDelayed({
                balls.forEach { it.setColor(Color.WHITE) }
                txtMensaje.text = "Rastreando..."
                animationActive = true
                iniciarAnimacion()
            }, 2500)
        }
    }

    private fun iniciarAnimacion() {
        var steps = 0
        val maxSteps = 200 // ~3.3 segundos a 60 FPS

        animationRunnable = object : Runnable {
            override fun run() {
                if (!animationActive || testFinalizado) return

                val width = trackArea.width.toFloat()
                val height = trackArea.height.toFloat()

                // Mover todas las bolas
                balls.forEach { ball ->
                    // Actualizar posición
                    ball.x += ball.vx
                    ball.y += ball.vy

                    // Rebotes en bordes con margen
                    if (ball.x <= 0.05f) {
                        ball.x = 0.05f
                        ball.vx = kotlin.math.abs(ball.vx)
                    } else if (ball.x >= 0.95f) {
                        ball.x = 0.95f
                        ball.vx = -kotlin.math.abs(ball.vx)
                    }

                    if (ball.y <= 0.05f) {
                        ball.y = 0.05f
                        ball.vy = kotlin.math.abs(ball.vy)
                    } else if (ball.y >= 0.95f) {
                        ball.y = 0.95f
                        ball.vy = -kotlin.math.abs(ball.vy)
                    }

                    // Actualizar posición visual
                    ball.updatePosition(width, height)
                }

                steps++
                if (steps >= maxSteps) {
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
        btnConfirmar.visibility = View.GONE

        balls.forEachIndexed { index, ball ->
            ball.setOnClickListener {
                if (testFinalizado || selectedIndices.size >= 2) return@setOnClickListener
                if (selectedIndices.contains(index)) return@setOnClickListener

                selectedIndices.add(index)
                ball.setColor(Color.parseColor("#F59E0B")) // Naranja

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

        // Mostrar las bolas objetivo en verde
        targetIndices.forEach { targetIndex ->
            balls[targetIndex].setColor(Color.parseColor("#10B981")) // Verde
        }

        var hits = 0
        selectedIndices.forEach { selected ->
            if (targetIndices.contains(selected)) {
                hits++
            } else {
                // Marcar selecciones incorrectas en rojo
                balls[selected].setColor(Color.parseColor("#EF4444")) // Rojo
            }
        }

        val score = when (hits) {
            2 -> 100
            1 -> 50
            else -> 0
        }

        val mensaje = when (hits) {
            2 -> "¡Perfecto! +100"
            1 -> "1 correcta +50"
            else -> "Fallaste +0"
        }
        txtMensaje.text = mensaje

        // Esperar 2 segundos antes de continuar
        handler.postDelayed({
            CortexManager.guardarPuntaje("t8", score)
            CortexManager.navegarAlSiguiente(this)
            finish()
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        animationActive = false
        animationRunnable?.let { handler.removeCallbacks(it) }
    }

    // Clase interna para las bolas
    private class BallView(context: android.content.Context, val ballIndex: Int) : View(context) {
        var x: Float = 0f  // Posición normalizada 0-1
        var y: Float = 0f
        var vx: Float = 0f // Velocidad normalizada
        var vy: Float = 0f

        init {
            setBackgroundColor(Color.WHITE)
            // Hacer las bolas circulares
            val size = (40 * context.resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // Hacer la vista circular
            val radius = minOf(w, h) / 2f
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        fun setColor(color: Int) {
            setBackgroundColor(color)
        }

        fun updatePosition(containerWidth: Float, containerHeight: Float) {
            val params = layoutParams as FrameLayout.LayoutParams
            // Centrar la bola en la posición normalizada
            params.leftMargin = (x * containerWidth - width / 2).toInt()
            params.topMargin = (y * containerHeight - height / 2).toInt()
            layoutParams = params
        }
    }
}