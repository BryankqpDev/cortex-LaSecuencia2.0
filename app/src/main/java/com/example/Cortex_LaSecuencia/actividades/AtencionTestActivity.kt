package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class AtencionTestActivity : AppCompatActivity() {

    private lateinit var txtPalabra: TextView
    private lateinit var txtContador: TextView

    // Botones
    private lateinit var btnRojo: Button
    private lateinit var btnVerde: Button
    private lateinit var btnAzul: Button
    private lateinit var btnAmarillo: Button

    // Datos del juego
    private val palabras = listOf("ROJO", "VERDE", "AZUL", "AMARILLO")

    // Colores HEX (Rojo, Verde, Azul, Amarillo)
    private val coloresHex = listOf(
        Color.parseColor("#EF4444"),
        Color.parseColor("#10B981"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#F59E0B")
    )

    private var rondaActual = 0
    private val TOTAL_RONDAS = 10
    private var aciertos = 0
    private var colorCorrectoActual = -1
    private var testFinalizado = false // Control de seguridad

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_atencion_test)

        txtPalabra = findViewById(R.id.txt_palabra_stroop)
        txtContador = findViewById(R.id.txt_contador)

        btnRojo = findViewById(R.id.btn_rojo)
        btnVerde = findViewById(R.id.btn_verde)
        btnAzul = findViewById(R.id.btn_azul)
        btnAmarillo = findViewById(R.id.btn_amarillo)

        btnRojo.setOnClickListener { verificarRespuesta(0) }
        btnVerde.setOnClickListener { verificarRespuesta(1) }
        btnAzul.setOnClickListener { verificarRespuesta(2) }
        btnAmarillo.setOnClickListener { verificarRespuesta(3) }

        generarEstimuloStroop()
    }

    private fun generarEstimuloStroop() {
        if (testFinalizado) return

        if (rondaActual >= TOTAL_RONDAS) {
            finalizarPrueba()
            return
        }

        rondaActual++
        txtContador.text = "Ronda $rondaActual / $TOTAL_RONDAS"

        // Lógica Stroop: Palabra vs Color
        val indicePalabra = Random.nextInt(0, 4)
        txtPalabra.text = palabras[indicePalabra]

        colorCorrectoActual = Random.nextInt(0, 4)
        txtPalabra.setTextColor(coloresHex[colorCorrectoActual])
    }

    private fun verificarRespuesta(indiceSeleccionado: Int) {
        if (testFinalizado) return

        if (indiceSeleccionado == colorCorrectoActual) {
            aciertos++
        }

        // Pequeño delay para que no sea tan brusco
        Handler(Looper.getMainLooper()).postDelayed({
            generarEstimuloStroop()
        }, 150)
    }

    private fun finalizarPrueba() {
        if (isFinishing || testFinalizado) return
        testFinalizado = true

        val nota = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()

        // Guardamos la nota del T5
        CortexManager.guardarPuntaje("t5", nota)

        val mensaje = if (nota >= 80) "¡Enfoque agudo! 🧠" else "Atención dispersa."

        AlertDialog.Builder(this)
            .setTitle("NIVEL 5 COMPLETADO") // Título más claro de progreso
            .setMessage("Aciertos: $aciertos/$TOTAL_RONDAS\nNota: $nota%\n\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("CONTINUAR AL TEST 6 ➡️") { _, _ ->
                // Esto llama al cerebro para buscar el "t6"
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}