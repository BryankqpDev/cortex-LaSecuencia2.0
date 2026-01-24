package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.actividades.WelcomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        val etEmpresa = findViewById<EditText>(R.id.et_empresa)
        val etSupervisor = findViewById<EditText>(R.id.et_supervisor)
        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etDni = findViewById<EditText>(R.id.et_dni)
        val etUnidad = findViewById<EditText>(R.id.et_unidad)
        val spinnerEquipo = findViewById<Spinner>(R.id.spinner_equipo)

        val btnSiguiente = findViewById<Button>(R.id.btn_siguiente)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)
        
        // --- ✅ RESTAURADO: BOTÓN CERRAR SESIÓN / SALIR ---
        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)
        val tvUsuarioActual = findViewById<TextView>(R.id.tv_usuario_actual)

        if (sessionManager.tieneSesionActiva()) {
            tvUsuarioActual.visibility = TextView.VISIBLE
            btnCerrarSesion.visibility = Button.VISIBLE
            tvUsuarioActual.text = "👤 ${sessionManager.getEmailUsuario()}"
        } else {
            tvUsuarioActual.visibility = TextView.GONE
            btnCerrarSesion.visibility = Button.GONE
        }

        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas salir?")
                .setPositiveButton("SÍ") { _, _ ->
                    sessionManager.cerrarSesion()
                    finishAffinity() // Cierra la app completamente
                }
                .setNegativeButton("NO", null)
                .show()
        }

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
                    startActivity(Intent(this, BiometriaActivity::class.java))
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                },
                onError = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                }
            )
        }

        btnAdmin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun esPlacaValida(placa: String): Boolean {
        val n = placa.replace(Regex("[\\s-]"), "").uppercase()
        return n.length == 6 && (n.matches(Regex("^[A-Z]{3}[0-9]{3}$")) || n.matches(Regex("^[0-9]{4}[A-Z]{2}$")))
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Salir")
            .setMessage("¿Deseas salir de la aplicación?")
            .setPositiveButton("SÍ") { _, _ -> finishAffinity() }
            .setNegativeButton("NO", null)
            .show()
    }
}