package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
    private val TOTAL_RONDAS = 10
    private var aciertos = 0
    private var intentoActual = 1

    override fun obtenerTestId(): String = "t9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espacial_test)

        imgFlecha = findViewById(R.id.img_flecha)
        txtInstruccion = findViewById(R.id.txt_instruccion)
        intentoActual = CortexManager.obtenerIntentoActual("t9")

        // Asegurar visibilidad inicial
        imgFlecha.alpha = 1.0f
        imgFlecha.visibility = View.VISIBLE

        val viewFinder = findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder)
        configurarSentinel(viewFinder, txtInstruccion)

        // Configurar botones direccionales
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

        // Iniciar primera ronda
        siguienteRonda()
    }

    private fun siguienteRonda() {
        if (testFinalizado || isFinishing) return

        if (rondaActual >= TOTAL_RONDAS) {
            finalizarTest()
            return
        }

        rondaActual++

        // Determinar color aleatoriamente
        esAzul = Random.nextBoolean()

        // Elegir dirección aleatoria
        // 0° = ARRIBA, 90° = DERECHA, 180° = ABAJO, 270° = IZQUIERDA
        direccionCorrecta = listOf(0, 90, 180, 270).random()

        // Actualizar instrucción con progreso
        actualizarInstruccion()

        // Rotar la flecha a la dirección elegida
        imgFlecha.rotation = direccionCorrecta.toFloat()

        // Aplicar color según la regla:
        // AZUL = responder hacia donde apunta la flecha
        // ROJA = responder OPUESTO a donde apunta la flecha
        imgFlecha.setColorFilter(
            if (esAzul) Color.parseColor("#3B82F6") // Azul brillante
            else Color.parseColor("#EF4444") // Rojo brillante
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

    private fun verificarRespuesta(direccionUsuario: Int) {
        if (testFinalizado || estaEnPausaPorAusencia) return

        // Calcular la dirección esperada según el color
        val direccionEsperada = if (esAzul) {
            // Si es AZUL, responder hacia donde apunta la flecha
            direccionCorrecta
        } else {
            // Si es ROJA, responder al lado OPUESTO (180 grados)
            (direccionCorrecta + 180) % 360
        }

        // Verificar si el usuario acertó
        if (direccionUsuario == direccionEsperada) {
            aciertos++
            // Feedback visual rápido
            imgFlecha.alpha = 0.5f
        }

        // Pequeño delay antes de la siguiente ronda para feedback visual
        imgFlecha.postDelayed({
            siguienteRonda()
        }, 300)
    }

    private fun finalizarTest() {
        testFinalizado = true

        val notaBase = (aciertos.toFloat() / TOTAL_RONDAS * 100).toInt()
        val notaFinal = (notaBase - penalizacionPorAusencia).coerceIn(0, 100)

        CortexManager.guardarPuntaje("t9", notaFinal)

        // ✅ Umbral 95% (igual que CortexManager)
        val aprobado = notaFinal >= 95

        // Si es el primer intento y no alcanzó 95%, permitir reintento
        if (intentoActual == 1 && !aprobado) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ INTENTO 1 - NO APROBADO")
                .setMessage(
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "Aciertos: $aciertos/$TOTAL_RONDAS\n" +
                            "Nota Base: $notaBase%\n" +
                            "Penalización: -$penalizacionPorAusencia pts\n" +
                            "Nota Final: $notaFinal%\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                            "RECUERDA:\n" +
                            "🟦 AZUL → Presiona donde apunta\n" +
                            "🟥 ROJA → Presiona dirección opuesta\n\n" +
                            "Necesitas 95% para aprobar."
                )
                .setCancelable(false)
                .setPositiveButton("INTENTO 2 →") { _, _ ->
                    startActivity(Intent(this, EspacialTestActivity::class.java))
                    finish()
                }
                .show()
        } else {
            // Segundo intento o aprobado
            val emoji = if (aprobado) "✅" else "❌"
            val estado = if (aprobado) "APROBADO" else "NO APROBADO"

            AlertDialog.Builder(this)
                .setTitle("$emoji ORIENTACIÓN ESPACIAL")
                .setMessage(
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "RESULTADO: $estado\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                            "Aciertos: $aciertos/$TOTAL_RONDAS\n" +
                            "Nota Base: $notaBase%\n" +
                            "Penalización: -$penalizacionPorAusencia pts\n" +
                            "Nota Final: $notaFinal%"
                )
                .setCancelable(false)
                .setPositiveButton("➡️ SIGUIENTE") { _, _ ->
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
                .show()
        }
    }

    override fun onTestPaused() {
        // Ocultar flecha cuando el usuario está ausente
        imgFlecha.visibility = View.INVISIBLE
    }

    override fun onTestResumed() {
        // Mostrar flecha cuando el usuario regresa
        if (!testFinalizado) {
            imgFlecha.visibility = View.VISIBLE
            imgFlecha.alpha = 1.0f
            actualizarInstruccion()
        }
    }
}