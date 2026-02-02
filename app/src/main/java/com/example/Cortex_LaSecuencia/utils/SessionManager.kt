package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "CortexSessionPrefs",
        Context.MODE_PRIVATE
    )

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
            apply() // Guarda de forma asíncrona
        }

        // 🔥 DEBUG: Verificar que se guardó
        android.util.Log.d("SessionManager", "Sesión guardada: $email, mantener=$mantenerSesion")
    }

    // ✅ Verificar si hay sesión activa (Persistencia real)
    fun tieneSesionActiva(): Boolean {
        val firebaseUser = auth.currentUser
        val sessionLocal = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val mantenerSesion = prefs.getBoolean(KEY_KEEP_SESSION, true) // ✅ Por defecto TRUE

        val tieneSeion = firebaseUser != null && sessionLocal && mantenerSesion

        // 🔥 DEBUG
        android.util.Log.d("SessionManager", "Verificando sesión:")
        android.util.Log.d("SessionManager", "  Firebase user: ${firebaseUser?.email}")
        android.util.Log.d("SessionManager", "  Local logged in: $sessionLocal")
        android.util.Log.d("SessionManager", "  Mantener sesión: $mantenerSesion")
        android.util.Log.d("SessionManager", "  Resultado: $tieneSeion")

        return tieneSeion
    }

    // ✅ Obtener email del usuario
    fun getEmailUsuario(): String {
        val firebaseEmail = auth.currentUser?.email
        if (firebaseEmail != null) {
            return firebaseEmail
        }
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // ✅ Obtener ID del usuario
    fun getUserId(): String {
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    // ✅ Cerrar sesión completamente
    fun cerrarSesion() {
        android.util.Log.d("SessionManager", "Cerrando sesión...")
        auth.signOut()
        prefs.edit().clear().apply()
    }
}