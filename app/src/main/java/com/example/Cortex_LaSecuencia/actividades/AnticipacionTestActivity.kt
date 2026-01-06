package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
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
    private lateinit var countdownText: TextView

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false
    private var duracionAnimacionActual: Long = 0

    private var intentosRealizados = 0
    private val MAX_INTENTOS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)
        countdownText = findViewById(R.id.txt_countdown)

        btnFrenar.setOnClickListener { if (juegoActivo) frenarVehiculo() }

        iniciarCuentaRegresiva()
    }

    private fun iniciarCuentaRegresiva() {
        countdownText.visibility = View.VISIBLE

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                countdownText.text = if (segundos > 0) segundos.toString() else "¡YA!"
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

        duracionAnimacionActual = (600 + Random.nextFloat() * 400).toLong()

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
        intentosRealizados++

        val puntaje: Int
        val mensaje: String
        val diferenciaAbsoluta: Float

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
            diferenciaAbsoluta = -1f
        } else {
            val centroVehiculo = vehiculo.x + vehiculo.width / 2
            val centroMeta = zonaMeta.x + zonaMeta.width / 2
            diferenciaAbsoluta = abs(centroVehiculo - centroMeta)

            val diferenciaPorcentual = (diferenciaAbsoluta / pista.width) * 100
            val penalizacion = (diferenciaPorcentual * 6).toInt()
            puntaje = (100 - penalizacion).coerceIn(0, 100)

            mensaje = when {
                puntaje >= 90 -> "¡PRECISIÓN PERFECTA! 😎"
                puntaje >= 70 -> "Buen cálculo."
                else -> "FRENADO IMPRECISO ⚠️"
            }
        }

        val intentoActual = CortexManager.obtenerIntentoActual("t3")
        val details = mapOf("distancia_px" to diferenciaAbsoluta, "velocidad_ms" to duracionAnimacionActual, "fallo_total" to falloTotal)
        CortexManager.logPerformanceMetric("t3", puntaje, details)
        CortexManager.guardarPuntaje("t3", puntaje)

        if (intentoActual == 1 && puntaje < 80) {
            mostrarDialogoFin("INTENTO FALLIDO", "$mensaje\nNota: $puntaje%\n\nQueda 1 intento.", true)
        } else {
            mostrarDialogoFin(if(puntaje >= 80) "PRUEBA SUPERADA ✅" else "TEST FINALIZADO", "Resultado Final:\nNota: $puntaje%\n$mensaje", false)
        }
    }

    private fun mostrarDialogoFin(titulo: String, mensaje: String, esReintento: Boolean) {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(if (esReintento) "REINTENTAR" else "SIGUIENTE") { _, _ ->
                if (esReintento) recreate()
                else {
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