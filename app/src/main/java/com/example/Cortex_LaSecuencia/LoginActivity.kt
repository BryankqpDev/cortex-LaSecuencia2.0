package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var txtForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        initViews()
        setupListeners()
    }

    private fun initViews() {
        inputEmail = findViewById(R.id.input_email)
        inputPassword = findViewById(R.id.input_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
        txtForgotPassword = findViewById(R.id.txt_forgot_password)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener { loginUser() }
        btnRegister.setOnClickListener { registerUser() }
        txtForgotPassword.setOnClickListener { resetPassword() }
    }

    private fun loginUser() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (!validateInputs(email, password)) return
        setLoadingState(true, "Iniciando...")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoadingState(false, "INICIAR SESIÓN")
                if (task.isSuccessful) {
                    CortexManager.usuarioAdmin = auth.currentUser
                    Toast.makeText(this, "Acceso concedido", Toast.LENGTH_SHORT).show()
                    navigateToAdmin()
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Error desconocido"
                    Log.e("LOGIN_ERROR", "Fallo al iniciar sesión", task.exception)
                    Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (!validateInputs(email, password)) return
        setLoadingState(true, "Registrando...")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoadingState(false, "CREAR CUENTA ADMIN")
                if (task.isSuccessful) {
                    CortexManager.usuarioAdmin = auth.currentUser
                    Toast.makeText(this, "Cuenta Admin creada con éxito", Toast.LENGTH_LONG).show()
                    navigateToAdmin()
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Error desconocido"
                    Log.e("REGISTER_ERROR", "Fallo al registrar usuario", task.exception)
                    // Mostrar un mensaje más amigable para el error CONHI
                    if (errorMsg.contains("CONHI")) {
                        Toast.makeText(this, "Error de conexión con Firebase. Verifica la consola y los servicios de Google.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al registrar: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.error = "Ingresa un email válido"
            return false
        }
        if (password.length < 6) {
            inputPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        return true
    }

    private fun resetPassword() {
        val email = inputEmail.text.toString().trim()
        if (email.isEmpty()) {
            inputEmail.error = "Ingresa tu email"
            return
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_LONG).show()
            } else {
                Log.e("RESET_ERROR", "Fallo al enviar correo", task.exception)
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean, message: String) {
        btnLogin.isEnabled = !isLoading
        btnRegister.isEnabled = !isLoading
        if (isLoading) btnLogin.text = message else btnLogin.text = "INICIAR SESIÓN"
    }

    private fun navigateToAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
