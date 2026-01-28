package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class SecuenciaTestActivity : TestBaseActivity() {

    private lateinit var botones: List<View>
    private lateinit var txtInstruccion: TextView

    private val secuenciaGenerada = mutableListOf<Int>()
    private val secuenciaUsuario = mutableListOf<Int>()
    private val LONGITUD_SECUENCIA = 6 
    private var esTurnoDelUsuario = false

    override fun obtenerTestId(): String = "t2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secuencia_test)

        txtInstruccion = findViewById(R.id.txt_instruccion_seq)
        botones = listOf(
            findViewById(R.id.btn_1), findViewById(R.id.btn_2), findViewById(R.id.btn_3),
            findViewById(R.id.btn_4), findViewById(R.id.btn_5), findViewById(R.id.btn_6),
            findViewById(R.id.btn_7), findViewById(R.id.btn_8), findViewById(R.id.btn_9)
        )

        configurarClicks()

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderSeq)
        configurarSentinel(viewFinder, null)

        // Iniciar el test único tras una pausa
        Handler(Looper.getMainLooper()).postDelayed({ if (!testFinalizado) prepararSecuenciaUnica() }, 1000)
    }

    private fun configurarClicks() {
        botones.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (esTurnoDelUsuario && !testFinalizado) {
                    iluminarBoton(index, true)
                    secuenciaUsuario.add(index)
                    verificarEntrada()
                }
            }
        }
    }

    private fun prepararSecuenciaUnica() {
        if (testFinalizado || isFinishing) return

        esTurnoDelUsuario = false
        secuenciaUsuario.clear()
        secuenciaGenerada.clear()

        // Generar 6 pasos aleatorios de una vez
        for (i in 0 until LONGITUD_SECUENCIA) {
            secuenciaGenerada.add(Random.nextInt(0, 9))
        }

        txtInstruccion.text = getString(R.string.t2_instruction_memorize)
        txtInstruccion.setTextColor(Color.CYAN)

        mostrarSecuenciaCompleta()
    }

    private fun mostrarSecuenciaCompleta() {
        val handler = Handler(Looper.getMainLooper())
        val tiempoPaso = 800L 

        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                if (!testFinalizado && !isFinishing) iluminarBoton(botonIndex, false)

                // Al llegar al último paso, dar el turno al usuario
                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        if (!testFinalizado && !isFinishing) {
                            esTurnoDelUsuario = true
                            txtInstruccion.text = getString(R.string.t2_instruction_replicate)
                            txtInstruccion.setTextColor(Color.WHITE)
                        }
                    }, tiempoPaso)
                }
            }, (i + 1) * tiempoPaso)
        }
    }

    private fun iluminarBoton(index: Int, esUsuario: Boolean) {
        val originalColor = Color.parseColor("#1E293B")
        val highlightColor = if (esUsuario) Color.parseColor("#F59E0B") else Color.parseColor("#00F0FF")

        botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(highlightColor)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) botones[index].backgroundTintList = android.content.res.ColorStateList.valueOf(originalColor)
        }, 500)
    }

    private fun verificarEntrada() {
        val indexActual = secuenciaUsuario.size - 1

        // Si se equivoca en cualquier paso
        if (secuenciaUsuario[indexActual] != secuenciaGenerada[indexActual]) {
            gestionarFallo()
            return
        }

        // Si completa los 6 pasos correctamente
        if (secuenciaUsuario.size == LONGITUD_SECUENCIA) {
            finalizarConExito()
        }
    }

    private fun gestionarFallo() {
        testFinalizado = true
        esTurnoDelUsuario = false
        
        // El puntaje es proporcional a cuántos pasos acertó antes de fallar
        val aciertos = (secuenciaUsuario.size - 1).coerceAtLeast(0)
        val puntajeBase = (aciertos.toFloat() / LONGITUD_SECUENCIA * 100).toInt()
        val puntajeFinal = (puntajeBase - penalizacionPorAusencia).coerceAtLeast(0)
        
        CortexManager.guardarPuntaje("t2", puntajeFinal)
        
        val intentoActual = CortexManager.obtenerIntentoActual("t2")
        if (intentoActual == 1 && puntajeFinal < 95) {
            recreate() // REINTENTO AUTOMÁTICO
        } else {
            mostrarDialogoFinal(puntajeFinal, "Secuencia incorrecta.")
        }
    }

    private fun finalizarConExito() {
        testFinalizado = true
        esTurnoDelUsuario = false
        
        val puntajeFinal = (100 - penalizacionPorAusencia).coerceAtLeast(0)
        CortexManager.guardarPuntaje("t2", puntajeFinal)

        mostrarDialogoFinal(puntajeFinal, "¡Excelente memoria!")
    }

    private fun mostrarDialogoFinal(puntaje: Int, mensaje: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("MEMORIA SECUENCIAL")
            .setMessage("$mensaje\nNota Final: $puntaje%\nPenalización ausencia: -$penalizacionPorAusencia")
            .setCancelable(false)
            .setPositiveButton("SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onTestPaused() {
        esTurnoDelUsuario = false
        txtInstruccion.text = getString(R.string.t2_instruction_pause)
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            esTurnoDelUsuario = true
            txtInstruccion.text = getString(R.string.t2_instruction_continue)
        }
    }
}