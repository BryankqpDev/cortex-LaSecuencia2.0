package com.example.Cortex_LaSecuencia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // 1. Obtener el nombre enviado desde MainActivity
        val nombreUsuario = intent.getStringExtra("USER_NAME") ?: "OPERADOR"

        // 2. Vincular con el diseño XML
        val txtName = findViewById<TextView>(R.id.wel_name)
        val btnStart = findViewById<Button>(R.id.btn_start_eval)

        // 3. Mostrar el nombre en pantalla (en mayúsculas)
        txtName.text = nombreUsuario.uppercase()

        // 4. Configurar el botón para el siguiente paso
        btnStart.setOnClickListener {
            // Aquí iniciaremos la secuencia de tests más adelante
        }
    }
}