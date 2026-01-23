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

            if (empresa.isEmpty() || supervisor.isEmpty() || nombre.isEmpty() || dni.isEmpty() || unidad.isEmpty()) {
                Toast.makeText(this, "⚠️ Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dni.length != 8) {
                etDni.error = "El DNI debe tener 8 dígitos."
                return@setOnClickListener
            }

            if (!esPlacaValida(unidad)) {
                etUnidad.error = "Placa inválida"
                return@setOnClickListener
            }

            // --- ✅ SOLUCIÓN: INICIAR SESIÓN ANÓNIMA PARA PERMISOS DE STORAGE ---
            btnSiguiente.isEnabled = false
            btnSiguiente.text = "AUTENTICANDO..."

            CortexManager.autenticarConductorAnonimo(
                onSuccess = {
                    val nuevoOperador = Operador(
                        nombre = nombre, dni = dni, empresa = empresa,
                        supervisor = supervisor, equipo = equipoSeleccionado, unidad = unidad,
                        fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                        hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    CortexManager.operadorActual = nuevoOperador
                    
                    // Ir a biometría con sesión activa
                    startActivity(Intent(this, BiometriaActivity::class.java))
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                },
                onError = { error ->
                    Toast.makeText(this, "Error de conexión: $error", Toast.LENGTH_LONG).show()
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                }
            )
        }

        btnAdmin.setOnClickListener { mostrarDialogoAdmin() }
    }

    private fun esPlacaValida(placa: String): Boolean {
        val n = placa.replace(Regex("[\\s-]"), "").uppercase()
        return n.length == 6 && (n.matches(Regex("^[A-Z]{3}[0-9]{3}$")) || n.matches(Regex("^[0-9]{4}[A-Z]{2}$")))
    }

    private fun mostrarDialogoAdmin() {
        val input = EditText(this).apply { inputType = 18; hint = "1007"; gravity = 17 }
        AlertDialog.Builder(this).setTitle("ACCESO ADMIN").setView(input)
            .setPositiveButton("ENTRAR") { _, _ ->
                if (input.text.toString() == "1007") startActivity(Intent(this, LoginActivity::class.java))
            }.show()
    }
}
