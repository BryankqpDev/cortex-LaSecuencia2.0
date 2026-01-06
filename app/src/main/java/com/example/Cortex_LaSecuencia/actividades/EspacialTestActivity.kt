package com.example.Cortex_LaSecuencia.actividades

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.random.Random

class EspacialTestActivity : AppCompatActivity() {

    private lateinit var txtIntento: TextView
    private lateinit var txtInstruccion: TextView
    private lateinit var txtFlecha: TextView
    private lateinit var btnArriba: Button
    private lateinit var btnAbajo: Button
    private lateinit var btnIzquierda: Button
    private lateinit var btnDerecha: Button

    private var trials = 0
    private var score = 0
    private val MAX_TRIALS = 5

    // Direcciones: 0=Arriba, 90=Derecha, 180=Abajo, 270=Izquierda
    private var correctDir = 0
    private var isBlue = true
    private var intentoActual = 1
    private var botonesBloqueados = false // Evita doble click rápido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_espacial_test)

        txtIntento = findViewById(R.id.lbl_t9)
        txtInstruccion = findViewById(R.id.t9_instruccion)
        txtFlecha = findViewById(R.id.t9_arrow)

        btnArriba = findViewById(R.id.btn_dir_up)
        btnAbajo = findViewById(R.id.btn_dir_down)
        btnIzquierda = findViewById(R.id.btn_dir_left)
        btnDerecha = findViewById(R.id.btn_dir_right)

        // Obtener intento y mostrarlo
        intentoActual = CortexManager.obtenerIntentoActual("t9")
        txtIntento.text = "INTENTO $intentoActual/2"

        btnArriba.setOnClickListener { clickDirection(0) }
        btnDerecha.setOnClickListener { clickDirection(90) }
        btnAbajo.setOnClickListener { clickDirection(180) }
        btnIzquierda.setOnClickListener { clickDirection(270) }

        // Pequeña pausa antes de empezar
        Handler(Looper.getMainLooper()).postDelayed({ siguienteTrial() }, 500)
    }

    private fun siguienteTrial() {
        if (trials >= MAX_TRIALS) {
            finalizarTest()
            return
        }

        botonesBloqueados = false
        val dirs = listOf(0, 90, 180, 270)
        val arrowDir = dirs[Random.nextInt(dirs.size)]
        isBlue = Random.nextBoolean() // True = Azul, False = Rojo

        // LÓGICA ESPACIAL:
        // Azul (isBlue) -> La dirección correcta es la misma de la flecha.
        // Rojo (!isBlue) -> La dirección correcta es la OPUESTA (+180 grados).
        correctDir = if (isBlue) {
            arrowDir
        } else {
            (arrowDir + 180) % 360
        }

        // Mostrar Flecha
        txtFlecha.text = when (arrowDir) {
            0 -> "↑"
            90 -> "→"
            180 -> "↓"
            270 -> "←"
            else -> "↑"
        }

        // Color de la flecha
        val colorFlecha = if (isBlue) Color.parseColor("#3B82F6") else Color.parseColor("#EF4444")
        txtFlecha.setTextColor(colorFlecha)

        // Instrucción visual rápida
        txtInstruccion.text = if (isBlue) "SIGUE LA FLECHA" else "¡INVIERTE!"
        txtInstruccion.setTextColor(Color.LTGRAY)
    }

    private fun clickDirection(dir: Int) {
        if (botonesBloqueados) return
        botonesBloqueados = true // Bloqueamos hasta el siguiente trial

        val esCorrecto = (dir == correctDir)
        if (esCorrecto) {
            score++
            txtInstruccion.text = "¡BIEN!"
            txtInstruccion.setTextColor(Color.GREEN)
        } else {
            txtInstruccion.text = "FALLO"
            txtInstruccion.setTextColor(Color.RED)
        }

        trials++

        // Esperamos 400ms para que el usuario vea si acertó o falló
        Handler(Looper.getMainLooper()).postDelayed({
            siguienteTrial()
        }, 400)
    }

    private fun finalizarTest() {
        // Cálculo de nota: (Aciertos / 5) * 100.  Cada acierto vale 20 pts.
        val finalScore = (score * 100 / MAX_TRIALS).coerceIn(0, 100)

        // Guardamos métricas
        val details = mapOf("aciertos" to score, "total_trials" to MAX_TRIALS)
        CortexManager.logPerformanceMetric("t9", finalScore, details)
        CortexManager.guardarPuntaje("t9", finalScore)

        // --- LÓGICA DE EXONERACIÓN ---
        // Necesita 80% (4 de 5) para pasar a la primera.
        if (intentoActual == 1 && finalScore < 80) {
            mostrarDialogoFin(finalScore, esReintento = true)
        } else {
            mostrarDialogoFin(finalScore, esReintento = false)
        }
    }

    private fun mostrarDialogoFin(puntaje: Int, esReintento: Boolean) {
        if (isFinishing) return

        val titulo: String
        val mensaje: String
        val textoBoton: String

        if (esReintento) {
            titulo = "ORIENTACIÓN CONFUSA ⚠️"
            mensaje = "Acertaste $score de $MAX_TRIALS.\nNota: $puntaje%\n\nRecuerda: Si es ROJA, presiona el botón CONTRARIO. Inténtalo de nuevo."
            textoBoton = "INTENTO 2"
        } else {
            titulo = if (puntaje >= 80) "BUENA ORIENTACIÓN 🧭" else "TEST FINALIZADO"
            mensaje = "Resultado: $score de $MAX_TRIALS aciertos.\nNota Final: $puntaje%"
            textoBoton = "SIGUIENTE TEST"
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton(textoBoton) { _, _ ->
                if (esReintento) {
                    recreate()
                } else {
                    CortexManager.navegarAlSiguiente(this)
                    finish()
                }
            }
            .show()
    }
}