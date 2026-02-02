package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.MainActivity
import com.example.Cortex_LaSecuencia.SolicitudDesbloqueo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LockedActivity : AppCompatActivity() {

    private var listenerDesbloqueo: ValueEventListener? = null
    private var dniBloqueado: String? = null
    private lateinit var txtUnlockTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        txtUnlockTime = findViewById(R.id.txt_unlock_time)
        val etSupervisorCode = findViewById<EditText>(R.id.et_supervisor_code)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        // 1. Iniciar Cuenta Regresiva
        gestionarTemporizador()

        // 2. Obtener el DNI (incluso si la app se reinició)
        dniBloqueado = CortexManager.operadorActual?.dni ?: CortexManager.obtenerDniBloqueado()

        // 3. Escuchar Firebase si tenemos un DNI válido
        if (!dniBloqueado.isNullOrEmpty()) {
            escucharAutorizacionRemota(dniBloqueado!!)
        } else {
            // Si no hay DNI, es raro, pero mostramos aviso
            Toast.makeText(this, "Esperando desbloqueo manual...", Toast.LENGTH_SHORT).show()
        }

        // 4. Botón de Desbloqueo Manual (Código Supervisor)
        btnUnlock.setOnClickListener {
            val codigo = etSupervisorCode.text.toString()
            if (CortexManager.verificarCodigoSupervisor(codigo)) {
                liberarYSalir("🔓 BLOQUEO LEVANTADO MANUALMENTE")
            } else {
                Toast.makeText(this, "⛔ CÓDIGO INCORRECTO", Toast.LENGTH_SHORT).show()
                etSupervisorCode.setText("")
            }
        }
    }

    private fun gestionarTemporizador() {
        val tiempoDesbloqueo = CortexManager.obtenerTiempoDesbloqueo()
        val tiempoRestante = tiempoDesbloqueo - System.currentTimeMillis()

        if (tiempoRestante > 0) {
            // Mostramos fecha de desbloqueo
            val date = Date(tiempoDesbloqueo)
            val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            // Iniciamos cuenta regresiva visual
            object : CountDownTimer(tiempoRestante, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val horas = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                    val minutos = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                    val segundos = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

                    txtUnlockTime.text = String.format("Disponible: %s\n(%02d:%02d:%02d)",
                        formatter.format(date), horas, minutos, segundos)
                }

                override fun onFinish() {
                    txtUnlockTime.text = "¡TIEMPO FINALIZADO!"
                    verificarSiYaPasoElTiempo()
                }
            }.start()
        } else {
            verificarSiYaPasoElTiempo()
        }
    }

    private fun verificarSiYaPasoElTiempo() {
        if (!CortexManager.estaBloqueado()) {
            liberarYSalir("⏳ TIEMPO DE ESPERA FINALIZADO")
        }
    }

    private fun escucharAutorizacionRemota(dni: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo").child(dni)

        listenerDesbloqueo = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Usamos la clase SolicitudDesbloqueo (que está en CortexManager o su archivo propio)
                val solicitud = snapshot.getValue(SolicitudDesbloqueo::class.java)

                if (solicitud != null) {
                    when (solicitud.estado) {
                        "autorizado" -> liberarYSalir("✅ DESBLOQUEADO POR ADMINISTRADOR")
                        "rechazado" -> {
                            // Opcional: Avisar al usuario pero mantener bloqueo
                            Toast.makeText(applicationContext, "❌ Solicitud rechazada", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Error silencioso de red
            }
        }

        dbRef.addValueEventListener(listenerDesbloqueo!!)
    }

    private fun liberarYSalir(mensaje: String) {
        // 1. Limpiar preferencias y borrar solicitud de Firebase
        CortexManager.desbloquearSistema(this)

        // 2. Feedback usuario
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // 3. Volver al inicio limpiando la pila de actividades
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpieza vital para evitar fugas de memoria y crashes
        if (dniBloqueado != null && listenerDesbloqueo != null) {
            FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo")
                .child(dniBloqueado!!)
                .removeEventListener(listenerDesbloqueo!!)
        }
    }

    // Bloquear botón "Atrás" para que no puedan salir de la pantalla
    override fun onBackPressed() {
        Toast.makeText(this, "Sistema bloqueado. Espere autorización.", Toast.LENGTH_SHORT).show()
        // No llamamos a super.onBackPressed()
    }
}