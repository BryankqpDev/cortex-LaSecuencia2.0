package com.example.Cortex_LaSecuencia.actividades

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
    private val META_PUNTOS = 5 // Objetivo del HTML original
    private val random = Random()
    private var juegoTerminado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coordinacion_test)

        containerJuego = findViewById(R.id.container_juego)
        txtContador = findViewById(R.id.txt_contador)

        // Damos 1 segundo de respiro antes de empezar
        Handler(Looper.getMainLooper()).postDelayed({
            spawnearBolita()
        }, 1000)
    }

    private fun spawnearBolita() {
        if (juegoTerminado || isFinishing) return

        // 1. Crear la vista de la bolita programáticamente
        val bolita = View(this)
        bolita.setBackgroundResource(R.drawable.circulo_amarillo)

        // Tamaño de la bolita (50dp convertidos a pixeles)
        val sizePx = (50 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(sizePx, sizePx)
        bolita.layoutParams = params

        // 2. Calcular posición aleatoria dentro del contenedor
        // Restamos el tamaño de la bolita para que no se salga del borde
        val maxX = containerJuego.width - sizePx
        val maxY = containerJuego.height - sizePx

        // Validación de seguridad por si la pantalla no ha cargado
        if (maxX > 0 && maxY > 0) {
            bolita.x = random.nextInt(maxX).toFloat()
            bolita.y = random.nextInt(maxY).toFloat()
        }

        // 3. Listener: Qué pasa al tocarla
        bolita.setOnClickListener {
            containerJuego.removeView(bolita) // Desaparece
            puntosAtrapados++
            actualizarMarcador()

            if (puntosAtrapados >= META_PUNTOS) {
                finalizarJuego()
            } else {
                // Aparece la siguiente inmediatamente
                spawnearBolita()
            }
        }

        // 4. Agregar al juego
        containerJuego.addView(bolita)
    }

    private fun actualizarMarcador() {
        txtContador.text = "ATRAPADOS: $puntosAtrapados / $META_PUNTOS"
    }

    private fun finalizarJuego() {
        juegoTerminado = true

        // Guardamos puntaje (100 si completó los 5)
        CortexManager.guardarPuntaje("t4", 100)

        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("RESULTADO T4")
            .setMessage("Coordinación verificada.\nObjetivos: 5/5")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}