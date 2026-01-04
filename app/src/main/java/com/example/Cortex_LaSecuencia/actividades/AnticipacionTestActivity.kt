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

class AnticipacionTestActivity : AppCompatActivity() {

    private lateinit var vehiculo: ImageView
    private lateinit var zonaMeta: View
    private lateinit var btnFrenar: Button
    private lateinit var pista: View
    private lateinit var orientationOverlay: FrameLayout
    private lateinit var countdownText: TextView

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false
    private var testIniciado = false // Para controlar que el test no empiece hasta después de la cuenta atrás

    // Control de intentos
    private var intentosRealizados = 0
    private val MAX_INTENTOS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)
        orientationOverlay = findViewById(R.id.orientation_overlay)
        countdownText = findViewById(R.id.txt_countdown)

        btnFrenar.setOnClickListener {
            if (juegoActivo) frenarVehiculo()
        }
        
        verificarOrientacion()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        verificarOrientacion()
    }

    private fun verificarOrientacion() {
        // Si el test ya empezó, no hacemos nada
        if (testIniciado) return

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientationOverlay.visibility = View.GONE
            iniciarCuentaRegresiva()
        } else {
            orientationOverlay.visibility = View.VISIBLE
        }
    }

    private fun iniciarCuentaRegresiva() {
        testIniciado = true // Marcamos que el proceso de inicio ha comenzado
        countdownText.visibility = View.VISIBLE

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                if (segundos > 0) {
                    countdownText.text = segundos.toString()
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
        Handler(Looper.getMainLooper()).postDelayed({
            iniciarCarrera()
        }, 500) // Pequeña pausa después de la cuenta atrás
    }

    private fun iniciarCarrera() {
        if (isFinishing) return

        juegoActivo = true
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()

        vehiculo.translationX = 0f

        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = 2500
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

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
        } else {
            val centroVehiculo = vehiculo.x + vehiculo.width / 2
            val centroMeta = zonaMeta.x + zonaMeta.width / 2
            val diferencia = abs(centroVehiculo - centroMeta)
            val radioMeta = zonaMeta.width / 2

            puntaje = when {
                diferencia < radioMeta * 0.5 -> 100
                diferencia < radioMeta -> 80
                else -> 0
            }
            mensaje = if (puntaje >= 80) "¡PRECISIÓN PERFECTA! 😎" else "CALIBRACIÓN NECESARIA ⚠️"
        }

        CortexManager.guardarPuntaje("t3", puntaje, intentosRealizados == 1)

        if (puntaje >= 80) {
            mostrarExito(puntaje, mensaje)
        } else {
            if (intentosRealizados < MAX_INTENTOS) {
                mostrarDialogoReintento(mensaje)
            } else {
                mostrarFalloFinal(mensaje)
            }
        }
    }

    private fun mostrarDialogoReintento(razonFallo: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("INTENTO FALLIDO")
            .setMessage("$razonFallo\n\nQueda 1 intento.")
            .setCancelable(false)
            .setPositiveButton("REINTENTAR") { _, _ ->
                programarInicioCarrera()
            }
            .show()
    }

    private fun mostrarFalloFinal(razonFallo: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("PRUEBA FALLIDA ❌")
            .setMessage("$razonFallo\n\nNo has superado la prueba de anticipación.")
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    private fun mostrarExito(puntaje: Int, mensaje: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("PRUEBA SUPERADA ✅")
            .setMessage("Precisión: $puntaje%\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        animador?.cancel()
    }
}