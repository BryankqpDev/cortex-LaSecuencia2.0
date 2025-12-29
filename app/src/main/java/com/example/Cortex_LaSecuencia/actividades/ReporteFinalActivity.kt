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
import com.example.Cortex_LaSecuencia.MainActivity // Asegúrate que este sea tu Login/Inicio
import com.example.Cortex_LaSecuencia.R

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

        // 1. Obtener Datos
        val resultados = CortexManager.obtenerResultados()

        // Diccionario para traducir códigos (t1, t2) a nombres bonitos
        val nombresPruebas = mapOf(
            "t1" to "Reflejos",
            "t2" to "Memoria",
            "t3" to "Anticipación",
            "t4" to "Coordinación",
            "t5" to "Atención",
            "t6" to "Escaneo Visual",
            "t7" to "Control Impulso"
        )

        var sumaNotas = 0
        var totalTests = 0

        // 2. Llenar la lista dinámicamente
        // Ordenamos por clave (t1, t2, t3...) para que salgan en orden
        resultados.toSortedMap().forEach { (key, nota) ->
            sumaNotas += nota
            totalTests++

            val nombreReal = nombresPruebas[key] ?: key.uppercase()
            agregarFila(containerResultados, nombreReal, nota)
        }

        // 3. Calcular Promedio
        val promedio = if (totalTests > 0) sumaNotas / totalTests else 0
        txtPuntaje.text = "$promedio%"

        // 4. Lógica de Aprobación (Threshold: 75%)
        val esApto = promedio >= 75
        val nombreUsuario = CortexManager.operadorActual?.nombre ?: "OPERADOR"
        val primerNombre = nombreUsuario.split(" ")[0] // Solo el primer nombre

        if (esApto) {
            // --- CASO APTO ---
            txtIcono.text = "😎✅"
            txtEstado.text = "APTO"
            txtEstado.setTextColor(Color.parseColor("#10B981")) // Verde
            txtPuntaje.setTextColor(Color.parseColor("#10B981"))
            txtMensaje.text = "¡Bien hecho, $primerNombre! ❤️\nTU FAMILIA TE ESPERA EN CASA."

            btnReiniciar.isEnabled = true
            btnReiniciar.background.setTint(Color.parseColor("#2563EB")) // Azul
        } else {
            // --- CASO NO APTO ---
            txtIcono.text = "😴🚫"
            txtEstado.text = "NO APTO"
            txtEstado.setTextColor(Color.parseColor("#EF4444")) // Rojo
            txtPuntaje.setTextColor(Color.parseColor("#EF4444"))
            txtMensaje.text = "Hola $primerNombre. Parece que no descansaste bien.\nSISTEMA BLOQUEADO (24H)."

            // Simulación de bloqueo
            btnReiniciar.text = "DESBLOQUEO DE SUPERVISOR 🔒"
            btnReiniciar.background.setTint(Color.parseColor("#334155")) // Gris oscuro
        }

        // 5. Botón de Reinicio
        btnReiniciar.setOnClickListener {
            // Aquí limpiamos todo para el siguiente usuario
            CortexManager.resetearEvaluacion()

            // Volver al Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // Función auxiliar para crear filas visuales (Nombre ....... 100%)
    private fun agregarFila(container: LinearLayout, nombre: String, nota: Int) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, 16, 0, 16)

        val txtNombre = TextView(this)
        txtNombre.text = nombre
        txtNombre.setTextColor(Color.WHITE)
        txtNombre.textSize = 16f
        txtNombre.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val txtNota = TextView(this)
        txtNota.text = "$nota%"
        txtNota.textSize = 16f
        txtNota.typeface = Typeface.DEFAULT_BOLD
        txtNota.gravity = Gravity.END

        // Color de la nota individual
        if (nota >= 75) {
            txtNota.setTextColor(Color.parseColor("#10B981")) // Verde
        } else {
            txtNota.setTextColor(Color.parseColor("#EF4444")) // Rojo
        }

        row.addView(txtNombre)
        row.addView(txtNota)
        container.addView(row)
    }
}