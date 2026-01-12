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
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class RastreoTestActivity : TestBaseActivity() {

    private lateinit var areaRastreo: FrameLayout
    private lateinit var txtMensaje: TextView
    private lateinit var btnConfirmar: Button
    private lateinit var txtIntento: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val bolas = mutableListOf<BallView>()
    private val indicesObjetivo = mutableListOf<Int>()
    private val indicesSeleccionados = mutableListOf<Int>()

    private var animacionActiva = false
    private var faseSeleccionHabilitada = false
    private var pasosRealizados = 0
    private val MAX_PASOS = 300
    private var tiempoInicioSeleccion: Long = 0

    override fun obtenerTestId(): String = "t8"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rastreo_test)

        areaRastreo = findViewById(R.id.area_rastreo)
        txtMensaje = findViewById(R.id.txt_mensaje)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        txtIntento = findViewById(R.id.txt_intento)

        val intentoActual = CortexManager.obtenerIntentoActual("t8")
        txtIntento.text = "INTENTO $intentoActual/2"

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        btnConfirmar.setOnClickListener {
            if (!testFinalizado && faseSeleccionHabilitada) {
                verificarSeleccion()
            }
        }

        iniciarTest()
    }

    private fun iniciarTest() {
        testFinalizado = false
        animacionActiva = false
        faseSeleccionHabilitada = false
        txtMensaje.text = "Memorice las 2 AZULES..."
        btnConfirmar.visibility = View.GONE
        indicesSeleccionados.clear()
        indicesObjetivo.clear()
        bolas.clear()
        areaRastreo.removeAllViews()
        pasosRealizados = 0

        areaRastreo.post {
            if (testFinalizado) return@post

            val ancho = areaRastreo.width.toFloat()
            val alto = areaRastreo.height.toFloat()

            if (ancho <= 0 || alto <= 0) {
                handler.postDelayed({ iniciarTest() }, 100)
                return@post
            }

            for (i in 0 until 5) {
                val bola = BallView(this, i)
                bola.posX = 0.15f + Random.nextFloat() * 0.7f
                bola.posY = 0.15f + Random.nextFloat() * 0.7f
                val velocidad = 0.015f + Random.nextFloat() * 0.015f
                val angulo = Random.nextFloat() * 2f * Math.PI.toFloat()
                bola.vx = kotlin.math.cos(angulo) * velocidad
                bola.vy = kotlin.math.sin(angulo) * velocidad
                bolas.add(bola)
                areaRastreo.addView(bola)
                bola.actualizarPosicion(ancho, alto)
            }

            indicesObjetivo.addAll(listOf(0, 1))
            bolas[0].cambiarColor(Color.parseColor("#3B82F6"))
            bolas[1].cambiarColor(Color.parseColor("#3B82F6"))

            handler.postDelayed({
                if (!testFinalizado && !estaEnPausaPorAusencia) {
                    iniciarAnimacion()
                }
            }, 2000)
        }
    }

    private val animacionRunnable = object : Runnable {
        override fun run() {
            if (!animacionActiva || testFinalizado || estaEnPausaPorAusencia) return

            val ancho = areaRastreo.width.toFloat()
            val alto = areaRastreo.height.toFloat()
            if (ancho <= 0 || alto <= 0) return

            bolas.forEach { bola ->
                bola.posX += bola.vx
                bola.posY += bola.vy
                if (bola.posX <= 0.05f) {
                    bola.posX = 0.05f
                    bola.vx = kotlin.math.abs(bola.vx)
                } else if (bola.posX >= 0.95f) {
                    bola.posX = 0.95f
                    bola.vx = -kotlin.math.abs(bola.vx)
                }
                if (bola.posY <= 0.05f) {
                    bola.posY = 0.05f
                    bola.vy = kotlin.math.abs(bola.vy)
                } else if (bola.posY >= 0.95f) {
                    bola.posY = 0.95f
                    bola.vy = -kotlin.math.abs(bola.vy)
                }
                bola.actualizarPosicion(ancho, alto)
            }

            pasosRealizados++
            if (pasosRealizados >= MAX_PASOS) {
                animacionActiva = false
                handler.post { habilitarSeleccion() }
            } else {
                handler.postDelayed(this, 16)
            }
        }
    }

    private fun iniciarAnimacion() {
        if (testFinalizado) return
        bolas.forEach { it.cambiarColor(Color.WHITE) }
        txtMensaje.text = "Rastreando..."
        animacionActiva = true
        handler.post(animacionRunnable)
    }

    private fun habilitarSeleccion() {
        if (testFinalizado) return
        faseSeleccionHabilitada = true
        tiempoInicioSeleccion = System.currentTimeMillis()
        btnConfirmar.visibility = View.GONE
        txtMensaje.text = "Toque los 2 objetivos:"

        bolas.forEachIndexed { indice, bola ->
            bola.setOnClickListener {
                if (!faseSeleccionHabilitada || testFinalizado || estaEnPausaPorAusencia) return@setOnClickListener
                if (indicesSeleccionados.size >= 2 || indicesSeleccionados.contains(indice)) return@setOnClickListener

                indicesSeleccionados.add(indice)
                bola.cambiarColor(Color.parseColor("#F59E0B"))
                btnConfirmar.text = "CONFIRMAR (${indicesSeleccionados.size}/2)"
                if (indicesSeleccionados.size == 2) btnConfirmar.visibility = View.VISIBLE
            }
        }
    }

    private fun verificarSeleccion() {
        if (testFinalizado) return
        testFinalizado = true
        faseSeleccionHabilitada = false
        handler.removeCallbacksAndMessages(null)

        val tiempoDecision = System.currentTimeMillis() - tiempoInicioSeleccion
        indicesObjetivo.forEach { bolas[it].cambiarColor(Color.parseColor("#10B981")) }

        var aciertos = 0
        indicesSeleccionados.forEach {
            if (indicesObjetivo.contains(it)) aciertos++
            else bolas[it].cambiarColor(Color.parseColor("#EF4444"))
        }

        val puntajeBase = when (aciertos) { 2 -> 100; 1 -> 50; else -> 0 }
        val puntajeFinal = (puntajeBase - penalizacionPorAusencia).coerceIn(0, 100)

        CortexManager.logPerformanceMetric("t8", puntajeFinal, mapOf("aciertos" to aciertos, "tiempo_ms" to tiempoDecision))
        CortexManager.guardarPuntaje("t8", puntajeFinal)

        handler.postDelayed({ if (!isFinishing) mostrarResultado(puntajeFinal, aciertos) }, 1500)
    }

    private fun mostrarResultado(puntaje: Int, aciertos: Int) {
        if (isFinishing) return
        val mensaje = when (aciertos) {
            2 -> "¡EXCELENTE!\nIdentificaste los 2 objetivos correctamente."
            1 -> "REGULAR\nSolo identificaste 1 de los 2 objetivos."
            else -> "FALLASTE\nNo identificaste ningún objetivo."
        }
        val mensajeCompleto = "$mensaje\n\nNota: $puntaje%" + (if (penalizacionPorAusencia > 0) "\nPenalización ausencia: -$penalizacionPorAusencia" else "")

        AlertDialog.Builder(this)
            .setTitle("RASTREO FINALIZADO")
            .setMessage(mensajeCompleto)
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                if (CortexManager.obtenerIntentoActual("t8") == 1 && puntaje < 80) recreate()
                else { CortexManager.navegarAlSiguiente(this); finish() }
            }
            .show()
    }

    override fun onTestPaused() {
        txtMensaje.text = "⏸ PAUSA POR AUSENCIA"
        handler.removeCallbacksAndMessages(null)
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            if (animacionActiva) {
                txtMensaje.text = "Rastreando..."
                handler.post(animacionRunnable)
            } else if (faseSeleccionHabilitada) {
                txtMensaje.text = "Toque los 2 objetivos:"
            } else if (pasosRealizados == 0) {
                handler.postDelayed({ if (!testFinalizado && !estaEnPausaPorAusencia) iniciarAnimacion() }, 500)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private class BallView(context: android.content.Context, val indice: Int) : View(context) {
        var posX: Float = 0f; var posY: Float = 0f; var vx: Float = 0f; var vy: Float = 0f
        init {
            setBackgroundColor(Color.WHITE)
            val size = (40 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
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
            val params = layoutParams as? FrameLayout.LayoutParams ?: return
            params.leftMargin = (posX * ancho - width / 2f).toInt().coerceIn(0, (ancho - width).toInt())
            params.topMargin = (posY * alto - height / 2f).toInt().coerceIn(0, (alto - height).toInt())
            layoutParams = params
        }
    }
}
