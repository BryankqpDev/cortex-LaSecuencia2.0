package com.example.Cortex_LaSecuencia.logic

import com.example.Cortex_LaSecuencia.utils.TestSessionParams

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SISTEMA DE SCORING ADAPTATIVO
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Propósito:
 * - Ajustar los umbrales de puntaje según la dificultad generada aleatoriamente
 * - Mantener equidad: si el test fue más difícil, se "perdona" más tiempo
 * - Evitar que la randomización penalice injustamente al usuario
 *
 * Principio científico:
 * Si el delay fue más largo → el usuario necesita más tiempo para reaccionar
 * Si la velocidad fue mayor → se permite mayor margen de error
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
object AdaptiveScoring {

    // ════════════════════════════════════════════════════════════════════════
    // SCORING PARA t1 - TEST DE REFLEJOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el puntaje adaptativo para el test de reflejos
     *
     * @param tiempoReaccionMs Tiempo de reacción del usuario en milisegundos
     * @param params Parámetros generados para esta sesión
     * @return Puntaje de 0-100
     *
     * Lógica:
     * - Los umbrales se ajustan según los valores generados aleatoriamente
     * - Si el umbralElite fue 220ms en vez de 200ms → se ajusta la fórmula
     * - Si el umbralPenalizacion fue 550ms en vez de 500ms → se perdona más
     */
    fun calcularPuntajeReflejos(
        tiempoReaccionMs: Long,
        params: TestSessionParams.ReflejosParams
    ): Int {
        return if (tiempoReaccionMs > params.umbralPenalizacionMs) {
            // Zona de penalización (reacción lenta)
            // Por cada 5ms por encima del umbral, se resta 1 punto
            maxOf(0, 100 - ((tiempoReaccionMs - params.umbralPenalizacionMs) / 5).toInt())
        } else {
            // Zona de élite (reacción rápida)
            // Penaliza si es demasiado rápido (posible anticipación)
            100 - maxOf(0, ((tiempoReaccionMs - params.umbralEliteMs) / 5).toInt())
        }.coerceIn(0, 100)
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCORING PARA t3 - TEST DE ANTICIPACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el puntaje adaptativo para el test de anticipación
     *
     * @param distanciaPx Distancia en píxeles entre el centro del vehículo y la meta
     * @param anchoPistaPx Ancho total de la pista en píxeles
     * @param params Parámetros generados para esta sesión
     * @return Puntaje de 0-100
     *
     * Lógica:
     * - Si la animación fue más rápida → se aplica el factor de tolerancia
     * - El factor permite ajustar dinámicamente la severidad de la penalización
     */
    fun calcularPuntajeAnticipacion(
        distanciaPx: Float,
        anchoPistaPx: Int,
        params: TestSessionParams.AnticipacionParams
    ): Int {
        val diferenciaPorcentual = (distanciaPx / anchoPistaPx) * 100

        // Aplicar factor de tolerancia: si fue más difícil, se penaliza menos
        val penalizacionBase = (diferenciaPorcentual * 6).toInt()
        val penalizacionAjustada = (penalizacionBase / params.factorTolerancia).toInt()

        return (100 - penalizacionAjustada).coerceIn(0, 100)
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCORING PARA t4 - TEST DE COORDINACIÓN
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el puntaje adaptativo para el test de coordinación
     *
     * @param tiempoTotalMs Tiempo total que tomó completar el test
     * @param params Parámetros generados para esta sesión
     * @return Puntaje de 0-100
     *
     * Lógica:
     * - El tiempo base se ajusta según los parámetros aleatorios
     * - Si el factor de dificultad fue mayor → se permite más tiempo
     */
    fun calcularPuntajeCoordinacion(
        tiempoTotalMs: Long,
        params: TestSessionParams.CoordinacionParams
    ): Int {
        // Ajustar el tiempo base según el factor de dificultad
        val tiempoBaseAjustado = (params.tiempoBaseMs * params.factorDificultad).toLong()

        if (tiempoTotalMs <= tiempoBaseAjustado) return 100

        val penalizacion = ((tiempoTotalMs - tiempoBaseAjustado) / 50).toInt()
        return (100 - penalizacion).coerceIn(0, 100)
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCORING PARA t6 - TEST DE ESCANEO VISUAL
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el puntaje adaptativo para el test de escaneo visual
     *
     * @param tiempoTotalMs Tiempo total que tomó encontrar el número
     * @param params Parámetros generados para esta sesión
     * @return Puntaje de 0-100
     *
     * Lógica:
     * - El tiempo base y el factor de penalización se ajustan dinámicamente
     */
    fun calcularPuntajeEscaneo(
        tiempoTotalMs: Long,
        params: TestSessionParams.EscaneoParams
    ): Int {
        if (tiempoTotalMs <= params.tiempoBaseMs) return 100

        val penalizacionBase = ((tiempoTotalMs - params.tiempoBaseMs) / 50).toInt()
        val penalizacionAjustada = (penalizacionBase * params.factorPenalizacion).toInt()

        return (100 - penalizacionAjustada).coerceIn(0, 100)
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES GENERALES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calcula un puntaje base estándar (para tests sin timing crítico)
     *
     * @param aciertos Número de respuestas correctas
     * @param total Número total de intentos
     * @return Puntaje de 0-100
     */
    fun calcularPuntajeEstandar(aciertos: Int, total: Int): Int {
        if (total == 0) return 0
        return ((aciertos.toFloat() / total) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Aplica la penalización por ausencia al puntaje final
     *
     * @param puntajeBase Puntaje calculado antes de penalizaciones
     * @param penalizacionAusencia Penalización acumulada por ausencias
     * @return Puntaje final ajustado
     */
    fun aplicarPenalizacionAusencia(puntajeBase: Int, penalizacionAusencia: Int): Int {
        return (puntajeBase - penalizacionAusencia).coerceIn(0, 100)
    }
}