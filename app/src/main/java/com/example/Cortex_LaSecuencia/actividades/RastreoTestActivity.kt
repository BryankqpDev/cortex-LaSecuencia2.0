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

    // === COMPONENTES DE LA UI ===
    private lateinit var areaRastreo: FrameLayout
    private lateinit var txtMensaje: TextView
    private lateinit var btnConfirmar: Button
    private lateinit var txtIntento: TextView

    // === VARIABLES DE CONTROL ===
    private val handler = Handler(Looper.getMainLooper())
    private val bolas = mutableListOf<BallView>()
    private val indicesObjetivo = mutableListOf<Int>()
    private val indicesSeleccionados = mutableListOf<Int>()

    private var animacionActiva = false
    private var testFinalizado = false
    private var animacionRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rastreo_test)

        // === INICIALIZAR COMPONENTES ===
        areaRastreo = findViewById(R.id.area_rastreo)
        txtMensaje = findViewById(R.id.txt_mensaje)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        txtIntento = findViewById(R.id.txt_intento)

        // === CONFIGURAR BADGE DE INTENTO ===
        val intentoActual = CortexManager.obtenerIntentoActual("t8")
        txtIntento.text = "INTENTO $intentoActual/2"

        // === CONFIGURAR BOTÓN ===
        btnConfirmar.setOnClickListener {
            if (!testFinalizado) verificarSeleccion()
        }

        // === INICIAR TEST ===
        iniciarTest()
    }

    /**
     * FUNCIÓN PRINCIPAL: Iniciar el test
     */
    private fun iniciarTest() {
        txtMensaje.text = "Memorice las 2 AZULES..."
        btnConfirmar.visibility = View.GONE
        indicesSeleccionados.clear()
        indicesObjetivo.clear()
        bolas.clear()
        areaRastreo.removeAllViews()

        // Esperar a que el área tenga dimensiones
        areaRastreo.post {
            val ancho = areaRastreo.width.toFloat()
            val alto = areaRastreo.height.toFloat()
            val radioBola = (20 * resources.displayMetrics.density)

            // === CREAR 5 BOLAS ===
            for (i in 0 until 5) {
                val bola = BallView(this, i)

                // Posición inicial normalizada (0.15 a 0.85)
                bola.posX = Random.nextFloat() * 0.7f + 0.15f
                bola.posY = Random.nextFloat() * 0.7f + 0.15f

                // CAMBIAR ESTA LÍNEA ↓↓↓
                val velocidad = Random.nextFloat() * 0.015f + 0.025f // Velocidad moderada

                val angulo = Random.nextFloat() * 2f * Math.PI.toFloat()
                bola.vx = kotlin.math.cos(angulo) * velocidad
                bola.vy = kotlin.math.sin(angulo) * velocidad

                val tamaño = (40 * resources.displayMetrics.density).toInt()
                val params = FrameLayout.LayoutParams(tamaño, tamaño)
                bola.layoutParams = params

                bolas.add(bola)
                areaRastreo.addView(bola)

                // Actualizar posición visual inicial
                bola.actualizarPosicion(ancho, alto)
            }

            // === MARCAR LAS PRIMERAS 2 COMO AZULES (OBJETIVOS) ===
            indicesObjetivo.add(0)
            indicesObjetivo.add(1)
            bolas[0].cambiarColor(Color.parseColor("#3B82F6"))
            bolas[1].cambiarColor(Color.parseColor("#3B82F6"))

            // === ESPERAR 2.5 SEGUNDOS ANTES DE EMPEZAR ===
            handler.postDelayed({
                if (!isFinishing) {
                    iniciarAnimacion()
                }
            }, 2500)
        }
    }

    /**
     * FUNCIÓN: Iniciar animación de movimiento
     */
    private fun iniciarAnimacion() {
        // Cambiar todas a blanco
        bolas.forEach { it.cambiarColor(Color.WHITE) }
        txtMensaje.text = "Rastreando..."
        animacionActiva = true

        var pasos = 0
        val maxPasos = 300 // ~5 segundos a 60 FPS

        animacionRunnable = object : Runnable {
            override fun run() {
                if (!animacionActiva || testFinalizado) return

                val ancho = areaRastreo.width.toFloat()
                val alto = areaRastreo.height.toFloat()

                // === MOVER TODAS LAS BOLAS ===
                bolas.forEach { bola ->
                    // Actualizar posición
                    bola.posX += bola.vx
                    bola.posY += bola.vy

                    // === REBOTES EN BORDES ===
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

                    // Actualizar posición visual
                    bola.actualizarPosicion(ancho, alto)
                }

                pasos++
                if (pasos >= maxPasos) {
                    // === TERMINAR ANIMACIÓN ===
                    animacionActiva = false
                    txtMensaje.text = "Toque los 2 objetivos:"
                    habilitarSeleccion()
                } else {
                    handler.postDelayed(this, 16) // ~60 FPS
                }
            }
        }
        handler.post(animacionRunnable!!)
    }

    /**
     * FUNCIÓN: Habilitar selección de bolas
     */
    private fun habilitarSeleccion() {
        btnConfirmar.visibility = View.GONE

        bolas.forEachIndexed { indice, bola ->
            bola.setOnClickListener {
                if (testFinalizado || indicesSeleccionados.size >= 2) return@setOnClickListener
                if (indicesSeleccionados.contains(indice)) return@setOnClickListener

                // Marcar como seleccionada (naranja)
                indicesSeleccionados.add(indice)
                bola.cambiarColor(Color.parseColor("#F59E0B"))

                // Actualizar botón
                btnConfirmar.text = "CONFIRMAR (${indicesSeleccionados.size})"

                if (indicesSeleccionados.size == 2) {
                    btnConfirmar.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * FUNCIÓN: Verificar selección del usuario
     */
    private fun verificarSeleccion() {
        testFinalizado = true
        animacionActiva = false
        animacionRunnable?.let { handler.removeCallbacks(it) }

        // === MOSTRAR OBJETIVOS CORRECTOS EN VERDE ===
        indicesObjetivo.forEach { indiceObjetivo ->
            bolas[indiceObjetivo].cambiarColor(Color.parseColor("#10B981"))
        }

        // === CONTAR ACIERTOS ===
        var aciertos = 0
        indicesSeleccionados.forEach { seleccionado ->
            if (indicesObjetivo.contains(seleccionado)) {
                aciertos++
            } else {
                // Marcar incorrectas en rojo
                bolas[seleccionado].cambiarColor(Color.parseColor("#EF4444"))
            }
        }

        // === CALCULAR PUNTAJE ===
        val puntaje = when (aciertos) {
            2 -> 100
            1 -> 50
            else -> 0
        }

        val mensaje = when (aciertos) {
            2 -> "Perfecto! +100"
            1 -> "1 correcta +50"
            else -> "Fallaste +0"
        }
        txtMensaje.text = mensaje

        // === ESPERAR 2 SEGUNDOS Y CONTINUAR ===
        handler.postDelayed({
            mostrarResultado(puntaje, aciertos)
        }, 2000)
    }

    /**
     * FUNCIÓN: Mostrar diálogo con resultado
     */
    private fun mostrarResultado(puntaje: Int, aciertos: Int) {
        val mensaje = when (aciertos) {
            2 -> {
                "EXCELENTE!\n\n" +
                        "Identificaste correctamente\n" +
                        "los 2 objetivos.\n\n" +
                        "Nota: $puntaje%"
            }
            1 -> {
                "REGULAR\n\n" +
                        "Identificaste solo 1 objetivo\n" +
                        "de los 2.\n\n" +
                        "Nota: $puntaje%"
            }
            else -> {
                "FALLASTE\n\n" +
                        "No identificaste ninguno\n" +
                        "de los objetivos.\n\n" +
                        "Nota: $puntaje%"
            }
        }

        AlertDialog.Builder(this)
            .setTitle("RASTREO (MOT)")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                manejarContinuacion(puntaje)
            }
            .show()
    }

    /**
     * FUNCIÓN: Manejar continuación
     */
    private fun manejarContinuacion(puntaje: Int) {
        CortexManager.guardarPuntaje("t8", puntaje)
        val intentoActual = CortexManager.obtenerIntentoActual("t8")

        if (intentoActual == 1 && puntaje < 95) {
            recreate()
        } else {
            CortexManager.navegarAlSiguiente(this)
            finish()
        }
    }

    /**
     * FUNCIÓN: Limpiar recursos
     */
    override fun onDestroy() {
        super.onDestroy()
        animacionActiva = false
        animacionRunnable?.let { handler.removeCallbacks(it) }
    }

    // ========================================
    // CLASE INTERNA: BallView
    // ========================================
    private class BallView(context: android.content.Context, val indice: Int) : View(context) {

        var posX: Float = 0f  // Posición normalizada 0-1
        var posY: Float = 0f
        var vx: Float = 0f    // Velocidad normalizada
        var vy: Float = 0f

        init {
            setBackgroundColor(Color.WHITE)
            val tamaño = (40 * context.resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(tamaño, tamaño)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            // Hacer circular
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        fun cambiarColor(color: Int) {
            setBackgroundColor(color)
        }

        fun actualizarPosicion(anchoContenedor: Float, altoContenedor: Float) {
            val params = layoutParams as FrameLayout.LayoutParams
            params.leftMargin = (posX * anchoContenedor - width / 2).toInt()
            params.topMargin = (posY * altoContenedor - height / 2).toInt()
            layoutParams = params
        }
    }
}