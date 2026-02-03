package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.doOnEnd
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.logic.AdaptiveScoring
import com.example.Cortex_LaSecuencia.utils.TestSessionParams
import com.example.Cortex_LaSecuencia.utils.TestBaseActivity
import kotlin.math.abs

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TEST DE ANTICIPACIÓN (t3) - VERSIÓN MEJORADA CON ACELERACIÓN VARIABLE
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Mejoras implementadas:
 * ✅ Velocidad base MÁS RÁPIDA (600-1400ms vs 900-2200ms)
 * ✅ Aceleración variable:
 *    - Tipo 0: Velocidad constante (lineal)
 *    - Tipo 1: Empieza LENTO → termina RÁPIDO
 *    - Tipo 2: Empieza RÁPIDO → termina LENTO
 * ✅ Multiplicador de velocidad 1.2x-1.8x
 * ✅ MUCHO más desafiante para trabajadores
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class AnticipacionTestActivity : TestBaseActivity() {

    private lateinit var vehiculo: ImageView
    private lateinit var zonaMeta: View
    private lateinit var btnFrenar: Button
    private lateinit var pista: View
    private lateinit var countdownText: TextView

    private var animador: ObjectAnimator? = null
    private var juegoActivo = false
    private var duracionAnimacionActual: Long = 0

    private var intentosRealizados = 0
    private val MAX_INTENTOS = 2

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PARÁMETROS ALEATORIOS DE ESTA SESIÓN (MEJORADOS)
    // ═══════════════════════════════════════════════════════════════════════
    private lateinit var sessionParams: TestSessionParams.AnticipacionParams

    override fun obtenerTestId(): String = "t3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)
        countdownText = findViewById(R.id.txt_countdown)

        // ═══════════════════════════════════════════════════════════════════
        // ✅ GENERAR PARÁMETROS ÚNICOS PARA ESTA EJECUCIÓN
        // ═══════════════════════════════════════════════════════════════════
        sessionParams = TestSessionParams.generarAnticipacionParams()
        TestSessionParams.registrarParametros("t3", sessionParams)

        btnFrenar.setOnClickListener { if (juegoActivo && !testFinalizado) frenarVehiculo() }

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, null)

        iniciarCuentaRegresiva()
    }

    private fun iniciarCuentaRegresiva() {
        if (testFinalizado) return
        countdownText.visibility = View.VISIBLE

        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000
                countdownText.text = if (segundos > 0) segundos.toString() else "¡YA!"
            }
            override fun onFinish() {
                countdownText.visibility = View.GONE
                programarInicioCarrera()
            }
        }.start()
    }

    private fun programarInicioCarrera() {
        if (testFinalizado) return
        vehiculo.translationX = 0f
        Handler(Looper.getMainLooper()).postDelayed({ iniciarCarrera() }, 500)
    }

    private fun iniciarCarrera() {
        if (isFinishing || testFinalizado) return
        juegoActivo = true
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()
        vehiculo.translationX = 0f

        // ═══════════════════════════════════════════════════════════════════
        // ✅ CALCULAR DURACIÓN CON VELOCIDAD BASE AUMENTADA
        // ═══════════════════════════════════════════════════════════════════
        val duracionBase = TestSessionParams.randomInRange(
            sessionParams.duracionAnimacionMinMs,
            sessionParams.duracionAnimacionMaxMs
        )

        // Aplicar factor de velocidad (más bajo = más rápido)
        // Ejemplo: duración 1000ms / factor 1.5 = 667ms (50% más rápido)
        duracionAnimacionActual = (duracionBase / sessionParams.factorVelocidadBase).toLong()

        // ═══════════════════════════════════════════════════════════════════
        // ✅ SELECCIONAR INTERPOLADOR SEGÚN TIPO DE ACELERACIÓN
        // ═══════════════════════════════════════════════════════════════════
        val interpolator: TimeInterpolator = when (sessionParams.tipoAceleracion) {
            1 -> AccelerateInterpolator(1.5f)  // Empieza lento → termina RÁPIDO
            2 -> DecelerateInterpolator(1.5f)  // Empieza RÁPIDO → termina lento
            else -> LinearInterpolator()        // Velocidad constante
        }

        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = duracionAnimacionActual
            setInterpolator(interpolator)
            doOnEnd { if (juegoActivo) evaluarFrenado(falloTotal = true) }
            start()
        }
    }

    private fun frenarVehiculo() {
        juegoActivo = false
        animador?.pause()
        evaluarFrenado(falloTotal = false)
    }

    private fun evaluarFrenado(falloTotal: Boolean) {
        if (testFinalizado) return
        intentosRealizados++

        var puntaje: Int
        var mensaje: String
        val diferenciaAbsoluta: Float

        if (falloTotal) {
            puntaje = 0
            mensaje = "¡NO FRENASTE! ❌"
            diferenciaAbsoluta = -1f
        } else {
            val centroVehiculo = vehiculo.x + vehiculo.width / 2
            val centroMeta = zonaMeta.x + zonaMeta.width / 2
            diferenciaAbsoluta = abs(centroVehiculo - centroMeta)

            // ═══════════════════════════════════════════════════════════════
            // ✅ USAR SCORING ADAPTATIVO
            // ═══════════════════════════════════════════════════════════════
            puntaje = AdaptiveScoring.calcularPuntajeAnticipacion(
                diferenciaAbsoluta,
                pista.width,
                sessionParams
            )

            // Aplicar penalización por ausencia
            puntaje = (puntaje - penalizacionPorAusencia).coerceIn(0, 100)

            // ═══════════════════════════════════════════════════════════════
            // ✅ MENSAJES AJUSTADOS PARA MAYOR DIFICULTAD
            // ═══════════════════════════════════════════════════════════════
            mensaje = when {
                puntaje >= 85 -> "¡PRECISIÓN PERFECTA! 😎\n¡Reflejos de élite!"
                puntaje >= 70 -> "BUEN CÁLCULO ✓\nReacción rápida."
                puntaje >= 50 -> "ACEPTABLE ⚠️\nPuedes mejorar."
                else -> "FRENADO IMPRECISO ❌\nRequiere más atención."
            }
        }

        val intentoActual = CortexManager.obtenerIntentoActual("t3")

        // ═══════════════════════════════════════════════════════════════════
        // ✅ LOG EXTENDIDO CON INFORMACIÓN DE ACELERACIÓN
        // ═══════════════════════════════════════════════════════════════════
        val tipoAceleracionTexto = when (sessionParams.tipoAceleracion) {
            1 -> "Aceleración (lento→rápido)"
            2 -> "Desaceleración (rápido→lento)"
            else -> "Velocidad constante"
        }

        val details = mapOf(
            "distancia_px" to diferenciaAbsoluta,
            "duracion_final_ms" to duracionAnimacionActual,
            "fallo_total" to falloTotal,
            "penaliz_ausencia" to penalizacionPorAusencia,
            "duracion_min_config" to sessionParams.duracionAnimacionMinMs,
            "duracion_max_config" to sessionParams.duracionAnimacionMaxMs,
            "factor_tolerancia" to sessionParams.factorTolerancia,
            "tipo_aceleracion" to tipoAceleracionTexto,
            "factor_velocidad_base" to sessionParams.factorVelocidadBase
        )
        CortexManager.logPerformanceMetric("t3", puntaje, details)
        CortexManager.guardarPuntaje("t3", puntaje)

        // ✅ Umbral 95% (igual que CortexManager)
        if (intentoActual == 1 && puntaje < 95) {
            testFinalizado = true
            mostrarDialogoFin(
                "INTENTO FALLIDO",
                "$mensaje\n\nNota: $puntaje%\n\nQueda 1 intento más.",
                true
            )
        } else {
            testFinalizado = true
            val titulo = if(puntaje >= 80) "PRUEBA SUPERADA ✅" else "TEST FINALIZADO"
            mostrarDialogoFin(titulo, "Resultado Final:\nNota: $puntaje%\n\n$mensaje", false)
        }
    }

    private fun mostrarDialogoFin(titulo: String, mensaje: String, esReintento: Boolean) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(if (esReintento) "REINTENTAR" else "SIGUIENTE") { _, _ ->
                if (esReintento) {
                    startActivity(Intent(this, AnticipacionTestActivity::class.java))
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
        animador?.pause()
        juegoActivo = false
    }

    override fun onTestResumed() {
        if (!testFinalizado) {
            animador?.resume()
            juegoActivo = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animador?.cancel()
    }
}