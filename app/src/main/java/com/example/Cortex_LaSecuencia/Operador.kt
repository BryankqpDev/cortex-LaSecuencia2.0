package com.example.Cortex_LaSecuencia

/**
 * ════════════════════════════════════════════════════════════════════════════
 * DATA CLASS OPERADOR - CON TRACKING DE TIEMPO
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Nuevos campos agregados:
 * - timestampInicio: Momento exacto en que empezó el primer test
 * - tiempoTotalSegundos: Tiempo total que tomó completar la batería
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
data class Operador(
    val nombre: String,
    val dni: String,
    val empresa: String,
    val supervisor: String,
    val equipo: String,
    val unidad: String,
    val fotoPerfil: String? = null,
    val fecha: String = "",
    val hora: String = "",

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ NUEVOS CAMPOS PARA TRACKING DE TIEMPO
    // ═══════════════════════════════════════════════════════════════════════
    var timestampInicio: Long = 0L,        // Timestamp cuando empieza el primer test
    var tiempoTotalSegundos: Int = 0       // Tiempo total en segundos al finalizar
)