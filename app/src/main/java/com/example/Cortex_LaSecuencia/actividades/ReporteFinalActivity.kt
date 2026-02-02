package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.PDFGenerator
import com.example.Cortex_LaSecuencia.MainActivity
import java.io.File


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
        val resultados = CortexManager.obtenerResultados()
        val nombresPruebas = mapOf(
            "t1" to "Reflejos", "t2" to "Memoria", "t3" to "Anticipación",
            "t4" to "Coordinación", "t5" to "Atención", "t6" to "Escaneo",
            "t7" to "Impulso", "t8" to "Rastreo", "t9" to "Espacial", "t10" to "Decisión"
        )

        var sumaNotas = 0
        val llavesOrdenadas = resultados.keys.sortedBy { it.removePrefix("t").toIntOrNull() ?: 99 }

        llavesOrdenadas.forEach { key ->
            val nota = resultados[key] ?: 0
            sumaNotas += nota
            agregarFila(containerResultados, nombresPruebas[key] ?: key.uppercase(), nota)
        }

        val promedio = if (resultados.isNotEmpty()) sumaNotas / resultados.size else 0
        txtPuntaje.text = "$promedio%"

        val esApto = promedio >= 75
        val nombreUsuario = CortexManager.operadorActual?.nombre?.split(" ")?.get(0) ?: "OPERADOR"

        CortexManager.registrarEvaluacion(promedio, esApto)
        CortexManager.registrarNube(promedio, esApto)

        if (esApto) {
            txtIcono.text = getString(R.string.report_icon_success)
            txtEstado.text = getString(R.string.report_status_apt)
            txtEstado.setTextColor(Color.parseColor("#10B981"))
            txtPuntaje.setTextColor(Color.parseColor("#10B981"))
            txtMensaje.text = getString(R.string.report_msg_success, nombreUsuario)
            btnReiniciar.text = getString(R.string.btn_finish_exit)
            btnReiniciar.background.setTint(Color.parseColor("#2563EB"))
            AudioManager.hablar("Felicidades. Maneje con cuidado. Su familia lo espera.")
        } else {
            txtIcono.text = getString(R.string.report_icon_failure)
            txtEstado.text = getString(R.string.report_status_not_apt)
            txtEstado.setTextColor(Color.parseColor("#EF4444"))
            txtPuntaje.setTextColor(Color.parseColor("#EF4444"))
            txtMensaje.text = getString(R.string.report_msg_failure, nombreUsuario)
            CortexManager.bloquearSistema(this)
            btnReiniciar.text = getString(R.string.btn_supervisor_unlock)
            btnReiniciar.background.setTint(Color.parseColor("#334155"))
            AudioManager.hablar("Lo siento. No cumple con el estándar de seguridad. Sistema bloqueado.")
        }

        // === 4. GENERACIÓN DE PDF AUTOMÁTICA ===
        generarPDFSafe(resultados, silent = true)

        btnDescargarPDF.setOnClickListener {
            generarPDFSafe(resultados, silent = false)
        }

        btnReiniciar.setOnClickListener {
            if (!esApto) mostrarDialogoDesbloqueo()
            else reiniciarApp()
        }
    }

    /**
     * ✅ RECUPERA LA FOTO GUARDADA EN EL CELULAR
     */
    private fun cargarFotoLocal(dni: String): Bitmap? {
        return try {
            // La ruta es: /data/user/0/com.example.Cortex_LaSecuencia/files/selfie_DNI.jpg
            val file = File(filesDir, "selfie_$dni.jpg")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generarPDFSafe(resultados: Map<String, Int>, silent: Boolean) {
        try {
            val operador = CortexManager.operadorActual
            if (operador != null) {
                // --- ✅ BUSCAR FOTO ANTES DE GENERAR ---
                val bitmapFoto = cargarFotoLocal(operador.dni)

                val pdfFile = PDFGenerator.generarPDF(
                    context = this,
                    operador = operador,
                    resultados = resultados,
                    fotoBitmap = bitmapFoto // ← AQUÍ PASAMOS LA FOTO AL PDF
                )
                
                if (!silent && pdfFile != null) {
                    Toast.makeText(this, getString(R.string.msg_success_pdf), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            if (!silent) Toast.makeText(this, getString(R.string.msg_error_pdf, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoDesbloqueo() {
        val inputCodigo = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Código Supervisor"
            gravity = Gravity.CENTER; setTextColor(Color.BLACK)
        }
        val container = android.widget.FrameLayout(this).apply { setPadding(50, 20, 50, 0); addView(inputCodigo) }
        AlertDialog.Builder(this).setTitle("🔓 DESBLOQUEO").setMessage("Ingrese el código (1007):").setView(container)
            .setPositiveButton("VALIDAR") { _, _ ->
                if (CortexManager.verificarCodigoSupervisor(inputCodigo.text.toString())) {
                    CortexManager.desbloquearSistema(this)
                    Toast.makeText(this, getString(R.string.msg_success_unlocked), Toast.LENGTH_SHORT).show()
                    reiniciarApp()
                } else Toast.makeText(this, getString(R.string.msg_error_incorrect_code), Toast.LENGTH_SHORT).show()
            }.setNegativeButton("CANCELAR", null).show()
    }

    private fun reiniciarApp() {
        CortexManager.resetearEvaluacion()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun agregarFila(container: LinearLayout, nombre: String, nota: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(10, 15, 10, 15) }
        val txtNombre = TextView(this).apply { text = nombre; setTextColor(Color.WHITE); textSize = 14f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val txtNota = TextView(this).apply { text = "$nota%"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(if (nota >= 75) Color.parseColor("#10B981") else Color.parseColor("#EF4444")) }
        row.addView(txtNombre); row.addView(txtNota); container.addView(row)
        val divider = android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(Color.parseColor("#334155")) }
        container.addView(divider)
    }
}