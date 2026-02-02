package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// ✅ IMPORTS CORRECTOS DE TUS ACTIVIDADES (Carpeta actividades)
import com.example.Cortex_LaSecuencia.actividades.AdminActivity
import com.example.Cortex_LaSecuencia.actividades.LoginActivity
// ✅ IMPORT DE UTILS
import com.example.Cortex_LaSecuencia.SessionManager
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.Operador


// ❌ NOTA: No necesitamos importar BiometriaActivity, Operador ni CortexManager
// porque están en el mismo paquete (com.example.Cortex_LaSecuencia)

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar SessionManager
        sessionManager = SessionManager(this)

        // Referencias a la UI
        val etEmpresa = findViewById<EditText>(R.id.et_empresa)
        val etSupervisor = findViewById<EditText>(R.id.et_supervisor)
        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etDni = findViewById<EditText>(R.id.et_dni)
        val etUnidad = findViewById<EditText>(R.id.et_unidad)
        val spinnerEquipo = findViewById<Spinner>(R.id.spinner_equipo)
        val btnSiguiente = findViewById<Button>(R.id.btn_siguiente)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)

        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)
        val tvUsuarioActual = findViewById<TextView>(R.id.tv_usuario_actual)

        // 1. MANEJO DEL BOTÓN ATRÁS (Evita salir por error)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("⚠️ Salir")
                    .setMessage("¿Deseas salir de la aplicación?")
                    .setPositiveButton("SÍ") { _, _ ->
                        finishAffinity()
                    }
                    .setNegativeButton("NO", null)
                    .show()
            }
        })

        // 2. VERIFICACIÓN DE SESIÓN (Para mostrar usuario logueado)
        if (sessionManager.tieneSesionActiva()) {
            tvUsuarioActual.visibility = TextView.VISIBLE
            btnCerrarSesion.visibility = Button.VISIBLE
            tvUsuarioActual.text = "👤 ${sessionManager.getEmailUsuario()}"
        } else {
            tvUsuarioActual.visibility = TextView.GONE
            btnCerrarSesion.visibility = Button.GONE
        }

        // Lógica Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas salir?")
                .setPositiveButton("SÍ") { _, _ ->
                    sessionManager.cerrarSesion()
                    recreate() // Recarga la actividad para actualizar la UI
                }
                .setNegativeButton("NO", null)
                .show()
        }

        // 3. BOTÓN SIGUIENTE -> VALIDACIÓN Y BIOMETRÍA
        btnSiguiente.setOnClickListener {
            // Obtener datos y limpiar espacios
            val empresa = etEmpresa.text.toString().trim().uppercase()
            val supervisor = etSupervisor.text.toString().trim().uppercase()
            val nombre = etNombre.text.toString().trim().uppercase()
            val dni = etDni.text.toString().trim()
            val unidad = etUnidad.text.toString().trim().uppercase()
            val equipoSeleccionado = spinnerEquipo.selectedItem.toString()

            // Validaciones básicas
            if (empresa.isEmpty() || supervisor.isEmpty() || nombre.isEmpty() || dni.isEmpty() || unidad.isEmpty()) {
                Toast.makeText(this, "⚠️ Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dni.length != 8) {
                etDni.error = "El DNI debe tener 8 dígitos."
                return@setOnClickListener
            }

            if (!esPlacaValida(unidad)) {
                etUnidad.error = "Placa inválida (Ej: ABC-123 o 1234-AB)"
                return@setOnClickListener
            }

            // Bloquear botón para evitar doble click
            btnSiguiente.isEnabled = false
            btnSiguiente.text = "AUTENTICANDO..."

            // Autenticación Anónima con Firebase antes de pasar a la cámara
            CortexManager.autenticarConductorAnonimo(
                onSuccess = {
                    // Guardar datos en el Singleton temporalmente
                    CortexManager.operadorActual = Operador(
                        nombre, dni, empresa, supervisor, equipoSeleccionado, unidad,
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    )

                    // ✅ Navegar a BiometriaActivity (está en el mismo paquete raíz)
                    val intent = Intent(this@MainActivity, BiometriaActivity::class.java)
                    startActivity(intent)

                    // Restaurar botón (por si vuelven atrás)
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                },
                onError = { error ->
                    Toast.makeText(this@MainActivity, "Error de conexión: $error", Toast.LENGTH_LONG).show()
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                }
            )
        }

        // 4. BOTÓN ADMIN (Acceso restringido)
        btnAdmin.setOnClickListener {
            if (sessionManager.tieneSesionActiva()) {
                // Si ya es admin, pasa directo
                val intent = Intent(this, AdminActivity::class.java)
                startActivity(intent)
            } else {
                // Si no, pide login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Función auxiliar para validar placas peruanas
    private fun esPlacaValida(placa: String): Boolean {
        val n = placa.replace(Regex("[\\s-]"), "").uppercase()
        // Acepta formato antiguo (ABC123) y nuevo/moto (1234AB)
        return n.length == 6 && (n.matches(Regex("^[A-Z]{3}[0-9]{3}$")) || n.matches(Regex("^[0-9]{4}[A-Z]{2}$")))
    }
}