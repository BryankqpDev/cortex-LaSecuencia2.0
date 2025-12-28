package com.example.Cortex_LaSecuencia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val nombreUsuario = intent.getStringExtra("USER_NAME") ?: "OPERADOR"
        val txtName = findViewById<TextView>(R.id.wel_name)
        val btnStart = findViewById<Button>(R.id.btn_start_eval)

        txtName.text = nombreUsuario.uppercase()

        // 4. Configurar el botón con validación de Sentinel (Cámara)
        btnStart.setOnClickListener {
            verificarPermisosYComenzar()
        }
    }


    private fun verificarPermisosYComenzar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // Si ya tenemos permiso, iniciamos el test
            iniciarTestReflejos()
        } else {
            // Si no, pedimos permiso al operador
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun iniciarTestReflejos() {
        Toast.makeText(this, "SENTINEL ACTIVADO: Iniciando Test 1", Toast.LENGTH_SHORT).show()
        // Aquí es donde haremos el salto a la pantalla del Test de Reflejos
    }

    // Resultado de la pregunta de permiso
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarTestReflejos()
        } else {
            Toast.makeText(this, "Sentinel requiere cámara para operar", Toast.LENGTH_LONG).show()
        }
    }
}