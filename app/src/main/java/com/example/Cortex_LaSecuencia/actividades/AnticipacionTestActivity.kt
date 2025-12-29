package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
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

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false

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

        btnFrenar.setOnClickListener {
            if (juegoActivo) frenarVehiculo()
        }

        // Inicia el primer intento
        programarInicioCarrera()
    }

    private fun programarInicioCarrera() {
        Handler(Looper.getMainLooper()).postDelayed({
            iniciarCarrera()
        }, 1000)
    }

    private fun iniciarCarrera() {
        if (isFinishing) return

        juegoActivo = true
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()

        vehiculo.translationX = 0f // Reset posición

        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = 2500
            interpolator = LinearInterpolator()
            doOnEnd {
                if (juegoActivo) evaluarFrenado(falloTotal = true)
            }
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

        var puntaje = 0
        var mensaje = ""

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
        } else {
            val centroVehiculo = vehiculo.x + (vehiculo.width / 2)
            val centroMeta = zonaMeta.x + (zonaMeta.width / 2)
            val diferencia = abs(centroVehiculo - centroMeta)
            val radioMeta = zonaMeta.width / 2

            puntaje = when {
                diferencia < (radioMeta * 0.5) -> 100
                diferencia < radioMeta -> 80
                diferencia < (radioMeta * 1.5) -> 40
                else -> 0
            }
            mensaje = if (puntaje >= 80) "¡EXCELENTE! 😎" else "CALIBRACIÓN NECESARIA ⚠️"
        }

        // --- LÓGICA DE DECISIÓN ---

        if (puntaje >= 80) {
            // CASO A: APROBÓ -> Siguiente nivel
            mostrarExito(puntaje, mensaje)
        } else {
            // CASO B: FALLÓ
            if (intentosRealizados < MAX_INTENTOS) {
                // Le queda 1 intento -> Reintento rápido (mismo nivel, no reinicia activity)
                mostrarDialogoReintento(mensaje)
            } else {
                // Se acabaron los intentos -> REINICIAR DESDE EL PRINCIPIO (Loop)
                reiniciarNivelCompleto(mensaje)
            }
        }
    }

    private fun mostrarDialogoReintento(razonFallo: String) {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("INTENTO FALLIDO ($intentosRealizados/$MAX_INTENTOS)")
            .setMessage("$razonFallo\n\nTe queda 1 oportunidad más.\n¡Concéntrate!")
            .setCancelable(false)
            .setPositiveButton("REINTENTAR") { _, _ ->
                vehiculo.translationX = 0f
                programarInicioCarrera()
            }
            .show()
    }

    private fun reiniciarNivelCompleto(razonFallo: String) {
        if (isFinishing) return

        // No guardamos puntaje porque vamos a reiniciar
        AlertDialog.Builder(this)
            .setTitle("¡INTENTOS AGOTADOS! ❌")
            .setMessage("$razonFallo\n\nHas fallado los 2 intentos.\nLa prueba se reiniciará desde el principio.")
            .setCancelable(false)
            .setPositiveButton("REINICIAR T3") { _, _ ->
                // ESTO REINICIA LA ACTIVIDAD DESDE CERO
                // (Como si acabara de entrar, reset de vidas a 0)
                recreate()
            }
            .show()
    }

    private fun mostrarExito(puntaje: Int, mensaje: String) {
        if (isFinishing) return

        CortexManager.guardarPuntaje("t3", puntaje)

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