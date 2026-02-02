package com.example.Cortex_LaSecuencia.utils

import java.security.SecureRandom
import kotlin.math.roundToLong

/**
 * ════════════════════════════════════════════════════════════════════════════
 * GENERADOR DE PARÁMETROS ALEATORIOS POR SESIÓN
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Propósito:
 * - Evitar memorización de tiempos y patrones
 * - Garantizar que cada ejecución del test sea única
 * - Mantener comparabilidad científica mediante rangos controlados
 *
 * Arquitectura:
 * - Usa SecureRandom para evitar patrones predecibles
 * - Genera parámetros al inicio de cada test
 * - Registra valores para auditoría (sin mostrarlos al usuario)
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
object TestSessionParams {

    private val secureRandom = SecureRandom()

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t1 - TEST DE REFLEJOS
    // ════════════════════════════════════════════════════════════════════════

    data class ReflejosParams(
        val delayMinMs: Long,           // Delay mínimo antes del estímulo
        val delayMaxMs: Long,           // Delay máximo antes del estímulo
        val umbralEliteMs: Int,         // Tiempo considerado "élite" (era 200ms fijo)
        val umbralPenalizacionMs: Int   // Umbral para penalización (era 500ms fijo)
    )

    /**
     * Genera parámetros aleatorios para el test de reflejos
     *
     * Rangos científicos:
     * - Delay: 1500-4500ms (variación amplia para evitar anticipación)
     * - Umbral élite: 180-220ms (±10% del original)
     * - Umbral penalización: 450-550ms (±10% del original)
     */
    fun generarReflejosParams(): ReflejosParams {
        val delayMin = randomInRange(1500L, 2000L)
        val delayMax = randomInRange(3500L, 4500L)
        val umbralElite = randomInRange(180, 220)
        val umbralPenalizacion = randomInRange(450, 550)

        return ReflejosParams(
            delayMinMs = delayMin,
            delayMaxMs = delayMax,
            umbralEliteMs = umbralElite,
            umbralPenalizacionMs = umbralPenalizacion
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t2 - TEST DE SECUENCIA
    // ════════════════════════════════════════════════════════════════════════

    data class SecuenciaParams(
        val tiempoPorPasoMs: Long,      // Tiempo entre cada paso de la secuencia
        val tiempoPreRespuestaMs: Long  // Delay adicional antes de habilitar respuesta
    )

    /**
     * Genera parámetros aleatorios para el test de secuencia
     *
     * Rangos científicos:
     * - Tiempo por paso: 650-950ms (variación ±18% del original 800ms)
     * - Pre-respuesta: 200-500ms (pausa antes de habilitar input)
     */
    fun generarSecuenciaParams(): SecuenciaParams {
        return SecuenciaParams(
            tiempoPorPasoMs = randomInRange(650L, 950L),
            tiempoPreRespuestaMs = randomInRange(200L, 500L)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t3 - TEST DE ANTICIPACIÓN
    // ════════════════════════════════════════════════════════════════════════

    data class AnticipacionParams(
        val duracionAnimacionMinMs: Long,  // Duración mínima del movimiento
        val duracionAnimacionMaxMs: Long,  // Duración máxima del movimiento
        val factorTolerancia: Float,       // Factor de tolerancia en el scoring
        val tipoAceleracion: Int,          // 0=Normal, 1=Aceleración, 2=Desaceleración
        val factorVelocidadBase: Float     // Multiplicador de velocidad base (>1 = más rápido)
    )

    /**
     * Genera parámetros aleatorios para el test de anticipación
     *
     * Rangos científicos MEJORADOS (más desafiante):
     * - Duración: 600-1400ms (MÁS RÁPIDO que antes: 900-2200ms)
     * - Tolerancia: 0.85-1.15 (ajuste dinámico del scoring)
     * - Aceleración: Variable (normal, acelera, desacelera)
     * - Velocidad base: 1.2-1.8x (20-80% más rápido)
     *
     * Tipos de aceleración:
     * 0 = Velocidad constante (lineal)
     * 1 = Empieza lento, termina RÁPIDO (aceleración)
     * 2 = Empieza RÁPIDO, termina lento (desaceleración)
     */
    fun generarAnticipacionParams(): AnticipacionParams {
        return AnticipacionParams(
            // Rangos más cortos = velocidad base más alta
            duracionAnimacionMinMs = randomInRange(600L, 800L),
            duracionAnimacionMaxMs = randomInRange(1000L, 1400L),
            factorTolerancia = randomFloatInRange(0.85f, 1.15f),
            // Tipo de aceleración aleatorio
            tipoAceleracion = randomInRange(0, 2),
            // Velocidad base aumentada 20-80% más que el original
            factorVelocidadBase = randomFloatInRange(1.2f, 1.8f)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t4 - TEST DE COORDINACIÓN
    // ════════════════════════════════════════════════════════════════════════

    data class CoordinacionParams(
        val tiempoBaseMs: Long,         // Tiempo base para scoring
        val factorDificultad: Float     // Multiplicador de dificultad
    )

    /**
     * Genera parámetros aleatorios para el test de coordinación
     *
     * Rangos científicos:
     * - Tiempo base: 2500-3500ms
     * - Factor dificultad: 0.9-1.1 (afecta la velocidad de aparición)
     */
    fun generarCoordinacionParams(): CoordinacionParams {
        return CoordinacionParams(
            tiempoBaseMs = randomInRange(2500L, 3500L),
            factorDificultad = randomFloatInRange(0.9f, 1.1f)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t6 - TEST DE ESCANEO VISUAL
    // ════════════════════════════════════════════════════════════════════════

    data class EscaneoParams(
        val tiempoBaseMs: Long,         // Tiempo base para scoring
        val factorPenalizacion: Float   // Factor de penalización por tiempo
    )

    /**
     * Genera parámetros aleatorios para el test de escaneo
     *
     * Rangos científicos:
     * - Tiempo base: 1300-1700ms
     * - Factor penalización: 0.9-1.1
     */
    fun generarEscaneoParams(): EscaneoParams {
        return EscaneoParams(
            tiempoBaseMs = randomInRange(1300L, 1700L),
            factorPenalizacion = randomFloatInRange(0.9f, 1.1f)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t7 - TEST DE CONTROL DE IMPULSO
    // ════════════════════════════════════════════════════════════════════════

    data class ImpulsoParams(
        val duracionEstimuloMs: Long,   // Duración del estímulo en pantalla
        val delayEntreRondasMs: Long    // Delay entre rondas
    )

    /**
     * Genera parámetros aleatorios para el test de impulso
     *
     * Rangos científicos:
     * - Duración estímulo: 750-1050ms
     * - Delay entre rondas: 150-300ms
     */
    fun generarImpulsoParams(): ImpulsoParams {
        return ImpulsoParams(
            duracionEstimuloMs = randomInRange(750L, 1050L),
            delayEntreRondasMs = randomInRange(150L, 300L)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARÁMETROS PARA t8 - TEST DE RASTREO VISUAL
    // ════════════════════════════════════════════════════════════════════════

    data class RastreoParams(
        val velocidadMinBolas: Float,   // Velocidad mínima de las bolas
        val velocidadMaxBolas: Float,   // Velocidad máxima de las bolas
        val duracionAnimacionPasos: Int // Duración total en pasos de animación
    )

    /**
     * Genera parámetros aleatorios para el test de rastreo
     *
     * Rangos científicos:
     * - Velocidad: 0.012-0.035 (variación amplia)
     * - Duración: 250-350 pasos (era 300 fijo)
     */
    fun generarRastreoParams(): RastreoParams {
        return RastreoParams(
            velocidadMinBolas = randomFloatInRange(0.012f, 0.018f),
            velocidadMaxBolas = randomFloatInRange(0.025f, 0.035f),
            duracionAnimacionPasos = randomInRange(250, 350)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES DE RANDOMIZACIÓN (PÚBLICAS PARA USO EN ACTIVITIES)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Genera un Long aleatorio en el rango [min, max]
     * Público para permitir variaciones dentro de cada test
     */
    fun randomInRange(min: Long, max: Long): Long {
        val range = max - min
        return min + (secureRandom.nextDouble() * range).roundToLong()
    }

    /**
     * Genera un Int aleatorio en el rango [min, max]
     * Público para permitir variaciones dentro de cada test
     */
    fun randomInRange(min: Int, max: Int): Int {
        return min + secureRandom.nextInt(max - min + 1)
    }

    /**
     * Genera un Float aleatorio en el rango [min, max]
     * Público para permitir variaciones dentro de cada test
     */
    fun randomFloatInRange(min: Float, max: Float): Float {
        return min + secureRandom.nextFloat() * (max - min)
    }

    // ════════════════════════════════════════════════════════════════════════
    // REGISTRO DE AUDITORÍA
    // ════════════════════════════════════════════════════════════════════════

    private val auditLog = mutableMapOf<String, String>()

    /**
     * Registra los parámetros usados en un test para auditoría
     */
    fun registrarParametros(testId: String, params: Any) {
        auditLog[testId] = params.toString()
    }

    /**
     * Obtiene el log de auditoría (para uso interno/debugging)
     */
    fun obtenerAuditLog(): Map<String, String> = auditLog.toMap()

    /**
     * Limpia el log de auditoría (al finalizar la batería completa)
     */
    fun limpiarAuditLog() {
        auditLog.clear()
    }
}