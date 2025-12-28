package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.os.Bundle
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

class AnticipacionTestActivity : AppCompatActivity() {

    private lateinit var vehiculo: ImageView
    private lateinit var zonaMeta: View
    private lateinit var btnFrenar: Button
    private lateinit var pista: View

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        // AQUI ESTABA EL ERROR: Ahora usamos los IDs correctos que pegaste en el XML
        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)

        btnFrenar.setOnClickListener {
            if (juegoActivo) frenarVehiculo()
        }

        // Inicia el juego tras 1 segundo
        Handler(Looper.getMainLooper()).postDelayed({
            iniciarCarrera()
        }, 1000)
    }

    private fun iniciarCarrera() {
        juegoActivo = true
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()

        // Animación fluida
        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = 2500
            interpolator = LinearInterpolator()
            doOnEnd {
                if (juegoActivo) evaluarFrenado(true)
            }
            start()
        }
    }

    private fun frenarVehiculo() {
        juegoActivo = false
        animador?.pause()
        evaluarFrenado(false)
    }

    private fun evaluarFrenado(falloTotal: Boolean) {
        if (falloTotal) {
            mostrarResultado(0, "¡REACCIÓN TARDÍA! ❌")
            return
        }

        val centroVehiculo = vehiculo.x + (vehiculo.width / 2)
        val centroMeta = zonaMeta.x + (zonaMeta.width / 2)
        val diferencia = abs(centroVehiculo - centroMeta)
        val radioMeta = zonaMeta.width / 2

        val puntaje = when {
            diferencia < (radioMeta * 0.5) -> 100
            diferencia < radioMeta -> 80
            diferencia < (radioMeta * 1.5) -> 40
            else -> 0
        }

        val mensaje = if (puntaje >= 80) "¡BUEN CÁLCULO! 😎" else "CALIBRACIÓN NECESARIA ⚠️"
        mostrarResultado(puntaje, mensaje)
    }

    private fun mostrarResultado(puntaje: Int, mensaje: String) {
        CortexManager.guardarPuntaje("t3", puntaje)
        AlertDialog.Builder(this)
            .setTitle("RESULTADO T3")
            .setMessage("Precisión: $puntaje%\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}