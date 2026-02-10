package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class AtencionTestActivity : TestBaseActivity() {

    private lateinit var txtPalabra: TextView
    private lateinit var txtContador: TextView

    private lateinit var btnRojo: Button
    private lateinit var btnVerde: Button
    private lateinit var btnAzul: Button
    private lateinit var btnAmarillo: Button

    private val palabras = listOf("ROJO", "VERDE", "AZUL", "AMARILLO")
    private val coloresHex = listOf(
        Color.parseColor("#EF4444"),
        Color.parseColor("#10B981"),
        Color.parseColor("#3B82F6"),
        Color.parseColor("#F59E0B")
    )

    private var rondaActual = 0
    private val TOTAL_RONDAS = 4
    private var aciertos = 0
    private var colorCorrectoActual = -1
    private var intentoActual = 1

    override fun obtenerTestId(): String = "t5"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_atencion_test)

        txtPalabra = findViewById(R.id.txt_palabra_stroop)
        txtContador = findViewById(R.id.txt_contador)

        btnRojo = findViewById(R.id.btn_rojo)
        btnVerde = findViewById(R.id.btn_verde)
        btnAzul = findViewById(R.id.btn_azul)
        btnAmarillo = findViewById(R.id.btn_amarillo)

        intentoActual = CortexManager.obtenerIntentoActual("t5")

        // --- ✅ ACTIVAR SENTINEL (CÁMARA) ---
        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        btnRojo.setOnClickListener { if (!testFinalizado) verificarRespuesta(0) }
        btnVerde.setOnClickListener { if (!testFinalizado) verificarRespuesta(1) }
        btnAzul.setOnClickListener { if (!testFinalizado) verificarRespuesta(2) }
        btnAmarillo.setOnClickListener { if (!testFinalizado) verificarRespuesta(3) }

        generarEstimuloStroop()
    }

    private fun generarEstimuloStroop() {
        if (testFinalizado) return
        if (rondaActual >= TOTAL_RONDAS) {
            finalizarPrueba()
            return
        }
        rondaActual++
        txtContador.text = "Intento $intentoActual | Ronda $rondaActual / $TOTAL_RONDAS"
        txtPalabra.text = palabras[Random.nextInt(0, 4)]
        colorCorrectoActual = Random.nextInt(0, 4)
        txtPalabra.setTextColor(coloresHex[colorCorrectoActual])
    }

    private fun verificarRespuesta(indiceSeleccionado: Int) {
        if (testFinalizado) return
        if (indiceSeleccionado == colorCorrectoActual) aciertos++
        Handler(Looper.getMainLooper()).postDelayed({ generarEstimuloStroop() }, 150)
    }

    private fun finalizarPrueba() {
        if (isFinishing || testFinalizado) return
        testFinalizado = true

        val notaBase = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()
        val notaFinal = (notaBase - penalizacionPorAusencia).coerceIn(0, 100)

        val details = mapOf("aciertos" to aciertos, "penaliz_ausencia" to penalizacionPorAusencia)
        CortexManager.logPerformanceMetric("t5", notaFinal, details)
        CortexManager.guardarPuntaje("t5", notaFinal)

        // ✅ Umbral 95% (igual que CortexManager)
        if (intentoActual == 1 && notaFinal < 95) {
            mostrarDialogoFin(notaFinal, esReintento = true)
        } else {
            mostrarDialogoFin(notaFinal, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(nota: Int, esReintento: Boolean) {
        if (isFinishing) return
        
        val titulo = when {
            esReintento -> "ATENCIÓN"
            nota >= 95 -> "¡EXCELENTE! 😎✅"
            else -> "ATENCIÓN"
        }
        
        val mensaje = when {
            esReintento -> "INTENTO REGISTRADO\n\nNota Final: $nota%\nPenalización por ausencia: -$penalizacionPorAusencia pts\n\nNecesitas 95% para saltarte el segundo intento."
            nota >= 95 -> "¡EXCELENTE!\n\nNota Final: $nota%\nPenalización por ausencia: -$penalizacionPorAusencia pts"
            else -> "MÓDULO FINALIZADO\n\nNota Final: $nota%\nPenalización por ausencia: -$penalizacionPorAusencia pts"
        }
        
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(if (esReintento) "INTENTO 2 →" else "➡️ SIGUIENTE") { _, _ ->
                if (esReintento) {
                    startActivity(Intent(this, AtencionTestActivity::class.java))
                    finish()
                }
                else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }

    override fun onTestPaused() {
        txtPalabra.text = "PAUSA"
        txtPalabra.setTextColor(Color.GRAY)
    }

    override fun onTestResumed() {
        if (!testFinalizado) generarEstimuloStroop()
    }
}