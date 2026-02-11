package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.logic.AdaptiveScoring
import com.example.Cortex_LaSecuencia.utils.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TEST DE REFLEJOS (t1) - VERSIÓN RANDOMIZADA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Cambios implementados:
 *  ✅ Delay aleatorio por ejecución (evita memorización)
 *  ✅ Umbrales de puntaje adaptativos (equidad según dificultad)
 *  ✅ Registro de parámetros para auditoría
 *  ✅ Mensajes mejorados con feedback del 95%
 *  ✅ Fix cámara oscura: TestBaseActivity ya tiene delay de 300ms para reintentos
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class ReflejosTestActivity : TestBaseActivity() {

    private lateinit var btnReflejo: CardView
    private lateinit var txtEstado: TextView
    private lateinit var txtIntento: TextView
    private lateinit var txtFeedback: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var tiempoInicio: Long = 0
    private var esperaRunnable: Runnable? = null
    private var botonActivo = false
    private var testEnProgreso = true

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PARÁMETROS ALEATORIOS DE ESTA SESIÓN
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var sessionParams: TestSessionParams.ReflejosParams

    override fun obtenerTestId(): String = "t1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reflejos_test)

        btnReflejo = findViewById(R.id.btn_reflejo)
        txtEstado = findViewById(R.id.txt_estado)
        txtIntento = findViewById(R.id.txt_intento)
        txtFeedback = findViewById(R.id.txt_feedback)

        val intentoActual = CortexManager.obtenerIntentoActual("t1")
        txtIntento.text = "INTENTO $intentoActual/2"

        // ═══════════════════════════════════════════════════════════════════
        // ✅ GENERAR PARÁMETROS ÚNICOS PARA ESTA EJECUCIÓN
        // ═══════════════════════════════════════════════════════════════════
        sessionParams = TestSessionParams.generarReflejosParams()
        TestSessionParams.registrarParametros("t1", sessionParams)

        btnReflejo.setOnClickListener { if (testEnProgreso && !testFinalizado) procesarClic() }

        handler.postDelayed({ if (!isFinishing && !testFinalizado) iniciarTest() }, 1000)

        // Conectar con SENTINEL de la base
        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, txtFeedback)
    }

    private fun iniciarTest() {
        botonActivo = false
        testEnProgreso = true

        btnReflejo.setCardBackgroundColor(Color.parseColor("#334155"))
        txtEstado.text = getString(R.string.status_wait)
        txtEstado.setTextColor(Color.WHITE)
        txtFeedback.visibility = TextView.GONE

        // ═══════════════════════════════════════════════════════════════════
        // ✅ USAR DELAY ALEATORIO GENERADO
        // ═══════════════════════════════════════════════════════════════════
        val tiempoEspera = TestSessionParams.randomInRange(
            sessionParams.delayMinMs,
            sessionParams.delayMaxMs
        )

        esperaRunnable = Runnable { if (!isFinishing && testEnProgreso && !testFinalizado) activarBoton() }
        handler.postDelayed(esperaRunnable!!, tiempoEspera)
    }

    private fun activarBoton() {
        botonActivo = true
        tiempoInicio = System.currentTimeMillis()
        btnReflejo.setCardBackgroundColor(Color.parseColor("#10B981"))
        txtEstado.text = getString(R.string.status_ready)
        txtEstado.setTextColor(Color.BLACK)
    }

    private fun procesarClic() {
        if (!testEnProgreso || testFinalizado) return

        esperaRunnable?.let { handler.removeCallbacks(it) }
        testEnProgreso = false

        val eraPrimerIntento = CortexManager.obtenerIntentoActual("t1") == 1
        val puntaje: Int
        val tiempoReaccion: Long
        val errorAnticipacion: Boolean

        if (botonActivo) {
            tiempoReaccion = System.currentTimeMillis() - tiempoInicio

            // ═══════════════════════════════════════════════════════════════
            // ✅ USAR SCORING ADAPTATIVO
            // ═══════════════════════════════════════════════════════════════
            puntaje = AdaptiveScoring.calcularPuntajeReflejos(tiempoReaccion, sessionParams)
            errorAnticipacion = false
        } else {
            tiempoReaccion = -1
            puntaje = 0
            errorAnticipacion = true
        }

        val details = mapOf(
            "tiempo_reaccion_ms" to tiempoReaccion,
            "error_anticipacion" to errorAnticipacion,
            "delay_usado_ms" to sessionParams.delayMinMs,
            "umbral_elite_usado" to sessionParams.umbralEliteMs,
            "umbral_penalizacion_usado" to sessionParams.umbralPenalizacionMs
        )
        CortexManager.logPerformanceMetric("t1", puntaje, details)
        CortexManager.guardarPuntaje("t1", puntaje)

        // ✅ Umbral 95% (igual que CortexManager)
        if (eraPrimerIntento && puntaje < 95) {
            testFinalizado = true
            mostrarResultadoConReintento(puntaje, tiempoReaccion, errorAnticipacion)
        } else {
            testFinalizado = true
            mostrarResultado(puntaje, tiempoReaccion, errorAnticipacion)
        }
    }

    private fun mostrarResultadoConReintento(puntaje: Int, tiempoMs: Long, errorAnticipacion: Boolean) {
        val mensaje = when {
            errorAnticipacion -> "PRESIONASTE ANTES DE TIEMPO!\n\nDebes esperar a que el círculo se ponga VERDE.\n\nNota: 0%\n\nNecesitas 95% para saltarte el segundo intento."
            tiempoMs < sessionParams.umbralEliteMs -> "INTENTO REGISTRADO\n\nTiempo: ${tiempoMs}ms\nNota: $puntaje%\n\nNecesitas 95% para saltarte el segundo intento."
            else -> "INTENTO REGISTRADO\n\nTiempo: ${tiempoMs}ms\nNota: $puntaje%\n\nNecesitas 95% para saltarte el segundo intento."
        }

        AlertDialog.Builder(this)
            .setTitle("REFLEJOS")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("INTENTO 2 →") { _, _ ->
                // ✅ El onPause() automático liberará la cámara
                // ✅ El onCreate() del reintento esperará 300ms (ya implementado en TestBaseActivity)
                startActivity(Intent(this, ReflejosTestActivity::class.java))
                finish()
            }
            .show()
    }
    
    private fun reiniciarTest() {
        testFinalizado = false
        fueInterrumpido = false
        testEnProgreso = false
        botonActivo = false
        tiempoInicio = 0L
        
        sessionParams = TestSessionParams.generarReflejosParams()
        TestSessionParams.registrarParametros("t1", sessionParams)
        
        handler.postDelayed({ if (!isFinishing && !testFinalizado) iniciarTest() }, 500)
    }

    private fun mostrarResultado(puntaje: Int, tiempoMs: Long, errorAnticipacion: Boolean) {
        val titulo = if (puntaje >= 95) "¡EXCELENTE! 😎✅" else "REFLEJOS"
        val mensaje = when {
            errorAnticipacion -> "PRESIONASTE ANTES DE TIEMPO!\n\nDebes esperar a que el círculo se ponga VERDE.\n\nNota: 0%"
            puntaje >= 95 -> "¡EXCELENTE!\n\nTiempo: ${tiempoMs}ms\nReflejos de élite.\n\nNota: $puntaje%"
            tiempoMs < 400 -> "MÓDULO FINALIZADO\n\nTiempo: ${tiempoMs}ms\nBuen reflejo.\n\nNota: $puntaje%"
            else -> "MÓDULO FINALIZADO\n\nTiempo: ${tiempoMs}ms\nPosible fatiga detectada.\n\nNota: $puntaje%"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    // Lógica de pausa/reanudación por SENTINEL
    override fun onTestPaused() {
        esperaRunnable?.let { handler.removeCallbacks(it) }
        testEnProgreso = false
        txtEstado.text = getString(R.string.status_pause)
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            iniciarTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esperaRunnable?.let { handler.removeCallbacks(it) }
    }
}