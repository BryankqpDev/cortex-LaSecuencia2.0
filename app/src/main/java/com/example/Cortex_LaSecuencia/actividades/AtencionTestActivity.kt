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
    private lateinit var txtIntento: TextView // Si no tienes este ID en el XML, bórralo y usa el contador

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
    private var testFinalizado = false
    private var intentoActual = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_atencion_test)

        txtPalabra = findViewById(R.id.txt_palabra_stroop)
        txtContador = findViewById(R.id.txt_contador)

        // OPCIONAL: Si tienes un TextView para el intento en tu XML
        // txtIntento = findViewById(R.id.txt_intento)

        btnRojo = findViewById(R.id.btn_rojo)
        btnVerde = findViewById(R.id.btn_verde)
        btnAzul = findViewById(R.id.btn_azul)
        btnAmarillo = findViewById(R.id.btn_amarillo)

        // Obtenemos el intento actual (1 o 2)
        intentoActual = CortexManager.obtenerIntentoActual("t5")

        // Actualizamos texto inicial si tienes el campo, sino lo ignoras
        // txtIntento.text = "INTENTO $intentoActual/2"

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
        // Mostramos Intento y Ronda en el mismo texto para ahorrar espacio si no tienes otro TextView
        txtContador.text = "Intento $intentoActual | Ronda $rondaActual / $TOTAL_RONDAS"

        // Lógica Stroop: Palabra vs Color
        val indicePalabra = Random.nextInt(0, 4)
        txtPalabra.text = palabras[indicePalabra]

        // El color real del texto (lo que el usuario debe identificar)
        colorCorrectoActual = Random.nextInt(0, 4)
        txtPalabra.setTextColor(coloresHex[colorCorrectoActual])
    }

    private fun verificarRespuesta(indiceSeleccionado: Int) {
        if (testFinalizado) return

        if (indiceSeleccionado == colorCorrectoActual) {
            aciertos++
        }

        // Delay de 150ms para fluidez
        Handler(Looper.getMainLooper()).postDelayed({
            generarEstimuloStroop()
        }, 150)
    }

    private fun finalizarPrueba() {
        if (isFinishing || testFinalizado) return
        testFinalizado = true

        val nota = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()

        // Guardamos métricas detalladas
        val details = mapOf("aciertos" to aciertos, "total_rondas" to TOTAL_RONDAS)
        CortexManager.logPerformanceMetric("t5", nota, details)
        CortexManager.guardarPuntaje("t5", nota)

        // --- LÓGICA DE EXONERACIÓN ---
        // Necesitas al menos 8 aciertos de 10 (80%) para pasar a la primera
        if (intentoActual == 1 && nota < 80) {
            // Reprobó intento 1 -> REPETIR
            mostrarDialogoFin(nota, esReintento = true)
        } else {
            // Aprobó o es intento 2 -> SIGUIENTE
            mostrarDialogoFin(nota, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(nota: Int, esReintento: Boolean) {
        if (isFinishing) return

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "ATENCIÓN DISPERSA ⚠️"
            mensaje = "Has acertado $aciertos de $TOTAL_RONDAS.\nNota: $nota%\n\nNecesitas más concentración. Tienes un segundo intento."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (nota >= 80) "¡ENFOQUE AGUDO! 🧠" else "TEST FINALIZADO"
            mensaje = "Aciertos: $aciertos de $TOTAL_RONDAS\nNota Final: $nota%"
            textoBoton = "SIGUIENTE TEST"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate() // Reinicia la actividad
                } else {
                    CortexManager.navegarAlSiguiente(this) // Se va al T6
                    finish()
                }
            }
            .show()
    }
}