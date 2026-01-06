package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.MainActivity
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.PDFGenerator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class ReporteFinalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_final)

        // === INICIALIZAR COMPONENTES ===
        val txtIcono = findViewById<TextView>(R.id.txt_icono_estado)
        val txtPuntaje = findViewById<TextView>(R.id.txt_puntaje_global)
        val txtEstado = findViewById<TextView>(R.id.txt_estado_texto)
        val txtMensaje = findViewById<TextView>(R.id.txt_mensaje_feedback)
        val containerResultados = findViewById<LinearLayout>(R.id.container_resultados)
        val btnReiniciar = findViewById<Button>(R.id.btn_reiniciar)
        val btnDescargarPDF = findViewById<Button>(R.id.btn_descargar_pdf) // NUEVO

        // === OBTENER RESULTADOS ===
        val resultados = CortexManager.obtenerResultados()
        val nombresPruebas = mapOf(
            "t1" to "Reflejos",
            "t2" to "Memoria",
            "t3" to "Anticipación",
            "t4" to "Coordinación",
            "t5" to "Atención",
            "t6" to "Escaneo Visual",
            "t7" to "Control Impulso",
            "t8" to "Rastreo",
            "t9" to "Espacial",
            "t10" to "Decisión"
        )

        // === CALCULAR PROMEDIO ===
        var sumaNotas = 0
        resultados.toSortedMap().forEach { (key, nota) ->
            sumaNotas += nota
            agregarFila(containerResultados, nombresPruebas[key] ?: key.uppercase(), nota)
        }

        val promedio = if (resultados.isNotEmpty()) sumaNotas / resultados.size else 0
        txtPuntaje.text = "$promedio%"

        // === DETERMINAR SI ES APTO ===
        val esApto = promedio >= 75
        val nombreUsuario = CortexManager.operadorActual?.nombre?.split(" ")?.get(0) ?: "OPERADOR"

        // Registrar en historial
        CortexManager.registrarEvaluacion(promedio, esApto)

        // === CONFIGURAR UI SEGÚN RESULTADO ===
        if (esApto) {
            // ✅ APTO
            txtIcono.text = "😎✅"
            txtEstado.text = "APTO"
            txtEstado.setTextColor(Color.parseColor("#10B981"))
            txtPuntaje.setTextColor(Color.parseColor("#10B981"))
            txtMensaje.text = "¡Bien hecho, $nombreUsuario! ❤️\nTU FAMILIA TE ESPERA EN CASA."
            btnReiniciar.isEnabled = true
            btnReiniciar.background.setTint(Color.parseColor("#2563EB"))
            AudioManager.hablar("Felicidades. Maneje con cuidado. Su familia lo espera.")
        } else {
            // ❌ NO APTO
            txtIcono.text = "😴🚫"
            txtEstado.text = "NO APTO"
            txtEstado.setTextColor(Color.parseColor("#EF4444"))
            txtPuntaje.setTextColor(Color.parseColor("#EF4444"))
            txtMensaje.text = "Hola $nombreUsuario. Parece que no descansaste bien.\nSISTEMA BLOQUEADO (24H)."
            CortexManager.bloquearSistema(this)
            btnReiniciar.text = "DESBLOQUEO DE SUPERVISOR 🔒"
            btnReiniciar.background.setTint(Color.parseColor("#334155"))
            AudioManager.hablar("Lo siento. No cumple con el estándar de seguridad. Sistema bloqueado.")
        }

        // === GENERAR PDF AUTOMÁTICAMENTE ===
        generarPDFAutomatico(resultados)

        // === BOTÓN: DESCARGAR PDF MANUALMENTE ===
        btnDescargarPDF.setOnClickListener {
            generarPDFManual(resultados)
        }

        // === BOTÓN: REINICIAR / DESBLOQUEAR ===
        btnReiniciar.setOnClickListener {
            if (!esApto) {
                mostrarDialogoDesbloqueo()
            } else {
                reiniciarApp()
            }
        }
    }

    /**
     * Generar PDF automáticamente al mostrar resultados
     */
    private fun generarPDFAutomatico(resultados: Map<String, Int>) {
        try {
            CortexManager.operadorActual?.let { operador ->
                val pdfFile = PDFGenerator.generarPDF(
                    context = this,
                    operador = operador,
                    resultados = resultados,
                    fotoBitmap = null // Puedes capturar foto si tienes
                )

                if (pdfFile != null) {
                    Toast.makeText(
                        this,
                        "✅ PDF generado automáticamente\nGuardado en: Descargas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } ?: run {
                Toast.makeText(
                    this,
                    "⚠️ Error: No hay datos del operador",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "❌ Error al generar PDF: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Generar PDF manualmente cuando el usuario presiona el botón
     */
    private fun generarPDFManual(resultados: Map<String, Int>) {
        try {
            CortexManager.operadorActual?.let { operador ->
                val pdfFile = PDFGenerator.generarPDF(
                    context = this,
                    operador = operador,
                    resultados = resultados,
                    fotoBitmap = null
                )

                if (pdfFile != null) {
                    // Mostrar diálogo con información
                    AlertDialog.Builder(this)
                        .setTitle("✅ PDF Generado")
                        .setMessage(
                            "Archivo: ${pdfFile.name}\n\n" +
                                    "Ubicación: Descargas\n\n" +
                                    "El PDF contiene el reporte completo de la evaluación."
                        )
                        .setPositiveButton("ENTENDIDO", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("❌ Error")
                .setMessage("No se pudo generar el PDF:\n${e.message}")
                .setPositiveButton("CERRAR", null)
                .show()
        }
    }

    /**
     * Mostrar diálogo para desbloqueo de supervisor
     */
    private fun mostrarDialogoDesbloqueo() {
        val inputCodigo = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Código (1007)"
            gravity = Gravity.CENTER
        }

        val container = android.widget.FrameLayout(this).apply {
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = 50
            params.rightMargin = 50
            inputCodigo.layoutParams = params
            addView(inputCodigo)
        }

        AlertDialog.Builder(this)
            .setTitle("🔓 DESBLOQUEO DE SUPERVISOR")
            .setMessage("Ingrese código de autorización:")
            .setView(container)
            .setPositiveButton("DESBLOQUEAR") { _, _ ->
                val codigo = inputCodigo.text.toString()
                if (CortexManager.verificarCodigoSupervisor(codigo)) {
                    CortexManager.desbloquearSistema(this)
                    Toast.makeText(this, "✅ BLOQUEO LEVANTADO", Toast.LENGTH_SHORT).show()
                    AudioManager.hablar("Sistema desbloqueado")
                    reiniciarApp()
                } else {
                    Toast.makeText(this, "❌ CÓDIGO INCORRECTO", Toast.LENGTH_SHORT).show()
                    AudioManager.hablar("Código incorrecto")
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    /**
     * Reiniciar aplicación
     */
    private fun reiniciarApp() {
        CortexManager.resetearEvaluacion()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Agregar fila de resultado a la lista
     */
    private fun agregarFila(container: LinearLayout, nombre: String, nota: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
        }

        val txtNombre = TextView(this).apply {
            text = nombre
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val txtNota = TextView(this).apply {
            text = "$nota%"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            setTextColor(
                if (nota >= 75) Color.parseColor("#10B981")
                else Color.parseColor("#EF4444")
            )
        }

        row.addView(txtNombre)
        row.addView(txtNota)
        container.addView(row)
    }
}