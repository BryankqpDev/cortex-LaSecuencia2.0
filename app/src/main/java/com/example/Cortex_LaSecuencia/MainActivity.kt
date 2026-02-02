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
import com.example.Cortex_LaSecuencia.actividades.LockedActivity
import com.example.Cortex_LaSecuencia.actividades.LoginActivity
import com.example.Cortex_LaSecuencia.actividades.AdminActivity
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

        val btnCerrarSesion = findViewById<Button>(R.id.btn_cerrar_sesion)
        val tvUsuarioActual = findViewById<TextView>(R.id.tv_usuario_actual)

        // --- SESIÓN DE ADMIN ---
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
                    recreate()
                }
                .setNegativeButton("NO", null)
                .show()
        }

        // ✅ CORREGIDO: Verificar sesión antes de ir a Login o Admin
        btnAdmin.setOnClickListener {
            if (sessionManager.tieneSesionActiva()) {
                // Ya tiene sesión activa, ir directo a AdminActivity
                startActivity(Intent(this@MainActivity, AdminActivity::class.java))
            } else {
                // No tiene sesión, pedir login
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
        }

        // --- FLUJO NORMAL DE REGISTRO ---
        btnSiguiente.setOnClickListener {
            val empresa = etEmpresa.text.toString().trim().uppercase()
            val supervisor = etSupervisor.text.toString().trim().uppercase()
            val nombre = etNombre.text.toString().trim().uppercase()
            val dni = etDni.text.toString().trim()
            val unidad = etUnidad.text.toString().trim().uppercase()
            val equipoSeleccionado = spinnerEquipo.selectedItem.toString()

            // 1. Validar campos vacíos
            if (empresa.isEmpty() || supervisor.isEmpty() || nombre.isEmpty() || dni.isEmpty() || unidad.isEmpty()) {
                Toast.makeText(this, "⚠️ Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. ✅ VERIFICACIÓN DE BLOQUEO (DESPUÉS DE LLENAR DATOS)
            if (CortexManager.estaBloqueado()) {
                mostrarDialogoSistemaBloqueado(nombre, dni)
                return@setOnClickListener
            }

            // 3. Validaciones de formato
            if (dni.length != 8) {
                etDni.error = "El DNI debe tener 8 dígitos."
                return@setOnClickListener
            }

            if (!esPlacaValida(unidad)) {
                etUnidad.error = "Placa inválida"
                return@setOnClickListener
            }

            // ✅ 4. GENERAR FECHA Y HORA ACTUALES
            val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            // 5. Proceder con autenticación y biometría
            btnSiguiente.isEnabled = false
            btnSiguiente.text = "AUTENTICANDO..."

            CortexManager.autenticarConductorAnonimo(
                onSuccess = {
                    // ✅ CREAR OPERADOR CON FECHA Y HORA
                    CortexManager.operadorActual = Operador(
                        nombre = nombre,
                        dni = dni,
                        empresa = empresa,
                        supervisor = supervisor,
                        equipo = equipoSeleccionado,
                        unidad = unidad,
                        fecha = fechaActual,
                        hora = horaActual
                    )
                    startActivity(Intent(this@MainActivity, BiometriaActivity::class.java))
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                },
                onError = { error ->
                    Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    btnSiguiente.isEnabled = true
                    btnSiguiente.text = "SIGUIENTE ➔"
                }
            )
        }
    }

    private fun mostrarDialogoSistemaBloqueado(nombre: String, dni: String) {
        // ✅ GENERAR FECHA Y HORA PARA SOLICITUD
        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        AlertDialog.Builder(this)
            .setTitle("SISTEMA BLOQUEADO 🔒")
            .setMessage("Hola $nombre, el sistema detecta que no se encuentra apto para realizar la prueba en este momento.\n\n¿Desea enviar una solicitud de desbloqueo al administrador?")
            .setCancelable(false)
            .setPositiveButton("ENVIAR SOLICITUD") { _, _ ->
                // Guardar operador temporal para que la solicitud lleve sus datos
                CortexManager.operadorActual = Operador(
                    nombre = nombre,
                    dni = dni,
                    empresa = "",
                    supervisor = "",
                    equipo = "",
                    unidad = "",
                    fecha = fechaActual,
                    hora = horaActual
                )
                CortexManager.enviarSolicitudDesbloqueo("Solicitud manual desde registro")

                Toast.makeText(this, "🚀 Solicitud enviada correctamente", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LockedActivity::class.java))
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun esPlacaValida(placa: String): Boolean {
        val n = placa.replace(Regex("[\\s-]"), "").uppercase()
        return n.length == 6 && (n.matches(Regex("^[A-Z]{3}[0-9]{3}$")) || n.matches(Regex("^[0-9]{4}[A-Z]{2}$")))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Salir")
            .setMessage("¿Deseas salir de la aplicación?")
            .setPositiveButton("SÍ") { _, _ -> finishAffinity() }
            .setNegativeButton("NO", null)
            .show()
    }
}