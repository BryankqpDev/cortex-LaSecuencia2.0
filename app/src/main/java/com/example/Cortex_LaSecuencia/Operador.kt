package com.example.Cortex_LaSecuencia

data class Operador(
    val nombre: String,
    val dni: String,
    val empresa: String,
    val supervisor: String,
    val equipo: String,
    val unidad: String,
    val fotoPerfil: String? = null,
    val fecha: String = "",
    val hora: String = ""
)