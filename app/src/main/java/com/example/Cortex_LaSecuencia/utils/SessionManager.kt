package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "CortexSessionPrefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_KEEP_SESSION = "keep_session"
    }

    // ✅ Guardar sesión después del login exitoso
    fun guardarSesion(email: String, userId: String, mantenerSesion: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_KEEP_SESSION, mantenerSesion)
            apply()
        }
    }

    // ✅ Verificar si hay sesión activa
    fun tieneSesionActiva(): Boolean {
        val sessionGuardada = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val mantenerSesion = prefs.getBoolean(KEY_KEEP_SESSION, false)
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        return sessionGuardada && mantenerSesion && firebaseUser != null
    }

    // ✅ Obtener email del usuario
    fun getEmailUsuario(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // ✅ Obtener ID del usuario
    fun getUserId(): String {
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    // ✅ Cerrar sesión (solo cuando el usuario lo pida explícitamente)
    fun cerrarSesion() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_KEEP_SESSION, false)
            apply()
        }
        FirebaseAuth.getInstance().signOut()
    }

    // ✅ Verificar si debe mantener la sesión
    fun debeMantenerSesion(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SESSION, false)
    }
}