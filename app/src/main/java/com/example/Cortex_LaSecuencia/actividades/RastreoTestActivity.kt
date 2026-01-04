package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
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
    private var tiempoInicioSeleccion: Long = 0 // ✅ NUEVO: Para medir tiempo de decisión

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rastreo_test)

        areaRastreo = findViewById(R.id.area_rastreo)
        txtMensaje = findViewById(R.id.txt_mensaje)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        txtIntento = findViewById(R.id.txt_intento)

        val intentoActual = CortexManager.obtenerIntentoActual("t8")
        txtIntento.text = "INTENTO $intentoActual/2"

        btnConfirmar.setOnClickListener { if (!testFinalizado) verificarSeleccion() }

        iniciarTest()
    }

    private fun iniciarTest() {
        txtMensaje.text = "Memorice las 2 AZULES..."
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
                val tamaño = (40 * resources.displayMetrics.density).toInt()
                bola.layoutParams = FrameLayout.LayoutParams(tamaño, tamaño)
                bolas.add(bola)
                areaRastreo.addView(bola)
                bola.actualizarPosicion(ancho, alto)
            }

            indicesObjetivo.addAll(listOf(0, 1))
            bolas[0].cambiarColor(Color.parseColor("#3B82F6"))
            bolas[1].cambiarColor(Color.parseColor("#3B82F6"))

            handler.postDelayed({ if (!isFinishing) iniciarAnimacion() }, 2000)
        }
    }

    private fun iniciarAnimacion() {
        bolas.forEach { it.cambiarColor(Color.WHITE) }
        txtMensaje.text = "Rastreando..."
        animacionActiva = true

        var pasos = 0
        val maxPasos = 300

        animacionRunnable = object : Runnable {
            override fun run() {
                if (!animacionActiva || testFinalizado) return
                val ancho = areaRastreo.width.toFloat()
                val alto = areaRastreo.height.toFloat()

                bolas.forEach { bola ->
                    bola.posX += bola.vx
                    bola.posY += bola.vy
                    if (bola.posX <= 0.05f || bola.posX >= 0.95f) bola.vx *= -1
                    if (bola.posY <= 0.05f || bola.posY >= 0.95f) bola.vy *= -1
                    bola.actualizarPosicion(ancho, alto)
                }

                pasos++
                if (pasos >= maxPasos) {
                    animacionActiva = false
                    txtMensaje.text = "Toque los 2 objetivos:"
                    habilitarSeleccion()
                } else {
                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(animacionRunnable!!)
    }

    private fun habilitarSeleccion() {
        tiempoInicioSeleccion = System.currentTimeMillis() // ✅ INICIA CRONÓMETRO DE DECISIÓN
        btnConfirmar.visibility = View.GONE

        bolas.forEachIndexed { indice, bola ->
            bola.setOnClickListener {
                if (testFinalizado || indicesSeleccionados.size >= 2 || indicesSeleccionados.contains(indice)) return@setOnClickListener

                indicesSeleccionados.add(indice)
                bola.cambiarColor(Color.parseColor("#F59E0B"))
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

        val tiempoDecision = System.currentTimeMillis() - tiempoInicioSeleccion // ✅ CALCULA TIEMPO

        indicesObjetivo.forEach { bolas[it].cambiarColor(Color.parseColor("#10B981")) }

        var aciertos = 0
        indicesSeleccionados.forEach {
            if (indicesObjetivo.contains(it)) aciertos++
            else bolas[it].cambiarColor(Color.parseColor("#EF4444"))
        }

        val puntaje = when (aciertos) { 2 -> 100; 1 -> 50; else -> 0 }
        val mensaje = when (aciertos) { 2 -> "Perfecto! +100"; 1 -> "1 correcta +50"; else -> "Fallaste +0" }
        txtMensaje.text = mensaje

        // --- ✅ REGISTRO DE MÉTRICAS DETALLADO ---
        val details = mapOf(
            "tiempo_decision_ms" to tiempoDecision,
            "aciertos" to aciertos,
            "objetivos" to indicesObjetivo,
            "seleccionados" to indicesSeleccionados
        )
        CortexManager.logPerformanceMetric("t8", puntaje, details)

        handler.postDelayed({ mostrarResultado(puntaje, aciertos) }, 2000)
    }

    private fun mostrarResultado(puntaje: Int, aciertos: Int) {
        val mensaje = when (aciertos) {
            2 -> "EXCELENTE!\n\nIdentificaste correctamente los 2 objetivos.\n\nNota: $puntaje%"
            1 -> "REGULAR\n\nIdentificaste solo 1 objetivo de los 2.\n\nNota: $puntaje%"
            else -> "FALLASTE\n\nNo identificaste ninguno de los objetivos.\n\nNota: $puntaje%"
        }

        AlertDialog.Builder(this)
            .setTitle("RASTREO (MOT)")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ -> manejarContinuacion(puntaje) }
            .show()
    }

    private fun manejarContinuacion(puntaje: Int) {
        CortexManager.guardarPuntaje("t8", puntaje)
        if (CortexManager.obtenerIntentoActual("t8") == 1 && puntaje < 80) {
            recreate()
        } else {
            CortexManager.navegarAlSiguiente(this)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animacionActiva = false
        animacionRunnable?.let { handler.removeCallbacks(it) }
    }
    
    // Clase BallView (sin cambios)
    private class BallView(context: android.content.Context, val indice: Int) : View(context) {
        var posX: Float = 0f; var posY: Float = 0f; var vx: Float = 0f; var vy: Float = 0f
        init { 
            setBackgroundColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
        }
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) { outline.setOval(0, 0, view.width, view.height) }
            }
        }
        fun cambiarColor(color: Int) { setBackgroundColor(color) }
        fun actualizarPosicion(ancho: Float, alto: Float) {
            val params = layoutParams as FrameLayout.LayoutParams
            params.leftMargin = (posX * ancho - width / 2).toInt()
            params.topMargin = (posY * alto - height / 2).toInt()
            layoutParams = params
        }
    }
}