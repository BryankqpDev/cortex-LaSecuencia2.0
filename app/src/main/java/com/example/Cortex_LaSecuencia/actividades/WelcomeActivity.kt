package com.example.Cortex_LaSecuencia.actividades

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import java.util.*

class WelcomeActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val txtGreeting = findViewById<TextView>(R.id.wel_greeting)
        val txtName = findViewById<TextView>(R.id.wel_name)
        val btnStart = findViewById<Button>(R.id.btn_start_eval)

        // Obtener nombre del operador
        val operador = CortexManager.operadorActual
        val nombreUsuario = operador?.nombre ?: "OPERADOR"
        txtName.text = nombreUsuario.uppercase()

        // Saludo según hora del día (como en HTML)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour >= 18 || hour < 5 -> "¡BUENAS NOCHES! 🌙"
            hour >= 12 -> "¡BUENAS TARDES! 🌤️"
            else -> "¡BUENOS DÍAS! ☀️"
        }
        txtGreeting.text = greeting
        
        // Hablar saludo (como en HTML: speak)
        AudioManager.hablar("$greeting Hola $nombreUsuario. Iniciemos la verificación.")

        // Botón para iniciar evaluación (como en HTML: verifica cámara primero)
        btnStart.setOnClickListener {
            verificarPermisosYComenzar()
        }
    }

    private fun verificarPermisosYComenzar() {
        // Verificar si ya tenemos permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // Permisos otorgados, iniciar evaluación
            iniciarEvaluacion()
        } else {
            // Solicitar permisos (como en HTML: muestra mensaje si no se otorgan)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Mostrar explicación
                AlertDialog.Builder(this)
                    .setTitle("Acceso a Cámara Requerido")
                    .setMessage("Sentinel requiere acceso a la cámara para monitorear tu presencia durante los tests. Es necesario para garantizar la seguridad del proceso.")
                    .setPositiveButton("OTORGAR") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("CANCELAR", null)
                    .show()
            } else {
                // Solicitar permisos directamente
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, iniciar evaluación
                iniciarEvaluacion()
            } else {
                // Permiso denegado (como en HTML: muestra mensaje y botón de reintento)
                Toast.makeText(
                    this,
                    "⚠️ Acceso a cámara requerido para Sentinel",
                    Toast.LENGTH_LONG
                ).show()
                
                // Mostrar botón de reintento (similar al HTML)
                AlertDialog.Builder(this)
                    .setTitle("Cámara Requerida")
                    .setMessage("Sentinel necesita acceso a la cámara para operar. Por favor, otorga el permiso en Configuración o presiona 'ACTIVAR CÁMARA MANUALMENTE'.")
                    .setPositiveButton("ACTIVAR CÁMARA MANUALMENTE") { _, _ ->
                        verificarPermisosYComenzar()
                    }
                    .setNegativeButton("CANCELAR", null)
                    .show()
            }
        }
    }

    private fun iniciarEvaluacion() {
        // Navegar al primer test usando CortexManager (como en HTML: startTestSequence)
        CortexManager.navegarAlSiguiente(this)
    }
}