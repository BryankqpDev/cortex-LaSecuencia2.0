package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class RastreoTestActivity : AppCompatActivity() {

    private lateinit var areaRastreo: FrameLayout
    private lateinit var txtMensaje: TextView
    private lateinit var btnConfirmar: Button
    private lateinit var txtIntento: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val bolas = mutableListOf<BallView>()
    private val indicesObjetivo = mutableListOf<Int>()
    private val indicesSeleccionados = mutableListOf<Int>()

    private var animacionActiva = false
    private var testFinalizado = false
    private var animacionRunnable: Runnable? = null
    private var tiempoInicioSeleccion: Long = 0
    private var intentoActual = 1 // Control de intento

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rastreo_test)

        areaRastreo = findViewById(R.id.area_rastreo)
        txtMensaje = findViewById(R.id.txt_mensaje)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        txtIntento = findViewById(R.id.txt_intento)

        // Inicializamos intento
        intentoActual = CortexManager.obtenerIntentoActual("t8")
        txtIntento.text = "INTENTO $intentoActual/2"

        btnConfirmar.setOnClickListener { if (!testFinalizado) verificarSeleccion() }

        iniciarTest()
    }

    private fun iniciarTest() {
        txtMensaje.text = "Memoriza las 2 AZULES..."
        btnConfirmar.visibility = View.GONE
        indicesSeleccionados.clear()
        indicesObjetivo.clear()
        bolas.clear()
        areaRastreo.removeAllViews()

        areaRastreo.post {
            val ancho = areaRastreo.width.toFloat()
            val alto = areaRastreo.height.toFloat()

            for (i in 0 until 5) {
                val bola = BallView(this, i)
                bola.posX = Random.nextFloat() * 0.7f + 0.15f
                bola.posY = Random.nextFloat() * 0.7f + 0.15f
                val velocidad = Random.nextFloat() * 0.015f + 0.025f
                val angulo = Random.nextFloat() * 2f * Math.PI.toFloat()
                bola.vx = kotlin.math.cos(angulo) * velocidad
                bola.vy = kotlin.math.sin(angulo) * velocidad

                // Tamaño fijo para que sean redondas
                val tamaño = (40 * resources.displayMetrics.density).toInt()
                bola.layoutParams = FrameLayout.LayoutParams(tamaño, tamaño)

                bolas.add(bola)
                areaRastreo.addView(bola)
                bola.actualizarPosicion(ancho, alto)
            }

            // Seleccionamos objetivos (0 y 1)
            indicesObjetivo.addAll(listOf(0, 1))
            bolas[0].cambiarColor(Color.parseColor("#3B82F6")) // Azul
            bolas[1].cambiarColor(Color.parseColor("#3B82F6")) // Azul

            // 2 segundos para memorizar
            handler.postDelayed({ if (!isFinishing) iniciarAnimacion() }, 2000)
        }
    }

    private fun iniciarAnimacion() {
        // Todas se vuelven blancas para confundir
        bolas.forEach { it.cambiarColor(Color.WHITE) }
        txtMensaje.text = "Rastreando..."
        animacionActiva = true

        var pasos = 0
        val maxPasos = 300 // Duración de la animación

        animacionRunnable = object : Runnable {
            override fun run() {
                if (!animacionActiva || testFinalizado) return
                val ancho = areaRastreo.width.toFloat()
                val alto = areaRastreo.height.toFloat()

                bolas.forEach { bola ->
                    bola.posX += bola.vx
                    bola.posY += bola.vy

                    // Rebote en paredes
                    if (bola.posX <= 0.05f || bola.posX >= 0.95f) bola.vx *= -1
                    if (bola.posY <= 0.05f || bola.posY >= 0.95f) bola.vy *= -1

                    bola.actualizarPosicion(ancho, alto)
                }

                pasos++
                if (pasos >= maxPasos) {
                    animacionActiva = false
                    txtMensaje.text = "Selecciona los 2 objetivos:"
                    habilitarSeleccion()
                } else {
                    handler.postDelayed(this, 16) // ~60 FPS
                }
            }
        }
        handler.post(animacionRunnable!!)
    }

    private fun habilitarSeleccion() {
        tiempoInicioSeleccion = System.currentTimeMillis()
        btnConfirmar.visibility = View.GONE

        bolas.forEachIndexed { indice, bola ->
            bola.setOnClickListener {
                if (testFinalizado || indicesSeleccionados.contains(indice)) return@setOnClickListener

                // Si ya seleccionó 2, no deja seleccionar más a menos que deseleccione (simplificado: max 2)
                if (indicesSeleccionados.size >= 2) return@setOnClickListener

                indicesSeleccionados.add(indice)
                bola.cambiarColor(Color.parseColor("#F59E0B")) // Naranja (Seleccionado)

                btnConfirmar.text = "CONFIRMAR (${indicesSeleccionados.size})"

                if (indicesSeleccionados.size == 2) {
                    btnConfirmar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun verificarSeleccion() {
        testFinalizado = true
        animacionActiva = false
        animacionRunnable?.let { handler.removeCallbacks(it) }

        val tiempoDecision = System.currentTimeMillis() - tiempoInicioSeleccion

        // Revelamos los verdaderos objetivos en Verde
        indicesObjetivo.forEach { bolas[it].cambiarColor(Color.parseColor("#10B981")) }

        var aciertos = 0
        indicesSeleccionados.forEach {
            if (indicesObjetivo.contains(it)) {
                aciertos++
            } else {
                // Si seleccionó uno incorrecto, lo marcamos en Rojo
                bolas[it].cambiarColor(Color.parseColor("#EF4444"))
            }
        }

        // Puntuación estricta: 2 aciertos = 100, 1 acierto = 50, 0 = 0
        val puntaje = when (aciertos) { 2 -> 100; 1 -> 50; else -> 0 }

        // Guardamos métricas
        val details = mapOf(
            "tiempo_decision_ms" to tiempoDecision,
            "aciertos" to aciertos,
            "objetivos" to indicesObjetivo,
            "seleccionados" to indicesSeleccionados
        )
        CortexManager.logPerformanceMetric("t8", puntaje, details)
        CortexManager.guardarPuntaje("t8", puntaje)

        // Delay pequeño para que el usuario vea el resultado visual antes del dialog
        handler.postDelayed({
            manejarContinuacion(puntaje, aciertos)
        }, 1500)
    }

    private fun manejarContinuacion(puntaje: Int, aciertos: Int) {
        if (isFinishing) return

        val esReintento: Boolean = (intentoActual == 1 && puntaje < 80) // Solo pasa con 100 (2 aciertos)

        val titulo: String
        val mensajeBase = when (aciertos) {
            2 -> "¡EXCELENTE! 🎯\nIdentificaste los 2 objetivos."
            1 -> "REGULAR ⚠️\nIdentificaste solo 1 objetivo."
            else -> "FALLASTE ❌\nPerdiste el rastro por completo."
        }
        val textoBoton: String

        if (esReintento) {
            titulo = "RASTREO INCOMPLETO"
            // Mensaje que explica que debe repetir
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (puntaje == 100) "RASTREO PERFECTO ✅" else "TEST FINALIZADO"
            textoBoton = "SIGUIENTE TEST" // Si es el último test, esto llevará al Resumen
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage("$mensajeBase\nNota: $puntaje%")
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate()
                } else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        animacionActiva = false
        animacionRunnable?.let { handler.removeCallbacks(it) }
    }

    // Clase BallView Mejorada (Círculos perfectos con ShapeDrawable)
    private class BallView(context: android.content.Context, val indice: Int) : View(context) {
        var posX: Float = 0f; var posY: Float = 0f; var vx: Float = 0f; var vy: Float = 0f

        private val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke(2, Color.BLACK) // Borde negro para verlas mejor
        }

        init {
            background = drawable
        }

        fun cambiarColor(color: Int) {
            drawable.setColor(color)
            invalidate()
        }

        fun actualizarPosicion(ancho: Float, alto: Float) {
            val params = layoutParams as FrameLayout.LayoutParams
            // Aseguramos que no se salga de los márgenes
            val left = (posX * ancho - width / 2).toInt().coerceIn(0, (ancho - width).toInt())
            val top = (posY * alto - height / 2).toInt().coerceIn(0, (alto - height).toInt())

            params.leftMargin = left
            params.topMargin = top
            layoutParams = params
        }
    }
}