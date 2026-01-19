package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.actividades.WelcomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etEmpresa = findViewById<EditText>(R.id.et_empresa)
        val etSupervisor = findViewById<EditText>(R.id.et_supervisor)
        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etDni = findViewById<EditText>(R.id.et_dni)
        val etUnidad = findViewById<EditText>(R.id.et_unidad)
        val spinnerEquipo = findViewById<Spinner>(R.id.spinner_equipo)

        val btnSiguiente = findViewById<Button>(R.id.btn_siguiente)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)

        btnSiguiente.setOnClickListener {
            val empresa = etEmpresa.text.toString().trim().uppercase()
            val supervisor = etSupervisor.text.toString().trim().uppercase()
            val nombre = etNombre.text.toString().trim().uppercase()
            val dni = etDni.text.toString().trim()
            val unidad = etUnidad.text.toString().trim().uppercase()
            val equipoSeleccionado = spinnerEquipo.selectedItem.toString()

            // --- VALIDACIONES ---
            if (empresa.isEmpty() || supervisor.isEmpty() || nombre.isEmpty() || dni.isEmpty() || unidad.isEmpty()) {
                Toast.makeText(this, "⚠️ Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dni.length != 8) {
                etDni.error = "El DNI debe tener 8 dígitos."
                etDni.requestFocus()
                return@setOnClickListener
            }

            if (!esPlacaValida(unidad)) {
                etUnidad.error = "Placa inválida (ej: ABC-123 o 1234-AB)"
                etUnidad.requestFocus()
                return@setOnClickListener
            } else {
                etUnidad.error = null
            }

            if (equipoSeleccionado.startsWith("-")) {
                Toast.makeText(this, "⚠️ Seleccione un equipo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = Date()
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
            val horaHoy = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

            val nuevoOperador = Operador(
                nombre = nombre, dni = dni, empresa = empresa,
                supervisor = supervisor, equipo = equipoSeleccionado, unidad = unidad,
                fecha = fechaHoy, hora = horaHoy
            )

            CortexManager.operadorActual = nuevoOperador
            Toast.makeText(this, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }

        btnAdmin.setOnClickListener { mostrarDialogoAdmin() }
    }

    private fun esPlacaValida(placa: String): Boolean {
        val placaNormalizada = placa.replace(Regex("[\\s-]"), "").uppercase()
        if (placaNormalizada.length != 6) return false
        val formatoAuto = "^[A-Z]{3}[0-9]{3}$"
        if (placaNormalizada.matches(Regex(formatoAuto))) return true
        val formatoMoto = "^[0-9]{4}[A-Z]{2}$"
        if (placaNormalizada.matches(Regex(formatoMoto))) return true
        return false
    }

    private fun mostrarDialogoAdmin() {
        val inputPassword = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Código (1007)"
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.BLACK)
        }

        val container = android.widget.FrameLayout(this).apply {
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 50; rightMargin = 50 }
            addView(inputPassword, params)
        }

        AlertDialog.Builder(this)
            .setTitle("ACCESO SUPERVISOR 🔒")
            .setMessage("Ingrese código de seguridad:")
            .setView(container)
            .setPositiveButton("ENTRAR") { _, _ ->
                if (inputPassword.text.toString() == "1007") {
                    Toast.makeText(this, "Acceso Autorizado ✅", Toast.LENGTH_SHORT).show()
                    
                    // --- ✅ CAMBIO: AHORA ENVÍA A LOGINACTIVITY ---
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "⛔ Código Incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }
}