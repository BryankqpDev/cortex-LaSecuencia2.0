package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.MainActivity
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.PDFGenerator

class ReporteFinalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_final)

        // === INICIALIZAR VISTAS ===
        val txtIcono = findViewById<TextView>(R.id.txt_icono_estado)
        val txtPuntaje = findViewById<TextView>(R.id.txt_puntaje_global)
        val txtEstado = findViewById<TextView>(R.id.txt_estado_texto)
        val txtMensaje = findViewById<TextView>(R.id.txt_mensaje_feedback)
        val containerResultados = findViewById<LinearLayout>(R.id.container_resultados)
        val btnReiniciar = findViewById<Button>(R.id.btn_reiniciar)
        val btnDescargarPDF = findViewById<Button>(R.id.btn_descargar_pdf)

        // === 1. OBTENER DATOS ===
        val resultados = CortexManager.obtenerResultados() // Esto devuelve Map<String, Int>
        val nombresPruebas = mapOf(
            "t1" to "Reflejos", "t2" to "Memoria", "t3" to "Anticipación",
            "t4" to "Coordinación", "t5" to "Atención", "t6" to "Escaneo",
            "t7" to "Impulso", "t8" to "Rastreo", "t9" to "Espacial", "t10" to "Decisión"
        )

        // === 2. CALCULAR PROMEDIO REAL ===
        var sumaNotas = 0
        // Aseguramos orden correcto de T1 a T10
        val llavesOrdenadas = resultados.keys.sortedBy { it.removePrefix("t").toIntOrNull() ?: 99 }

        llavesOrdenadas.forEach { key ->
            val nota = resultados[key] ?: 0
            sumaNotas += nota
            agregarFila(containerResultados, nombresPruebas[key] ?: key.uppercase(), nota)
        }

        // Evitar división por cero
        val promedio = if (resultados.isNotEmpty()) sumaNotas / resultados.size else 0

        // MOSTRAR PUNTAJE GRANDE
        txtPuntaje.text = "$promedio%"

        // === 3. EVALUACIÓN FINAL (Regla de Oro: >= 75 es APTO) ===
        val esApto = promedio >= 75
        val nombreUsuario = CortexManager.operadorActual?.nombre?.split(" ")?.get(0) ?: "OPERADOR"

        // Guardar en base de datos interna
        CortexManager.registrarEvaluacion(promedio, esApto)

        //Guardar en FIREBASE u OTRO
        CortexManager.registrarNube(promedio, esApto)

        if (esApto) {
            // --- ESCENARIO APTO (VERDE) ---
            txtIcono.text = "😎✅"
            txtEstado.text = "APTO"
            txtEstado.setTextColor(Color.parseColor("#10B981")) // Verde Esmeralda
            txtPuntaje.setTextColor(Color.parseColor("#10B981"))

            txtMensaje.text = "¡Bien hecho, $nombreUsuario! ❤️\nTU FAMILIA TE ESPERA EN CASA."

            // Configurar botones
            btnReiniciar.text = "FINALIZAR / SALIR"
            btnReiniciar.isEnabled = true
            btnReiniciar.background.setTint(Color.parseColor("#2563EB")) // Azul

            AudioManager.hablar("Felicidades. Maneje con cuidado. Su familia lo espera.")
        } else {
            // --- ESCENARIO NO APTO (ROJO) ---
            txtIcono.text = "😴🚫"
            txtEstado.text = "NO APTO"
            txtEstado.setTextColor(Color.parseColor("#EF4444")) // Rojo Peligro
            txtPuntaje.setTextColor(Color.parseColor("#EF4444"))

            txtMensaje.text = "Hola $nombreUsuario. Parece que no descansaste bien.\nSISTEMA BLOQUEADO (24H)."

            // Bloqueo lógico en la app
            CortexManager.bloquearSistema(this)

            // Configurar botones
            btnReiniciar.text = "DESBLOQUEO DE SUPERVISOR 🔒"
            btnReiniciar.background.setTint(Color.parseColor("#334155")) // Gris Oscuro

            AudioManager.hablar("Lo siento. No cumple con el estándar de seguridad. Sistema bloqueado.")
        }

        // === 4. GENERACIÓN DE PDF ===
        // Generar automáticamente al entrar
        generarPDFSafe(resultados, silent = true)

        btnDescargarPDF.setOnClickListener {
            generarPDFSafe(resultados, silent = false)
        }

        btnReiniciar.setOnClickListener {
            if (!esApto) {
                mostrarDialogoDesbloqueo()
            } else {
                reiniciarApp()
            }
        }
    }

    private fun generarPDFSafe(resultados: Map<String, Int>, silent: Boolean) {
        try {
            val operador = CortexManager.operadorActual
            if (operador != null) {
                val pdfFile = PDFGenerator.generarPDF(
                    context = this,
                    operador = operador,
                    resultados = resultados,
                    fotoBitmap = null // Si tuvieras la foto del inicio, pásala aquí
                )
                if (!silent && pdfFile != null) {
                    Toast.makeText(this, "✅ PDF Guardado en Descargas", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            if (!silent) Toast.makeText(this, "Error PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoDesbloqueo() {
        val inputCodigo = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Código Supervisor"
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
        }

        val container = android.widget.FrameLayout(this).apply {
            setPadding(50, 20, 50, 0)
            addView(inputCodigo)
        }

        AlertDialog.Builder(this)
            .setTitle("🔓 DESBLOQUEO")
            .setMessage("Ingrese el código (1007):")
            .setView(container)
            .setPositiveButton("VALIDAR") { _, _ ->
                val codigo = inputCodigo.text.toString()
                if (CortexManager.verificarCodigoSupervisor(codigo)) {
                    CortexManager.desbloquearSistema(this)
                    Toast.makeText(this, "✅ DESBLOQUEADO", Toast.LENGTH_SHORT).show()
                    reiniciarApp()
                } else {
                    Toast.makeText(this, "❌ CÓDIGO INCORRECTO", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun reiniciarApp() {
        CortexManager.resetearEvaluacion()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun agregarFila(container: LinearLayout, nombre: String, nota: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 15, 10, 15)
        }

        val txtNombre = TextView(this).apply {
            text = nombre
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val txtNota = TextView(this).apply {
            text = "$nota%"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            // Colores umbral individuales (opcional, visualmente ayuda)
            setTextColor(if (nota >= 75) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
        }

        row.addView(txtNombre)
        row.addView(txtNota)
        container.addView(row)

        // Línea divisoria tenue
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#334155"))
        }
        container.addView(divider)
    }
}