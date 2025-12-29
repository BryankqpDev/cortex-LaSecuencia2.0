package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Importante para la alerta de Admin
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.actividades.WelcomeActivity // Si decides usarla después
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Asegúrate que tu XML se llame así

        // 1. Identificar los componentes (Usando los IDs de tu NUEVO XML)
        val etEmpresa = findViewById<EditText>(R.id.et_empresa)
        val etSupervisor = findViewById<EditText>(R.id.et_supervisor)
        val etNombre = findViewById<EditText>(R.id.et_nombre)
        val etDni = findViewById<EditText>(R.id.et_dni)
        val etUnidad = findViewById<EditText>(R.id.et_unidad)
        val spinnerEquipo = findViewById<Spinner>(R.id.spinner_equipo)

        val btnSiguiente = findViewById<Button>(R.id.btn_siguiente)
        val btnAdmin = findViewById<Button>(R.id.btn_admin)

        // 2. Configurar el botón SIGUIENTE
        btnSiguiente.setOnClickListener {
            // Obtener texto limpio
            val empresa = etEmpresa.text.toString().trim().uppercase()
            val supervisor = etSupervisor.text.toString().trim().uppercase()
            val nombre = etNombre.text.toString().trim().uppercase()
            val dni = etDni.text.toString().trim()
            val unidad = etUnidad.text.toString().trim().uppercase()

            // Obtener selección del Spinner (Desplegable)
            val equipoSeleccionado = spinnerEquipo.selectedItem.toString()

            // Validaciones
            if (empresa.isEmpty() || supervisor.isEmpty() || nombre.isEmpty() || dni.isEmpty()) {
                Toast.makeText(this, "⚠️ Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (equipoSeleccionado.startsWith("-")) {
                Toast.makeText(this, "⚠️ Seleccione un equipo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Capturar Fecha y Hora (Para Admin/Data)
            val now = Date()
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
            val horaHoy = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

            // Crear el objeto Operador completo
            val nuevoOperador = Operador(
                nombre = nombre,
                dni = dni,
                empresa = empresa,
                supervisor = supervisor,
                equipo = equipoSeleccionado,
                unidad = unidad,
                fecha = fechaHoy, // Vital para el Excel
                hora = horaHoy
            )

            // Guardar en el Cerebro de la App
            CortexManager.operadorActual = nuevoOperador

            Toast.makeText(this, "Bienvenido, $nombre", Toast.LENGTH_SHORT).show()

            // Iniciar la evaluación
            // Opción A: Ir directo a los tests (Recomendado ahora)
            CortexManager.navegarAlSiguiente(this)

            // Opción B: Ir a la WelcomeActivity (Si ya la tienes lista)
            // val intent = Intent(this, WelcomeActivity::class.java)
            // startActivity(intent)
        }

        // 3. Configurar el botón ADMIN (Con contraseña)
        btnAdmin.setOnClickListener {
            mostrarDialogoAdmin()
        }
    }

    // Función auxiliar para pedir contraseña
    private fun mostrarDialogoAdmin() {
        val inputPassword = EditText(this)
        inputPassword.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        inputPassword.hint = "Código (1007)"
        inputPassword.gravity = android.view.Gravity.CENTER

        // Ajuste visual para que se vea bien en fondo oscuro
        inputPassword.setTextColor(android.graphics.Color.BLACK)

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        inputPassword.layoutParams = params
        container.addView(inputPassword)

        AlertDialog.Builder(this)
            .setTitle("ACCESO SUPERVISOR 🔒")
            .setMessage("Ingrese código de seguridad:")
            .setView(container)
            .setPositiveButton("ENTRAR") { _, _ ->
                val codigo = inputPassword.text.toString()
                if (codigo == "1007") {
                    Toast.makeText(this, "Acceso Autorizado ✅", Toast.LENGTH_SHORT).show()

                    // --- AQUÍ CONECTAS LA NUEVA PANTALLA ---
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)

                } else {
                    Toast.makeText(this, "⛔ Código Incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }
}