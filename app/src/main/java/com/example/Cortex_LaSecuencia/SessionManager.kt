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

    fun guardarSesion(email: String, userId: String, mantenerSesion: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_KEEP_SESSION, mantenerSesion)
            apply()
        }
    }

    fun tieneSesionActiva(): Boolean {
        val firebaseUser = auth.currentUser
        val sessionLocal = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val mantenerSesion = prefs.getBoolean(KEY_KEEP_SESSION, true)
        return firebaseUser != null && sessionLocal && mantenerSesion
    }

    fun getEmailUsuario(): String {
        return auth.currentUser?.email ?: prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun cerrarSesion() {
        auth.signOut()
        prefs.edit().clear().apply()
    }
}