package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity

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

        btnReflejo.setOnClickListener { if (testEnProgreso && !testFinalizado) procesarClic() }

        handler.postDelayed({ if (!isFinishing && !testFinalizado) iniciarTest() }, 1000)

        // --- ✅ CONECTAR CON SENTINEL DE LA BASE ---
        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, txtFeedback)
    }

    private fun iniciarTest() {
        botonActivo = false
        testEnProgreso = true

        btnReflejo.setCardBackgroundColor(Color.parseColor("#334155"))
        txtEstado.text = "ESPERE"
        txtEstado.setTextColor(Color.WHITE)
        txtFeedback.visibility = TextView.GONE

        val tiempoEspera = (2000 + (Math.random() * 3000)).toLong()
        esperaRunnable = Runnable { if (!isFinishing && testEnProgreso && !testFinalizado) activarBoton() }
        handler.postDelayed(esperaRunnable!!, tiempoEspera)
    }

    private fun activarBoton() {
        botonActivo = true
        tiempoInicio = System.currentTimeMillis()
        btnReflejo.setCardBackgroundColor(Color.parseColor("#10B981"))
        txtEstado.text = "YA!"
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
            puntaje = calcularPuntaje(tiempoReaccion)
            errorAnticipacion = false
        } else {
            tiempoReaccion = -1
            puntaje = 0
            errorAnticipacion = true
        }

        val details = mapOf("tiempo_reaccion_ms" to tiempoReaccion, "error_anticipacion" to errorAnticipacion)
        CortexManager.logPerformanceMetric("t1", puntaje, details)
        CortexManager.guardarPuntaje("t1", puntaje)

        if (eraPrimerIntento && puntaje < 80) {
            testFinalizado = true
            recreate()
        } else {
            testFinalizado = true
            mostrarResultado(puntaje, tiempoReaccion, errorAnticipacion)
        }
    }

    private fun calcularPuntaje(tiempoMs: Long): Int {
        return if (tiempoMs > 500) {
            maxOf(0, 100 - ((tiempoMs - 500) / 5).toInt())
        } else {
            100 - maxOf(0, ((tiempoMs - 200) / 5).toInt())
        }.coerceIn(0, 100)
    }

    private fun mostrarResultado(puntaje: Int, tiempoMs: Long, errorAnticipacion: Boolean) {
        val mensaje = when {
            errorAnticipacion -> "PRESIONASTE ANTES DE TIEMPO!\n\nDebes esperar a que el círculo se ponga VERDE.\n\nNota: 0%"
            tiempoMs < 200 -> "¡INCREÍBLE!\n\nTiempo: ${tiempoMs}ms\nReflejos de élite.\n\nNota: $puntaje%"
            tiempoMs < 400 -> "MUY BIEN\n\nTiempo: ${tiempoMs}ms\nBuen reflejo.\n\nNota: $puntaje%"
            else -> "LENTO\n\nTiempo: ${tiempoMs}ms\nPosible fatiga detectada.\n\nNota: $puntaje%"
        }

        AlertDialog.Builder(this)
            .setTitle("REFLEJOS")
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("CONTINUAR") { _, _ ->
                CortexManager.navegarAlSiguiente(this)
                finish()
            }
            .show()
    }

    // --- ✅ LÓGICA DE PAUSA/REANUDACIÓN POR SENTINEL ---
    override fun onTestPaused() {
        // Si el usuario se va, detenemos el cronómetro de espera
        esperaRunnable?.let { handler.removeCallbacks(it) }
        testEnProgreso = false
        txtEstado.text = "PAUSA"
    }

    override fun onTestResumed() {
        // Al volver, reiniciamos el ciclo de espera
        if (!testFinalizado) {
            iniciarTest()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esperaRunnable?.let { handler.removeCallbacks(it) }
    }
}