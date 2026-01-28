package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.CortexManager
import java.text.SimpleDateFormat
import java.util.*

class LockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        val txtUnlockTime = findViewById<TextView>(R.id.txt_unlock_time)
        val etSupervisorCode = findViewById<EditText>(R.id.et_supervisor_code)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        // Mostrar tiempo de desbloqueo
        val unlockTime = CortexManager.obtenerTiempoDesbloqueo()
        if (unlockTime > 0) {
            val date = Date(unlockTime)
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            txtUnlockTime.text = "Disponible: ${formatter.format(date)}"
        }

        btnUnlock.setOnClickListener {
            val codigo = etSupervisorCode.text.toString()
            if (CortexManager.verificarCodigoSupervisor(codigo)) {
                CortexManager.desbloquearSistema(this)
                Toast.makeText(this, "BLOQUEO LEVANTADO", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "CÓDIGO INCORRECTO", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
