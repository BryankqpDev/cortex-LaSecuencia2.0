package com.example.Cortex_LaSecuencia

data class Operador(
    val nombre: String,
    val dni: String,
    val empresa: String,
    val supervisor: String,
    val equipo: String, // Aquí guardaremos lo que elija del Spinner
    val unidad: String,
    val fotoPerfil: String? = null, // Para Sentinel (Cámara)

    // Agregamos fecha y hora para el Excel de Admin
    val fecha: String = "",
    val hora: String = ""
)