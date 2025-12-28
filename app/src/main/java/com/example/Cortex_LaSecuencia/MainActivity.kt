package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Esto conecta este código con tu archivo XML
        setContentView(R.layout.activity_main)

        // 1. Identificar los componentes del diseño
        val inputEmpresa = findViewById<EditText>(R.id.in_company)
        val inputNombre = findViewById<EditText>(R.id.in_name)
        val inputDni = findViewById<EditText>(R.id.in_dni)
        val botonSiguiente = findViewById<Button>(R.id.btn_start_eval)

        // 2. Configurar la acción del botón (el "onclick" de tu HTML)
        botonSiguiente.setOnClickListener {
            val empresa = inputEmpresa.text.toString()
            val nombre = inputNombre.text.toString()
            val dni = inputDni.text.toString()

            // Validación simple (como en tu JS)
            if (empresa.isEmpty() || nombre.isEmpty() || dni.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Crear el objeto Operador con los datos ingresados
                val nuevoOperador = Operador(
                    empresa = empresa,
                    supervisor = "PENDIENTE", // Lo completaremos luego
                    nombre = nombre,
                    dni = dni,
                    tipoEquipo = "POR DEFINIR",
                    unidad = "S/N"


                )

                // Mensaje de éxito
                Toast.makeText(this, "Bienvenido ${nuevoOperador.nombre}", Toast.LENGTH_LONG).show()

                // AQUÍ iría el paso a la siguiente pantalla (Welcome)
                // Esto crea un "puente" hacia la siguiente pantalla
                val intent = Intent(this, WelcomeActivity::class.java)
// Pasamos el nombre para que la siguiente pantalla salude
                intent.putExtra("USER_NAME", nombre)
                startActivity(intent)
            }
        }
    }
}