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

    // Colores HEX idénticos a tu HTML/CSS
    // Red: #EF4444, Green: #10B981, Blue: #3B82F6, Yellow: #F59E0B
    private val coloresHex = listOf(
        Color.parseColor("#EF4444"), // Rojo
        Color.parseColor("#10B981"), // Verde
        Color.parseColor("#3B82F6"), // Azul
        Color.parseColor("#F59E0B")  // Amarillo
    )

    // Mapeo para saber qué índice es qué color
    // 0=Rojo, 1=Verde, 2=Azul, 3=Amarillo

    private var rondaActual = 0
    private val TOTAL_RONDAS = 10
    private var aciertos = 0
    private var colorCorrectoActual = -1 // Índice del color (0-3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_atencion_test)

        txtPalabra = findViewById(R.id.txt_palabra_stroop)
        txtContador = findViewById(R.id.txt_contador)

        btnRojo = findViewById(R.id.btn_rojo)
        btnVerde = findViewById(R.id.btn_verde)
        btnAzul = findViewById(R.id.btn_azul)
        btnAmarillo = findViewById(R.id.btn_amarillo)

        // Configurar Listeners (0=Rojo, 1=Verde, 2=Azul, 3=Amarillo)
        btnRojo.setOnClickListener { verificarRespuesta(0) }
        btnVerde.setOnClickListener { verificarRespuesta(1) }
        btnAzul.setOnClickListener { verificarRespuesta(2) }
        btnAmarillo.setOnClickListener { verificarRespuesta(3) }

        // Iniciar
        generarEstimuloStroop()
    }

    private fun generarEstimuloStroop() {
        if (rondaActual >= TOTAL_RONDAS) {
            finalizarPrueba()
            return
        }

        rondaActual++
        txtContador.text = "Ronda $rondaActual / $TOTAL_RONDAS"

        // 1. Elegir qué palabra mostramos (EJ: "AZUL") - Esto es el distractor
        val indicePalabra = Random.nextInt(0, 4)
        txtPalabra.text = palabras[indicePalabra]

        // 2. Elegir de qué color la pintamos (EJ: Rojo) - Esta es la respuesta correcta
        colorCorrectoActual = Random.nextInt(0, 4)
        txtPalabra.setTextColor(coloresHex[colorCorrectoActual])
    }

    private fun verificarRespuesta(indiceSeleccionado: Int) {
        if (indiceSeleccionado == colorCorrectoActual) {
            // ¡Correcto! (El usuario eligió el color de la tinta)
            aciertos++
        } else {
            // Error (El usuario leyó la palabra o se equivocó)
            // Opcional: Vibrar o sonido
        }

        // Siguiente ronda inmediata (o con pequeño delay si prefieres)
        Handler(Looper.getMainLooper()).postDelayed({
            generarEstimuloStroop()
        }, 150) // Pequeño delay para feedback visual del botón
    }

    private fun finalizarPrueba() {
        if (isFinishing) return

        // Calculamos nota (Simple regla de 3)
        val nota = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()

        CortexManager.guardarPuntaje("t5", nota)

        val mensaje = if (nota >= 80) "¡Excelente enfoque!" else "Cuidado con las distracciones."

        AlertDialog.Builder(this)
            .setTitle("ATENCIÓN EVALUADA")
            .setMessage("Aciertos: $aciertos de $TOTAL_RONDAS\nNota: $nota%\n$mensaje")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }
}