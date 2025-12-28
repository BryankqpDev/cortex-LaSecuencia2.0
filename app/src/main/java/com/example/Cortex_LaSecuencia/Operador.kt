package com.example.Cortex_LaSecuencia

// Esta clase guarda el perfil del trabajador evaluado
data class Operador(
    val nombre: String,
    val dni: String,
    val empresa: String,
    val supervisor: String,
    val equipo: String,
    val unidad: String,
    val fotoPerfil: String? = null // Para la captura de Sentinel
)