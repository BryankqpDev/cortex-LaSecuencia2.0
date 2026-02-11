package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class EspacialTestActivity : TestBaseActivity() {

    private lateinit var imgFlecha: ImageView
    private lateinit var txtInstruccion: TextView
    private var direccionCorrecta = 0
    private var esAzul = true
    private var rondaActual = 0
    private val TOTAL_RONDAS = 4
    private var aciertos = 0
    private var intentoActual = 1

    override fun obtenerTestId(): String = "t9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espacial_test)

        imgFlecha = findViewById(R.id.img_flecha)
        txtInstruccion = findViewById(R.id.txt_instruccion)
        intentoActual = CortexManager.obtenerIntentoActual("t9")

        imgFlecha.alpha = 1.0f
        imgFlecha.visibility = View.VISIBLE

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, txtInstruccion)

        findViewById<ImageButton>(R.id.btn_up).setOnClickListener {
            if(!estaEnPausaPorAusencia && !testFinalizado) verificarRespuesta(0)
        }
        findViewById<ImageButton>(R.id.btn_right).setOnClickListener {
            if(!estaEnPausaPorAusencia && !testFinalizado) verificarRespuesta(90)
        }
        findViewById<ImageButton>(R.id.btn_down).setOnClickListener {
            if(!estaEnPausaPorAusencia && !testFinalizado) verificarRespuesta(180)
        }
        findViewById<ImageButton>(R.id.btn_left).setOnClickListener {
            if(!estaEnPausaPorAusencia && !testFinalizado) verificarRespuesta(270)
        }

        siguienteRonda()
    }

    private fun siguienteRonda() {
        if (testFinalizado || isFinishing) return

        if (rondaActual >= TOTAL_RONDAS) {
            finalizarTest()
            return
        }

        rondaActual++
        esAzul = Random.nextBoolean()
        direccionCorrecta = listOf(0, 90, 180, 270).random()

        actualizarInstruccion()

        imgFlecha.rotation = direccionCorrecta.toFloat()
        imgFlecha.setColorFilter(
            if (esAzul) Color.parseColor("#3B82F6") else Color.parseColor("#EF4444")
        )

        imgFlecha.alpha = 1.0f
        imgFlecha.visibility = View.VISIBLE
    }

    private fun actualizarInstruccion() {
        if (!estaEnPausaPorAusencia) {
            val colorEmoji = if (esAzul) "🟦" else "🟥"
            val colorTexto = if (esAzul) "AZUL" else "ROJA"
            val accion = if (esAzul) "Dirección Real" else "Dirección CONTRARIA"

            txtInstruccion.text = "RONDA $rondaActual/$TOTAL_RONDAS\n$colorEmoji FLECHA $colorTexto: $accion"
            txtInstruccion.setTextColor(Color.WHITE)
        }
    }

    private fun reiniciarTest() {
        testFinalizado = false
        rondaActual = 0
        aciertos = 0
        penalizacionPorAusencia = 0
        intentoActual = CortexManager.obtenerIntentoActual("t9")
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (!testFinalizado) siguienteRonda()
        }, 500)
    }
    
    private fun verificarRespuesta(direccionUsuario: Int) {
        if (testFinalizado || estaEnPausaPorAusencia) return

        val direccionEsperada = if (esAzul) direccionCorrecta else (direccionCorrecta + 180) % 360

        if (direccionUsuario == direccionEsperada) {
            aciertos++
            imgFlecha.alpha = 0.5f
        }

        imgFlecha.postDelayed({
            siguienteRonda()
        }, 300)
    }

    private fun finalizarTest() {
        testFinalizado = true
        val notaBase = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()
        val notaFinal = (notaBase - penalizacionPorAusencia).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t9", notaFinal)

        if (intentoActual == 1 && notaFinal < 95) {
            AlertDialog.Builder(this)
                .setTitle("ORIENTACIÓN ESPACIAL")
                .setMessage("INTENTO REGISTRADO\n\nAciertos: $aciertos/$TOTAL_RONDAS\nNota Final: $notaFinal%\n\nNecesitas 95% para saltarte el segundo intento.")
                .setCancelable(false)
                .setPositiveButton("INTENTO 2 →") { _, _ ->
                    reiniciarTest()
                }
                .show()
        } else {
            val titulo = if (notaFinal >= 95) "¡EXCELENTE! 😎✅" else "ORIENTACIÓN ESPACIAL"
            AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage("MÓDULO FINALIZADO\n\nAciertos: $aciertos/$TOTAL_RONDAS\nNota Final: $notaFinal%\nPenalización ausencia: -$penalizacionPorAusencia pts")
                .setCancelable(false)
                .setPositiveButton("➡️ SIGUIENTE") { _, _ ->
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
                .show()
        }
    }

    override fun onTestPaused() {
        imgFlecha.visibility = View.INVISIBLE
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            imgFlecha.visibility = View.VISIBLE
            imgFlecha.alpha = 1.0f
            actualizarInstruccion()
        }
    }
}
