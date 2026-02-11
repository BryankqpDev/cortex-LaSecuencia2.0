package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.random.Random

class SecuenciaTestActivity : TestBaseActivity() {

    private lateinit var botones: List<View>
    private lateinit var txtInstruccion: TextView

    private val secuenciaGenerada = mutableListOf<Int>()
    private val secuenciaUsuario = mutableListOf<Int>()
    private val LONGITUD_SECUENCIA = 6
    private var esTurnoDelUsuario = false
    private var intentoActual = 1

    private lateinit var sessionParams: TestSessionParams.SecuenciaParams

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

        intentoActual = CortexManager.obtenerIntentoActual("t2")

        sessionParams = TestSessionParams.generarSecuenciaParams()
        TestSessionParams.registrarParametros("t2", sessionParams)

        configurarClicks()

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinderSeq)
        configurarSentinel(viewFinder, null)

        Handler(Looper.getMainLooper()).postDelayed({ if (!testFinalizado) prepararSecuenciaUnica() }, 1000)
    }

    private fun configurarClicks() {
        botones.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (esTurnoDelUsuario && !testFinalizado && !estaEnPausaPorAusencia) {
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

        for (i in 0 until LONGITUD_SECUENCIA) {
            secuenciaGenerada.add(Random.nextInt(0, 9))
        }

        txtInstruccion.text = "MEMORIZA LA SECUENCIA"
        txtInstruccion.setTextColor(Color.CYAN)

        mostrarSecuenciaCompleta()
    }

    private fun mostrarSecuenciaCompleta() {
        val handler = Handler(Looper.getMainLooper())
        val tiempoPaso = sessionParams.tiempoPorPasoMs

        secuenciaGenerada.forEachIndexed { i, botonIndex ->
            handler.postDelayed({
                if (!testFinalizado && !isFinishing) iluminarBoton(botonIndex, false)

                if (i == secuenciaGenerada.size - 1) {
                    handler.postDelayed({
                        if (!testFinalizado && !isFinishing) {
                            esTurnoDelUsuario = true
                            txtInstruccion.text = "REPITE LA SECUENCIA"
                            txtInstruccion.setTextColor(Color.WHITE)
                        }
                    }, sessionParams.tiempoPreRespuestaMs)
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

        if (secuenciaUsuario[indexActual] != secuenciaGenerada[indexActual]) {
            gestionarFallo()
            return
        }

        if (secuenciaUsuario.size == LONGITUD_SECUENCIA) {
            finalizarConExito()
        }
    }

    private fun gestionarFallo() {
        testFinalizado = true
        esTurnoDelUsuario = false

        val aciertos = (secuenciaUsuario.size - 1).coerceAtLeast(0)
        val puntajeBase = (aciertos.toFloat() / LONGITUD_SECUENCIA * 100).toInt()
        val puntajeFinal = (puntajeBase - penalizacionPorAusencia).coerceAtLeast(0)

        CortexManager.guardarPuntaje("t2", puntajeFinal)

        if (intentoActual == 1 && puntajeFinal < 95) {
            mostrarDialogoReintento(puntajeFinal)
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

    private fun mostrarDialogoReintento(puntaje: Int) {
        AlertDialog.Builder(this)
            .setTitle("MEMORIA")
            .setMessage("INTENTO REGISTRADO\n\nNota: $puntaje%\n\nNecesitas 95% para saltarte el segundo intento.")
            .setCancelable(false)
            .setPositiveButton("INTENTO 2 →") { _, _ ->
                reiniciarTest()
            }
            .show()
    }
    
    private fun reiniciarTest() {
        testFinalizado = false
        secuenciaGenerada.clear()
        secuenciaUsuario.clear()
        esTurnoDelUsuario = false
        penalizacionPorAusencia = 0
        intentoActual = CortexManager.obtenerIntentoActual("t2")
        
        sessionParams = TestSessionParams.generarSecuenciaParams()
        TestSessionParams.registrarParametros("t2", sessionParams)
        
        Handler(Looper.getMainLooper()).postDelayed({ 
            if (!testFinalizado) prepararSecuenciaUnica() 
        }, 500)
    }

    private fun mostrarDialogoFinal(puntaje: Int, mensaje: String) {
        if (isFinishing) return
        val titulo = if (puntaje >= 95) "¡EXCELENTE! 😎✅" else "MEMORIA"
        val resultado = if (puntaje >= 95) "¡EXCELENTE!" else "MÓDULO FINALIZADO"
        
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage("$resultado\n\n$mensaje\nNota Final: $puntaje%\nPenalización ausencia: -$penalizacionPorAusencia pts")
            .setCancelable(false)
            .setPositiveButton("➡️ SIGUIENTE") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    override fun onTestPaused() {
        esTurnoDelUsuario = false
        txtInstruccion.text = "PAUSA POR AUSENCIA"
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            esTurnoDelUsuario = true
            txtInstruccion.text = "CONTINUAR"
        }
    }
}
