package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.math.abs
import kotlin.random.Random

class AnticipacionTestActivity : AppCompatActivity() {

    private lateinit var vehiculo: ImageView
    private lateinit var zonaMeta: View
    private lateinit var btnFrenar: Button
    private lateinit var pista: View
    private lateinit var orientationOverlay: FrameLayout
    private lateinit var countdownText: TextView

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false
    private var testIniciado = false
    private var duracionAnimacionActual: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)
        orientationOverlay = findViewById(R.id.orientation_overlay)
        countdownText = findViewById(R.id.txt_countdown)

        btnFrenar.setOnClickListener { if (juegoActivo) frenarVehiculo() }

        verificarOrientacion()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        verificarOrientacion()
    }

    private fun verificarOrientacion() {
        if (testIniciado) return
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientationOverlay.visibility = View.GONE
            iniciarCuentaRegresiva()
        } else {
            orientationOverlay.visibility = View.VISIBLE
        }
    }

    private fun iniciarCuentaRegresiva() {
        testIniciado = true
        countdownText.visibility = View.VISIBLE

        // Muestra qué intento es durante la cuenta regresiva
        val intento = CortexManager.obtenerIntentoActual("t3")

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                if (segundos > 0) {
                    countdownText.text = "$segundos\n(Intento $intento/2)"
                } else {
                    countdownText.text = "¡YA!"
                }
            }
            override fun onFinish() {
                countdownText.visibility = View.GONE
                programarInicioCarrera()
            }
        }.start()
    }

    private fun programarInicioCarrera() {
        vehiculo.translationX = 0f
        Handler(Looper.getMainLooper()).postDelayed({ iniciarCarrera() }, 500)
    }

    private fun iniciarCarrera() {
        if (isFinishing) return
        juegoActivo = true
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()
        vehiculo.translationX = 0f

        // --- ⚡ CAMBIO DE VELOCIDAD ⚡ ---
        // Antes: 2200 a 3200 ms (Lento)
        // Ahora: 1200 a 2000 ms (Más rápido y desafiante)
        duracionAnimacionActual = (1200 + Random.nextFloat() * 800).toLong()

        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = duracionAnimacionActual
            interpolator = LinearInterpolator()
            doOnEnd { if (juegoActivo) evaluarFrenado(falloTotal = true) }
            start()
        }
    }

    private fun frenarVehiculo() {
        juegoActivo = false
        animador?.pause()
        evaluarFrenado(falloTotal = false)
    }

    private fun evaluarFrenado(falloTotal: Boolean) {
        val puntaje: Int
        var mensaje: String
        val diferenciaAbsoluta: Float

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
            diferenciaAbsoluta = -1f
        } else {
            val centroVehiculo = vehiculo.x + vehiculo.width / 2
            val centroMeta = zonaMeta.x + zonaMeta.width / 2
            diferenciaAbsoluta = abs(centroVehiculo - centroMeta)

            // Hacemos el cálculo de puntaje un poco más estricto dado que es más rápido
            val diferenciaPorcentual = (diferenciaAbsoluta / pista.width) * 100
            val penalizacion = (diferenciaPorcentual * 6).toInt() // Penalización ligeramente mayor
            puntaje = (100 - penalizacion).coerceIn(0, 100)

            mensaje = when {
                puntaje >= 90 -> "¡PRECISIÓN PERFECTA! 😎"
                puntaje >= 70 -> "Buen cálculo."
                else -> "FRENADO IMPRECISO ⚠️"
            }
        }

        // --- REGISTRO DE MÉTRICAS ---
        val intentoActual = CortexManager.obtenerIntentoActual("t3")
        val details = mapOf(
            "distancia_px" to diferenciaAbsoluta,
            "velocidad_ms" to duracionAnimacionActual,
            "fallo_total" to falloTotal
        )
        CortexManager.logPerformanceMetric("t3", puntaje, details)
        CortexManager.guardarPuntaje("t3", puntaje) // CortexManager se encarga de saber si es intento 1 o 2

        // --- LÓGICA DE EXONERACIÓN (DINÁMICA TEST 1) ---
        if (intentoActual == 1 && puntaje < 80) {
            // Primer intento fallido o bajo -> REPETIR
            mostrarDialogoFin(
                titulo = "INTENTO FALLIDO",
                mensaje = "$mensaje\nNota: $puntaje%\n\nNecesitas mejorar la precisión en el segundo intento.",
                esReintento = true
            )
        } else {
            // Aprobó (>80) O ya es el segundo intento -> TERMINAR
            mostrarDialogoFin(
                titulo = if(puntaje >= 80) "PRUEBA SUPERADA ✅" else "TEST FINALIZADO",
                mensaje = "Resultado Final:\nNota: $puntaje%\n$mensaje",
                esReintento = false
            )
        }
    }

    private fun mostrarDialogoFin(titulo: String, mensaje: String, esReintento: Boolean) {
        if (isFinishing) return

        val textoBoton = if (esReintento) "INTENTO 2" else "SIGUIENTE TEST"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate() // Recarga la actividad para reiniciar el coche y la lógica
                } else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        animador?.cancel()
    }
}