package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import java.util.Random

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TEST DE CONTROL DE IMPULSO (t7) - VERSIÓN RANDOMIZADA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Cambios implementados:
 * ✅ Duración del estímulo variable (evita memorización)
 * ✅ Delay entre rondas aleatorio
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class ImpulsoTestActivity : TestBaseActivity() {

    private lateinit var cardEstimulo: CardView
    private lateinit var txtIcono: TextView
    private lateinit var txtFeedback: TextView
    private lateinit var layoutRoot: View

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random()

    private var rondaActual = 0
    private val TOTAL_RONDAS = 5
    private var erroresImpulso = 0
    private var erroresOmision = 0
    private val tiemposDeReaccionGo = mutableListOf<Long>()

    private var esAzul = false
    private var esperandoRespuesta = false
    private var respondioEnEstaRonda = false
    private var tiempoInicioEstimulo: Long = 0

    private var runnableRonda: Runnable? = null

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PARÁMETROS ALEATORIOS DE ESTA SESIÓN
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var sessionParams: TestSessionParams.ImpulsoParams

    override fun obtenerTestId(): String = "t7"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impulso_test)

        cardEstimulo = findViewById(R.id.card_estimulo)
        txtIcono = findViewById(R.id.txt_icono_central)
        txtFeedback = findViewById(R.id.txt_feedback)
        layoutRoot = findViewById(R.id.layout_root)

        // ═══════════════════════════════════════════════════════════════════
        // ✅ GENERAR PARÁMETROS ÚNICOS PARA ESTA EJECUCIÓN
        // ═══════════════════════════════════════════════════════════════════
        sessionParams = TestSessionParams.generarImpulsoParams()
        TestSessionParams.registrarParametros("t7", sessionParams)

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        layoutRoot.setOnClickListener { if (!testFinalizado && !estaEnPausaPorAusencia) procesarToque() }
        cardEstimulo.setOnClickListener { if (!testFinalizado && !estaEnPausaPorAusencia) procesarToque() }

        programarSiguiente(1000)
    }

    private fun programarSiguiente(delay: Long) {
        runnableRonda?.let { handler.removeCallbacks(it) }
        runnableRonda = Runnable { if (!testFinalizado && !estaEnPausaPorAusencia) siguienteEstimulo() }
        handler.postDelayed(runnableRonda!!, delay)
    }

    private fun siguienteEstimulo() {
        if (testFinalizado || isFinishing) return
        if (rondaActual >= TOTAL_RONDAS) {
            finalizarPrueba()
            return
        }

        rondaActual++
        esperandoRespuesta = true
        respondioEnEstaRonda = false
        esAzul = random.nextFloat() > 0.3

        if (esAzul) {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#3B82F6"))
            txtIcono.text = "🔵"
        } else {
            cardEstimulo.setCardBackgroundColor(Color.parseColor("#F59E0B"))
            txtIcono.text = "✖️"
        }

        tiempoInicioEstimulo = System.currentTimeMillis()
        txtFeedback.text = "..."

        // ═══════════════════════════════════════════════════════════════════
        // ✅ USAR DURACIÓN DE ESTÍMULO ALEATORIA
        // ═══════════════════════════════════════════════════════════════════
        // Antes: 900ms fijo (memorizable)
        // Ahora: Variable por sesión
        runnableRonda = Runnable { if (!testFinalizado && !estaEnPausaPorAusencia) evaluarFinDeRonda() }
        handler.postDelayed(runnableRonda!!, sessionParams.duracionEstimuloMs)
    }

    private fun procesarToque() {
        if (!esperandoRespuesta || respondioEnEstaRonda || testFinalizado) return
        respondioEnEstaRonda = true

        if (esAzul) {
            val tiempoReaccion = System.currentTimeMillis() - tiempoInicioEstimulo
            tiemposDeReaccionGo.add(tiempoReaccion)
            txtFeedback.text = "¡BIEN! (${tiempoReaccion}ms)"
            txtFeedback.setTextColor(Color.GREEN)
        } else {
            erroresImpulso++
            txtFeedback.text = "¡ERROR! NO TOQUES"
            txtFeedback.setTextColor(Color.RED)
        }
    }

    private fun evaluarFinDeRonda() {
        if (!respondioEnEstaRonda && esAzul) {
            erroresOmision++
            txtFeedback.text = "¡TARDAS!"
            txtFeedback.setTextColor(Color.LTGRAY)
        }

        cardEstimulo.setCardBackgroundColor(Color.parseColor("#1E293B"))
        txtIcono.text = ""
        esperandoRespuesta = false

        // ═══════════════════════════════════════════════════════════════════
        // ✅ USAR DELAY ENTRE RONDAS ALEATORIO
        // ═══════════════════════════════════════════════════════════════════
        programarSiguiente(sessionParams.delayEntreRondasMs)
    }

    private fun finalizarPrueba() {
        if (isFinishing || testFinalizado) return
        testFinalizado = true
        handler.removeCallbacksAndMessages(null)

        val penalizacionTotal = (erroresImpulso * 20) + (erroresOmision * 10) + penalizacionPorAusencia
        val notaFinal = (100 - penalizacionTotal).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t7", notaFinal)

        val details = mapOf(
            "tiempos_reaccion_ms" to tiemposDeReaccionGo,
            "errores_impulso" to erroresImpulso,
            "errores_omision" to erroresOmision,
            "penaliz_ausencia" to penalizacionPorAusencia,
            "duracion_estimulo_config" to sessionParams.duracionEstimuloMs,
            "delay_entre_rondas_config" to sessionParams.delayEntreRondasMs
        )
        CortexManager.logPerformanceMetric("t7", notaFinal, details)
        CortexManager.guardarPuntaje("t7", notaFinal)

        // ✅ Umbral 95% (igual que CortexManager)
        if (CortexManager.obtenerIntentoActual("t7") == 1 && notaFinal < 95) {
            mostrarDialogoReintento(notaFinal)
        } else {
            val titulo = if (notaFinal >= 95) "¡EXCELENTE! 😎✅" else "IMPULSO"
            val resultado = if (notaFinal >= 95) "¡EXCELENTE!" else "MÓDULO FINALIZADO"
            
            AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage("$resultado\n\nNota Final: $notaFinal%\nPenalización ausencia: -$penalizacionPorAusencia pts")
                .setCancelable(false)
                .setPositiveButton("➡️ SIGUIENTE") { _, _ ->
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
                .show()
        }
    }

    private fun mostrarDialogoReintento(puntaje: Int) {
        AlertDialog.Builder(this)
            .setTitle("IMPULSO")
            .setMessage("INTENTO REGISTRADO\n\nNota Final: $puntaje%\nPenalización ausencia: -$penalizacionPorAusencia pts\n\nNecesitas 95% para saltarte el segundo intento.")
            .setCancelable(false)
            .setPositiveButton("INTENTO 2 →") { _, _ ->
                startActivity(Intent(this, ImpulsoTestActivity::class.java))
                finish()
            }
            .show()
    }

    override fun onTestPaused() {
        runnableRonda?.let { handler.removeCallbacks(it) }
        txtFeedback.text = "PAUSA POR AUSENCIA"
    }

    override fun onTestResumed() {
        txtFeedback.text = "REANUDANDO..."
        programarSiguiente(1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}