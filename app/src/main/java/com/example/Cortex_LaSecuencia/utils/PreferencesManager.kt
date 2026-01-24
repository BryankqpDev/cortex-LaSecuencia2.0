package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "CortexEmailPrefs",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val KEY_EMAILS_DESTINO = "emails_destino"
        private const val KEY_ENVIO_AUTO = "envio_automatico"
    }

    // ✅ Guardar múltiples emails
    fun setEmailsDestino(emails: List<String>) {
        val json = gson.toJson(emails)
        prefs.edit().putString(KEY_EMAILS_DESTINO, json).apply()
    }

    // ✅ Obtener lista de emails
    fun getEmailsDestino(): List<String> {
        val json = prefs.getString(KEY_EMAILS_DESTINO, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // ✅ Activar/Desactivar envío automático
    fun setEnvioAutomatico(activo: Boolean) {
        prefs.edit().putBoolean(KEY_ENVIO_AUTO, activo).apply()
    }

    // ✅ Verificar si está activo el envío automático
    fun isEnvioAutomaticoActivo(): Boolean {
        return prefs.getBoolean(KEY_ENVIO_AUTO, false)
    }

    // ✅ Limpiar todas las preferencias
    fun limpiarPreferencias() {
        prefs.edit().clear().apply()
    }
}