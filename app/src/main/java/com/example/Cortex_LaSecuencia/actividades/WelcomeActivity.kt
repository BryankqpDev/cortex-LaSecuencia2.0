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

        val operador = CortexManager.operadorActual
        val nombreUsuario = operador?.nombre ?: "OPERADOR"
        txtName.text = nombreUsuario.uppercase()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour >= 18 || hour < 5 -> "¡BUENAS NOCHES! 🌙"
            hour >= 12 -> "¡BUENAS TARDES! 🌤️"
            else -> "¡BUENOS DÍAS! ☀️"
        }
        txtGreeting.text = greeting

        AudioManager.hablar("$greeting Hola $nombreUsuario. Iniciemos la verificación.")

        btnStart.setOnClickListener {
            verificarPermisosYComenzar()
        }
    }

    private fun verificarPermisosYComenzar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            iniciarEvaluacion()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
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
                iniciarEvaluacion()
            } else {
                Toast.makeText(
                    this,
                    "⚠️ Acceso a cámara requerido para Sentinel",
                    Toast.LENGTH_LONG
                ).show()

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
        // ═══════════════════════════════════════════════════════════════════
        // ✅ INICIAR CRONÓMETRO: Guardar timestamp de inicio
        // ═══════════════════════════════════════════════════════════════════
        val operador = CortexManager.operadorActual
        if (operador != null && operador.timestampInicio == 0L) {
            operador.timestampInicio = System.currentTimeMillis()
            android.util.Log.d(
                "WelcomeActivity",
                "⏱️ Cronómetro iniciado: ${operador.timestampInicio}"
            )
        }

        // Navegar al primer test
        CortexManager.navegarAlSiguiente(this)
    }
}