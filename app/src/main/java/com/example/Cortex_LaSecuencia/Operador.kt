package com.example.Cortex_LaSecuencia

data class Operador(
    val empresa: String,
    val supervisor: String,
    val nombre: String,
    val dni: String,
    val tipoEquipo: String,
    val unidad: String,
    val fotoBase64: String? = null
)