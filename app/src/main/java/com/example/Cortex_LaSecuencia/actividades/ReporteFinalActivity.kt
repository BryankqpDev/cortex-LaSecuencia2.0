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

    private var ultimoPdfGenerado: File? = null

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
            val timestampFin = if (operador.timestampFin > 0) operador.timestampFin else System.currentTimeMillis()
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

        // Generación automática del PDF al iniciar
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
        val file = ultimoPdfGenerado
        if (file == null || !file.exists()) {
            Toast.makeText(this, "⚠️ Primero genera el PDF con el botón DESCARGAR PDF", Toast.LENGTH_LONG).show()
            return
        }
        
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent.createChooser(intent, "Compartir con..."))
        }
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
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent.createChooser(intent, "Compartir con..."))
        }
    }

    private fun generarPDFSafe(resultados: Map<String, Int>, silent: Boolean) {
        try {
            val operador = CortexManager.operadorActual
            if (operador != null) {
                val bitmapFoto = cargarFotoLocal(operador.dni)
                val pdfFile = PDFGenerator.generarPDF(this, operador, resultados, bitmapFoto)
                
                // Guardar la referencia exacta de retorno
                ultimoPdfGenerado = pdfFile
                
                if (!silent && pdfFile != null) {
                    Toast.makeText(this, "PDF generado", Toast.LENGTH_LONG).show()
                }
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
            hint = "Ingrese código OTP de 6 dígitos"
            gravity = Gravity.CENTER; setTextColor(Color.BLACK)
            textSize = 20f
            letterSpacing = 0.3f
        }

        val btnSolicitar = Button(this).apply {
            text = "📩 SOLICITAR CÓDIGO"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            textSize = 14f
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(btnSolicitar)
            addView(android.widget.Space(this@ReporteFinalActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 24
                )
            })
            addView(inputCodigo)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("🔐 DESBLOQUEO OTP")
            .setMessage("Solicite el código al administrador")
            .setView(layout)
            .setPositiveButton("VALIDAR", null) // Se configura después para evitar dismiss automático
            .setNegativeButton("CANCELAR", null)
            .create()

        dialog.show()

        // ── Botón SOLICITAR CÓDIGO ──
        btnSolicitar.setOnClickListener {
            val operador = CortexManager.operadorActual ?: return@setOnClickListener
            btnSolicitar.isEnabled = false
            btnSolicitar.text = "Enviando..."

            val body = org.json.JSONObject().apply {
                put("nombre", operador.nombre)
                put("dni", operador.dni)
                put("empresa", operador.empresa)
                put("fecha", operador.fecha)
                put("hora", operador.hora)
            }

            callBackend("/solicitar-desbloqueo", body) { response, error ->
                runOnUiThread {
                    btnSolicitar.isEnabled = true
                    btnSolicitar.text = "📩 SOLICITAR CÓDIGO"
                    if (error == null && response?.optBoolean("success") == true) {
                        Toast.makeText(this, "✅ Código enviado al administrador", Toast.LENGTH_LONG).show()
                    } else {
                        val msg = error ?: response?.optString("error") ?: "Error desconocido"
                        Toast.makeText(this, "❌ Error al solicitar código: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // ── Botón VALIDAR (override para no cerrar el diálogo en caso de error) ──
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val codigoIngresado = inputCodigo.text.toString().trim()
            if (codigoIngresado.isEmpty()) {
                Toast.makeText(this, "Ingrese el código", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val operador = CortexManager.operadorActual ?: return@setOnClickListener

            val body = org.json.JSONObject().apply {
                put("dni", operador.dni)
                put("codigo", codigoIngresado)
            }

            callBackend("/validar-codigo", body) { response, error ->
                runOnUiThread {
                    if (error == null && response?.optBoolean("success") == true) {
                        Toast.makeText(this, "✅ Sistema desbloqueado", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        CortexManager.desbloquearSistema(this)
                        reiniciarApp()
                    } else {
                        Toast.makeText(this, "❌ Código inválido o expirado", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Llamada HTTP POST al backend de Cortex.
     * Ejecuta en hilo separado y devuelve resultado en callback.
     *
     * @param endpoint Ruta del endpoint (ej: "/solicitar-desbloqueo")
     * @param body JSONObject con el cuerpo de la petición
     * @param callback (response: JSONObject?, error: String?) → se ejecuta en hilo llamante
     */
    private fun callBackend(
        endpoint: String,
        body: org.json.JSONObject,
        callback: (response: org.json.JSONObject?, error: String?) -> Unit
    ) {
        Thread {
            try {
                val url = java.net.URL("https://supabase-catha-cortex-backend.c2awqr.easypanel.host$endpoint")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = org.json.JSONObject(responseText)
                callback(json, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Error de conexión")
            }
        }.start()
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