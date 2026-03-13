package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.SessionManager
import com.example.Cortex_LaSecuencia.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        sessionManager = SessionManager(this)

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.input_email)
        val etPassword = findViewById<EditText>(R.id.input_password)
        val cbMantenerSesion = findViewById<CheckBox>(R.id.cb_mantener_sesion)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegistrar = findViewById<Button>(R.id.btn_register)

        cbMantenerSesion.isChecked = true

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val mantenerSesion = cbMantenerSesion.isChecked

            if (validarCampos(email, password)) {
                iniciarSesion(email, password, mantenerSesion)
            }
        }

        btnRegistrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validarCampos(email, password)) {
                registrarUsuario(email, password)
            }
        }
    }

    private fun validarCampos(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Ingrese el correo", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Ingrese la contraseña", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun iniciarSesion(email: String, password: String, mantenerSesion: Boolean) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        sessionManager.guardarSesion(user.email ?: email, user.uid, mantenerSesion)
                        Toast.makeText(this, "✅ Bienvenido", Toast.LENGTH_SHORT).show()
                        irAAdminActivity()
                    }
                } else {
                    Toast.makeText(this, "❌ Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registrarUsuario(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        sessionManager.guardarSesion(user.email ?: email, user.uid, true)
                        Toast.makeText(this, "✅ Registro exitoso", Toast.LENGTH_SHORT).show()
                        irAAdminActivity()
                    }
                } else {
                    Toast.makeText(this, "❌ Error al registrar", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun irAAdminActivity() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
