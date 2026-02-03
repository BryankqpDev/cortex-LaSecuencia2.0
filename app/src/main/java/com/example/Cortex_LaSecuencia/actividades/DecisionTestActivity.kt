package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class DecisionTestActivity : TestBaseActivity() {

    private lateinit var btnOpcion1: Button
    private lateinit var btnOpcion2: Button
    private lateinit var indicadorRegla: View

    private var rondaActual = 0
    private val TOTAL_RONDAS = 8
    private var aciertos = 0
    private var esReglaMayor = true
    private var valor1 = 0
    private var valor2 = 0
    private var intentoActual = 1

    override fun obtenerTestId(): String = "t10"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decision_test)

        btnOpcion1 = findViewById(R.id.btn_opcion_1)
        btnOpcion2 = findViewById(R.id.btn_opcion_2)
        indicadorRegla = findViewById(R.id.indicador_regla)

        intentoActual = CortexManager.obtenerIntentoActual("t10")

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        btnOpcion1.setOnClickListener { if(!estaEnPausaPorAusencia) verificarRespuesta(valor1) }
        btnOpcion2.setOnClickListener { if(!estaEnPausaPorAusencia) verificarRespuesta(valor2) }

        siguienteRonda()
    }

    private fun siguienteRonda() {
        if (testFinalizado || isFinishing) return
        if (rondaActual >= TOTAL_RONDAS) {
            finalizarTest()
            return
        }
        rondaActual++

        esReglaMayor = Random.nextBoolean()
        val colorRegra = if (esReglaMayor) Color.parseColor("#3B82F6") else Color.parseColor("#F59E0B")
        
        // Aplicar color a los casilleros (botones) para que sea más claro
        btnOpcion1.backgroundTintList = android.content.res.ColorStateList.valueOf(colorRegra)
        btnOpcion2.backgroundTintList = android.content.res.ColorStateList.valueOf(colorRegra)
        indicadorRegla.setBackgroundColor(colorRegra)

        valor1 = Random.nextInt(1, 100)
        valor2 = Random.nextInt(1, 100)
        while (valor1 == valor2) valor2 = Random.nextInt(1, 100)

        btnOpcion1.text = valor1.toString()
        btnOpcion2.text = valor2.toString()
    }

    private fun verificarRespuesta(seleccion: Int) {
        if (testFinalizado || estaEnPausaPorAusencia) return
        val correcto = if (esReglaMayor) seleccion == maxOf(valor1, valor2) else seleccion == minOf(valor1, valor2)
        if (correcto) aciertos++
        siguienteRonda()
    }

    private fun finalizarTest() {
        testFinalizado = true
        val notaBase = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()
        val notaFinal = (notaBase - penalizacionPorAusencia).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t10", notaFinal)

        // ✅ Umbral 95% (igual que CortexManager)
        if (intentoActual == 1 && notaFinal < 95) {
            mostrarDialogoReintento(notaFinal, aciertos)
        } else {
            AlertDialog.Builder(this)
                .setTitle("EVALUACIÓN FINALIZADA")
                .setMessage("Aciertos: $aciertos/$TOTAL_RONDAS\nNota Final: $notaFinal%\nPenalización ausencia: -$penalizacionPorAusencia pts")
                .setCancelable(false)
                .setPositiveButton("VER REPORTE") { _, _ ->
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
                .show()
        }
    }

    private fun mostrarDialogoReintento(nota: Int, aciertos: Int) {
        AlertDialog.Builder(this)
            .setTitle("DECISIÓN - INTENTO 1")
            .setMessage("Aciertos: $aciertos/$TOTAL_RONDAS\nNota Final: $nota%\nPenalización ausencia: -$penalizacionPorAusencia pts\n\n⚠️ Tendrás un segundo intento.")
            .setCancelable(false)
            .setPositiveButton("INTENTO 2 →") { _, _ ->
                startActivity(Intent(this, DecisionTestActivity::class.java))
                finish()
            }
            .show()
    }

    override fun onTestPaused() {
        btnOpcion1.visibility = View.INVISIBLE
        btnOpcion2.visibility = View.INVISIBLE
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            btnOpcion1.visibility = View.VISIBLE
            btnOpcion2.visibility = View.VISIBLE
        }
    }
}
