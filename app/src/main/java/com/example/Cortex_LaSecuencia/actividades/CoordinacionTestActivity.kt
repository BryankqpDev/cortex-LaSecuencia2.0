package com.example.Cortex_LaSecuencia.actividades

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
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
    private val META_PUNTOS = 5 // Objetivo: Atrapas 5 amarillas para ganar

    private val random = Random()
    private var juegoActivo = true
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)

        // Instrucción inicial
        txtContador.text = "¡ATRAPA 5 AMARILLAS! (Evita las rojas)"

        // Iniciar el caos después de 1 segundo
        handler.postDelayed({
            iniciarGeneradorDeBolitas()
        }, 1000)
    }

    private fun iniciarGeneradorDeBolitas() {
        // Este Runnable se llama a sí mismo para crear un bucle infinito
        val generador = object : Runnable {
            override fun run() {
                if (juegoActivo && !isFinishing) {
                    spawnearEntidadDinamica()
                    // Velocidad de aparición: Cada 600ms sale una nueva (ajústalo si es muy difícil)
                    handler.postDelayed(this, 600)
                }
            }
        }
        handler.post(generador)
    }

    private fun spawnearEntidadDinamica() {
        if (!juegoActivo) return

        val view = View(this)

        // --- 1. DECIDIR TIPO (Amarillo o Rojo) ---
        // 40% probabilidad de ser Amarillo (Objetivo), 60% Rojo (Distractor)
        val esObjetivo = random.nextBoolean() && random.nextBoolean() == false
        // (Ajuste simple: aprox 1 de cada 3 será amarilla para que sea reto buscarla)
        val esAmarillo = random.nextInt(100) < 40 // 40% chance de ser amarilla

        if (esAmarillo) {
            view.setBackgroundResource(R.drawable.circulo_amarillo)
            view.tag = "OBJETIVO" // Etiqueta para saber que es la buena
        } else {
            view.setBackgroundResource(R.drawable.circulo_rojo)
            view.tag = "DISTRACTOR"
        }

        // Tamaño
        val sizePx = (55 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(sizePx, sizePx)
        view.layoutParams = params

        // --- 2. POSICIÓN INICIAL Y FINAL (Trayectoria) ---
        val containerW = containerJuego.width
        val containerH = containerJuego.height

        if (containerW == 0 || containerH == 0) return

        // Comenzar en un punto aleatorio de la izquierda o arriba
        val startX = random.nextInt(containerW - sizePx).toFloat()
        val startY = containerH.toFloat() + 100f // Empieza abajo (fuera de pantalla)

        // Definimos movimiento: De abajo hacia arriba (tipo burbujas) o aleatorio
        // Para hacerlo más caótico como pediste:
        view.x = random.nextInt(containerW - sizePx).toFloat()
        view.y = containerH.toFloat() // Empieza abajo

        val destinoY = -200f // Termina arriba (fuera de pantalla)
        val destinoX = random.nextInt(containerW - sizePx).toFloat() // Se mueve un poco lateralmente

        // Agregamos la vista
        containerJuego.addView(view)

        // --- 3. ANIMACIÓN DE MOVIMIENTO ---
        // Duración aleatoria entre 2 y 4 segundos (algunas rápidas, otras lentas)
        val duracion = (2000 + random.nextInt(2000)).toLong()

        val animX = ObjectAnimator.ofFloat(view, "translationX", view.x, destinoX)
        val animY = ObjectAnimator.ofFloat(view, "translationY", view.y, destinoY)

        animX.duration = duracion
        animY.duration = duracion

        animX.start()
        animY.start()

        // --- 4. LIMPIEZA AUTOMÁTICA ---
        // Si la animación termina y nadie la tocó, la borramos para no llenar la memoria
        animY.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                containerJuego.removeView(view)
            }
        })

        // --- 5. INTERACCIÓN (TAP) ---
        view.setOnClickListener {
            if (view.tag == "OBJETIVO") {
                // ¡ACIERTO!
                puntosAtrapados++
                txtContador.text = "ATRAPADOS: $puntosAtrapados / $META_PUNTOS"
                txtContador.setTextColor(Color.GREEN)

                containerJuego.removeView(view) // Desaparece al tocar

                if (puntosAtrapados >= META_PUNTOS) {
                    finalizarJuego()
                }
            } else {
                // ¡ERROR! (Tocó roja)
                txtContador.text = "¡CUIDADO CON LAS ROJAS!"
                txtContador.setTextColor(Color.RED)
                view.alpha = 0.5f // Feedback visual de que se equivocó
            }
        }
    }

    private fun finalizarJuego() {
        if (!juegoActivo) return
        juegoActivo = false
        handler.removeCallbacksAndMessages(null) // Detener generación

        CortexManager.guardarPuntaje("t4", 100)

        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("COORDINACIÓN SUPERADA ⚡")
            .setMessage("Has capturado los objetivos móviles.\nNivel de atención: ÓPTIMO.")
            .setCancelable(false)
            .setPositiveButton("FINALIZAR EVALUACIÓN") { _, _ ->
                // Aquí termina el flujo por ahora, o va al reporte
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        juegoActivo = false
        handler.removeCallbacksAndMessages(null) // Limpiar memoria
    }
}