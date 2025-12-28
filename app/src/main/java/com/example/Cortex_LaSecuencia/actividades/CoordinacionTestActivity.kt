package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import java.util.Random

class CoordinacionTestActivity : AppCompatActivity() {

    private lateinit var containerJuego: FrameLayout
    private lateinit var txtContador: TextView

    private var puntosAtrapados = 0
    private val META_PUNTOS = 5

    private val random = Random()
    private var juegoActivo = true
    private val handler = Handler(Looper.getMainLooper())

    // Keys para guardar la velocidad dentro de cada vista
    private val TAG_VX = R.id.tag_vx_key // Necesitaremos definir esto en resources
    private val TAG_VY = R.id.tag_vy_key // Necesitaremos definir esto en resources

    // Velocidad base (pixeles por frame)
    private val VELOCIDAD_BASE = 15f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)

        txtContador.text = "¡ATRAPA 5 AMARILLAS! (Rebotan)"

        // Iniciar el juego después de 1 segundo
        handler.postDelayed({
            iniciarGenerador()
            iniciarMotorFisica()
        }, 1000)
    }

    // --- 1. GENERADOR DE BOLITAS (El que las crea) ---
    private fun iniciarGenerador() {
        val generador = object : Runnable {
            override fun run() {
                if (juegoActivo && !isFinishing) {
                    spawnearEntidadRebotante()
                    // Aparece una nueva cada 800ms (ajústalo según dificultad)
                    handler.postDelayed(this, 800)
                }
            }
        }
        handler.post(generador)
    }

    // --- 2. MOTOR DE FÍSICA (El que las mueve y hace rebotar) ---
    private fun iniciarMotorFisica() {
        val physicsLoop = object : Runnable {
            override fun run() {
                if (!juegoActivo) return
                actualizarPosicionesYRebotes()
                // Ejecutar esto aprox 60 veces por segundo (cada 16ms)
                handler.postDelayed(this, 16)
            }
        }
        handler.post(physicsLoop)
    }

    private fun actualizarPosicionesYRebotes() {
        val containerW = containerJuego.width.toFloat()
        val containerH = containerJuego.height.toFloat()

        // Recorremos todas las bolitas que hay en pantalla
        for (i in 0 until containerJuego.childCount) {
            val bolita = containerJuego.getChildAt(i)

            // Obtenemos sus velocidades actuales (si no tienen, usamos 0)
            var vx = bolita.getTag(TAG_VX) as? Float ?: 0f
            var vy = bolita.getTag(TAG_VY) as? Float ?: 0f

            // Calculamos nueva posición
            var nextX = bolita.x + vx
            var nextY = bolita.y + vy
            val size = bolita.width.toFloat()

            // --- LÓGICA DE REBOTE ---

            // Rebote Horizontal (Izquierda / Derecha)
            if (nextX < 0) {
                nextX = 0f
                vx = -vx // Invertir dirección X
            } else if (nextX + size > containerW) {
                nextX = containerW - size
                vx = -vx // Invertir dirección X
            }

            // Rebote Vertical (Arriba / Abajo)
            if (nextY < 0) {
                nextY = 0f
                vy = -vy // Invertir dirección Y
            } else if (nextY + size > containerH) {
                nextY = containerH - size
                vy = -vy // Invertir dirección Y
            }

            // Aplicamos la nueva posición y guardamos las nuevas velocidades
            bolita.x = nextX
            bolita.y = nextY
            bolita.setTag(TAG_VX, vx)
            bolita.setTag(TAG_VY, vy)
        }
    }

    private fun spawnearEntidadRebotante() {
        if (!juegoActivo || containerJuego.width == 0) return

        val view = View(this)

        // Decidir tipo (Amarillo objetivo vs Rojo distractor)
        val esAmarillo = random.nextInt(100) < 40 // 40% chance amarillo
        if (esAmarillo) {
            view.setBackgroundResource(R.drawable.circulo_amarillo)
            view.tag = "OBJETIVO"
        } else {
            view.setBackgroundResource(R.drawable.circulo_rojo)
            view.tag = "DISTRACTOR"
        }

        val sizePx = (55 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(sizePx, sizePx)
        view.layoutParams = params

        // Posición inicial ALEATORIA dentro de la pantalla
        val maxX = containerJuego.width - sizePx
        val maxY = containerJuego.height - sizePx
        view.x = random.nextInt(maxX).toFloat()
        view.y = random.nextInt(maxY).toFloat()

        // Velocidad inicial ALEATORIA (Dirección y magnitud)
        // vx va de -VELOCIDAD_BASE a +VELOCIDAD_BASE
        val vx = (random.nextFloat() * 2 - 1) * VELOCIDAD_BASE
        val vy = (random.nextFloat() * 2 - 1) * VELOCIDAD_BASE

        // Guardamos la velocidad en la vista para que el motor de física la use
        view.setTag(TAG_VX, vx)
        view.setTag(TAG_VY, vy)

        containerJuego.addView(view)

        // Interacción (Tap)
        view.setOnClickListener {
            if (view.tag == "OBJETIVO") {
                puntosAtrapados++
                txtContador.text = "ATRAPADOS: $puntosAtrapados / $META_PUNTOS"
                txtContador.setTextColor(Color.GREEN)
                containerJuego.removeView(view)
                if (puntosAtrapados >= META_PUNTOS) finalizarJuego()
            } else {
                txtContador.text = "¡CUIDADO CON LAS ROJAS!"
                txtContador.setTextColor(Color.RED)
                view.alpha = 0.5f
            }
        }
    }

    private fun finalizarJuego() {
        if (!juegoActivo) return
        juegoActivo = false
        handler.removeCallbacksAndMessages(null) // Detener motor y generador

        CortexManager.guardarPuntaje("t4", 100)

        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("COORDINACIÓN SUPERADA ⚡")
            .setMessage("Objetivos dinámicos capturados.\nPrueba finalizada.")
            .setCancelable(false)
            .setPositiveButton("FINALIZAR EVALUACIÓN") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        juegoActivo = false
        handler.removeCallbacksAndMessages(null)
    }
}