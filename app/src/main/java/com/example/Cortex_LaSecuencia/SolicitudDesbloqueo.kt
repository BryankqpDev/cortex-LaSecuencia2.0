package com.example.Cortex_LaSecuencia

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SolicitudDesbloqueo(
    val dni: String = "",
    val nombre: String = "",
    val fecha: String = "",
    val motivo: String = "",
    val estado: String = "pendiente", // 'pendiente', 'autorizado', 'rechazado'
    val timestamp: Long = 0
)