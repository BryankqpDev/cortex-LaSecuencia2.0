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
        
        duracionAnimacionActual = (2200 + Random.nextFloat() * 1000).toLong()

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
        var mensaje: String
        val diferenciaAbsoluta: Float

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
            diferenciaAbsoluta = -1f // Valor para indicar fallo
        } else {
            val centroVehiculo = vehiculo.x + vehiculo.width / 2
            val centroMeta = zonaMeta.x + zonaMeta.width / 2
            diferenciaAbsoluta = abs(centroVehiculo - centroMeta)
            
            val diferenciaPorcentual = (diferenciaAbsoluta / pista.width) * 100
            val penalizacion = (diferenciaPorcentual * 5).toInt()
            puntaje = (100 - penalizacion).coerceIn(0, 100)

            mensaje = when {
                puntaje >= 95 -> "¡PRECISIÓN PERFECTA! 😎"
                puntaje >= 70 -> "Buen cálculo."
                else -> "CALIBRACIÓN NECESARIA ⚠️"
            }
        }

        // --- ✅ REGISTRO DE MÉTRICAS DETALLADO ---
        val details = mapOf(
            "distancia_del_centro_px" to diferenciaAbsoluta,
            "duracion_animacion_ms" to duracionAnimacionActual,
            "fallo_por_no_frenar" to falloTotal
        )
        CortexManager.logPerformanceMetric("t3", puntaje, details)
        CortexManager.guardarPuntaje("t3", puntaje, intentosRealizados == 1)

        if (puntaje >= 80) {
            mostrarExito(puntaje, mensaje)
        } else {
            if (intentosRealizados < MAX_INTENTOS) mostrarDialogoReintento(mensaje)
            else mostrarFalloFinal(mensaje)
        }
    }

    private fun mostrarDialogoReintento(razonFallo: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("INTENTO FALLIDO")
            .setMessage("$razonFallo\n\nQueda 1 intento.")
            .setCancelable(false)
            .setPositiveButton("REINTENTAR") { _, _ -> programarInicioCarrera() }
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