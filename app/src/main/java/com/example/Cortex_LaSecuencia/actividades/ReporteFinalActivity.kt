package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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

        val txtIcono = findViewById<TextView>(R.id.txt_icono_estado)
        val txtPuntaje = findViewById<TextView>(R.id.txt_puntaje_global)
        val txtEstado = findViewById<TextView>(R.id.txt_estado_texto)
        val txtMensaje = findViewById<TextView>(R.id.txt_mensaje_feedback)
        val containerResultados = findViewById<LinearLayout>(R.id.container_resultados)
        val btnReiniciar = findViewById<Button>(R.id.btn_reiniciar)
        val btnDescargarPDF = findViewById<Button>(R.id.btn_descargar_pdf)
        val btnCompartir = findViewById<Button>(R.id.btn_compartir_whatsapp)

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

        val operador = CortexManager.operadorActual
        if (operador != null && operador.timestampInicio > 0) {
            val timestampFin = System.currentTimeMillis()
            val tiempoTranscurridoMs = timestampFin - operador.timestampInicio
            val tiempoSegundos = (tiempoTranscurridoMs / 1000).toInt()
            operador.tiempoTotalSegundos = tiempoSegundos
        }

        if (esApto) {
            txtIcono.text = "😎"
            txtEstado.text = "APTO"
            txtEstado.setTextColor(Color.parseColor("#10B981"))
            txtPuntaje.setTextColor(Color.parseColor("#10B981"))
            txtMensaje.text = "¡Buen trabajo, $nombreUsuario!\nConduce con precaución."
            btnReiniciar.background.setTint(Color.parseColor("#2563EB"))
            AudioManager.hablar("Felicidades. Maneje con cuidado. Su familia lo espera.")
        } else {
            txtIcono.text = "🚫"
            txtEstado.text = "NO APTO"
            txtEstado.setTextColor(Color.parseColor("#EF4444"))
            txtPuntaje.setTextColor(Color.parseColor("#EF4444"))
            txtMensaje.text = "$nombreUsuario, no cumples con el estándar.\nSISTEMA BLOQUEADO."
            CortexManager.enviarSolicitudDesbloqueo("Nota final: $promedio% (Umbral: 75%)")
            CortexManager.bloquearSistema(this)
            btnReiniciar.text = "DESBLOQUEO REMOTO / SUPERVISOR"
            btnReiniciar.background.setTint(Color.parseColor("#334155"))
            AudioManager.hablar("Lo siento. No cumple con el estándar de seguridad. Sistema bloqueado.")
        }

        generarPDFSafe(resultados, silent = true)
        btnDescargarPDF.setOnClickListener { generarPDFSafe(resultados, silent = false) }

        btnCompartir.setOnClickListener { mostrarDialogoCompartir() }

        btnReiniciar.setOnClickListener {
            if (!esApto) mostrarDialogoDesbloqueo()
            else reiniciarApp()
        }
    }

    private fun mostrarDialogoCompartir() {
        AlertDialog.Builder(this)
            .setTitle("Compartir Reporte")
            .setItems(arrayOf("📄 Compartir PDF", "📸 Compartir Captura")) { _, which ->
                if (which == 0) compartirPDF() else compartirCaptura()
            }
            .show()
    }

    private fun compartirPDF() {
        val operador = CortexManager.operadorActual ?: return
        val file = File(filesDir, "reporte_${operador.dni}.pdf")
        if (!file.exists()) { Toast.makeText(this, "PDF no encontrado", Toast.LENGTH_SHORT).show(); return }
        
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        try { startActivity(intent) } catch (e: Exception) { startActivity(Intent.createChooser(intent, "Compartir con...")) }
    }

    private fun compartirCaptura() {
        val rootView = window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false
        
        val file = File(cacheDir, "captura_reporte.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        try { startActivity(intent) } catch (e: Exception) { startActivity(Intent.createChooser(intent, "Compartir con...")) }
    }

    private fun generarPDFSafe(resultados: Map<String, Int>, silent: Boolean) {
        try {
            val operador = CortexManager.operadorActual
            if (operador != null) {
                val bitmapFoto = cargarFotoLocal(operador.dni)
                val pdfFile = PDFGenerator.generarPDF(this, operador, resultados, bitmapFoto)
                if (!silent && pdfFile != null) Toast.makeText(this, "PDF generado", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) { }
    }

    private fun cargarFotoLocal(dni: String): Bitmap? {
        return try {
            val file = File(filesDir, "selfie_$dni.jpg")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (e: Exception) { null }
    }

    private fun mostrarDialogoDesbloqueo() {
        val inputCodigo = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Código (1007)"
            gravity = Gravity.CENTER; setTextColor(Color.BLACK)
        }
        val container = android.widget.FrameLayout(this).apply { setPadding(50, 20, 50, 0); addView(inputCodigo) }
        
        AlertDialog.Builder(this)
            .setTitle("🔓 DESBLOQUEO")
            .setMessage("Esperando autorización del administrador o ingrese código manual:")
            .setView(container)
            .setPositiveButton("VALIDAR") { _, _ ->
                if (CortexManager.verificarCodigoSupervisor(inputCodigo.text.toString())) {
                    CortexManager.desbloquearSistema(this)
                    reiniciarApp()
                } else Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun reiniciarApp() {
        CortexManager.resetearEvaluacion()
        startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }

    private fun agregarFila(container: LinearLayout, nombre: String, nota: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(10, 15, 10, 15) }
        val txtNombre = TextView(this).apply { text = nombre; setTextColor(Color.WHITE); textSize = 14f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val txtNota = TextView(this).apply { text = "$nota%"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END; setTextColor(if (nota >= 75) Color.parseColor("#10B981") else Color.parseColor("#EF4444")) }
        row.addView(txtNombre); row.addView(txtNota); container.addView(row)
    }
}