package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.Cortex_LaSecuencia.actividades.AnticipacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity
import com.example.Cortex_LaSecuencia.actividades.CoordinacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.AtencionTestActivity
import com.example.Cortex_LaSecuencia.actividades.EscaneoTestActivity
import com.example.Cortex_LaSecuencia.actividades.ImpulsoTestActivity
import com.example.Cortex_LaSecuencia.actividades.RastreoTestActivity
import com.example.Cortex_LaSecuencia.actividades.EspacialTestActivity
import com.example.Cortex_LaSecuencia.actividades.DecisionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReporteFinalActivity
import com.example.Cortex_LaSecuencia.actividades.IntroTestActivity

// --- ✅ NUEVO: CLASE DE DATOS PARA MÉTRICAS DETALLADAS ---
data class TestMetricLog(
    val testId: String,
    val attempt: Int,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any> = mapOf()
)

object CortexManager {

    private const val PREFS_NAME = "CortexPrefs"
    private const val KEY_LOCK_UNTIL = "cortex_lock_until"

    var operadorActual: Operador? = null

    // Sistema de puntuación principal
    private val resultados = mutableMapOf<String, Int>()
    private val intentosPorTest = mutableMapOf<String, Int>()
    private val puntajesTemporales = mutableMapOf<String, MutableList<Int>>()

    // --- ✅ NUEVO: LOG DE RENDIMIENTO DETALLADO ---
    val performanceLog = mutableListOf<TestMetricLog>()
    
    // --- ✅ NUEVO: HISTORIAL GLOBAL PARA ADMIN ---
    val historialGlobal = mutableListOf<RegistroData>()

    private val listaDeTests = listOf("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10")

    data class TestInfo(val title: String, val icon: String, val desc: String)

    private val testInfos = mapOf(
        "t1" to TestInfo("1. REFLEJOS", "⚡", "Medición de fatiga y vía retino-cortical..."),
        "t2" to TestInfo("2. MEMORIA", "🧠", "Memoria de trabajo y retención visual..."),
        "t3" to TestInfo("3. ANTICIPACIÓN", "⏱️", "Lóbulo Parietal (Cálculo Espacio-Tiempo)..."),
        "t4" to TestInfo("4. COORDINACIÓN", "🎯", "Precisión motora fina y cerebelo..."),
        "t5" to TestInfo("5. ATENCIÓN", "👁️", "Inhibición de distractores (Stroop)..."),
        "t6" to TestInfo("6. ESCANEO", "🔍", "Búsqueda visual en entornos complejos..."),
        "t7" to TestInfo("7. IMPULSO", "✋", "Control de impulsos (Corteza Prefrontal)..."),
        "t8" to TestInfo("8. RASTREO", "📍", "Conciencia Situacional (MOT)..."),
        "t9" to TestInfo("9. ESPACIAL", "🧭", "Orientación y conflicto direccional..."),
        "t10" to TestInfo("10. DECISIÓN", "⚖️", "Flexibilidad Cognitiva...")
    )

    fun obtenerInfoTest(testId: String): TestInfo = testInfos[testId]!!

    // --- ✅ NUEVO: FUNCIÓN PARA REGISTRAR MÉTRICAS DETALLADAS ---
    fun logPerformanceMetric(testId: String, score: Int, details: Map<String, Any>) {
        val log = TestMetricLog(
            testId = testId,
            attempt = obtenerIntentoActual(testId),
            score = score,
            details = details
        )
        performanceLog.add(log)
    }
    
    fun guardarPuntaje(testId: String, puntaje: Int, esPrimerIntento: Boolean = true) {
        val puntajeClamp = puntaje.coerceIn(0, 100)
        val intentoActual = intentosPorTest.getOrPut(testId) { 1 }

        if (intentoActual == 1) {
            if (puntajeClamp >= 80) { // Umbral de éxito para no repetir
                resultados[testId] = puntajeClamp
            } else {
                puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
                intentosPorTest[testId] = 2 // Marcar para segundo intento
            }
        } else {
            puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
            val promedio = (puntajesTemporales[testId]!!.sum() / puntajesTemporales[testId]!!.size).coerceIn(0, 100)
            resultados[testId] = promedio
        }
    }

    fun obtenerIntentoActual(testId: String): Int = intentosPorTest.getOrPut(testId) { 1 }

    fun obtenerResultados(): Map<String, Int> = resultados

    fun registrarEvaluacion(notaFinal: Int, esApto: Boolean) {
        operadorActual?.let {
            val reg = RegistroData(it.fecha, it.hora, it.supervisor, it.nombre, it.dni, it.equipo, notaFinal, if (esApto) "APTO" else "NO APTO")
            historialGlobal.add(reg)
        }
    }

    fun navegarAlSiguiente(context: Context) {
        val ultimoCompletado = listaDeTests.lastOrNull { resultados.containsKey(it) }
        val indiceUltimo = if (ultimoCompletado == null) -1 else listaDeTests.indexOf(ultimoCompletado)
        
        val proximoTestId = if (indiceUltimo + 1 < listaDeTests.size) listaDeTests[indiceUltimo + 1] else null
        
        if (proximoTestId != null) {
            val intent = Intent(context, IntroTestActivity::class.java)
            intent.putExtra("TEST_ID", proximoTestId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        } else {
            val intent = Intent(context, ReporteFinalActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    fun obtenerIntentTest(context: Context, testId: String): Intent? {
        return when (testId) {
            "t1" -> Intent(context, ReflejosTestActivity::class.java)
            "t2" -> Intent(context, SecuenciaTestActivity::class.java)
            "t3" -> Intent(context, AnticipacionTestActivity::class.java)
            "t4" -> Intent(context, CoordinacionTestActivity::class.java)
            "t5" -> Intent(context, AtencionTestActivity::class.java)
            "t6" -> Intent(context, EscaneoTestActivity::class.java)
            "t7" -> Intent(context, ImpulsoTestActivity::class.java)
            "t8" -> Intent(context, RastreoTestActivity::class.java)
            "t9" -> Intent(context, EspacialTestActivity::class.java)
            "t10" -> Intent(context, DecisionTestActivity::class.java)
            else -> null
        }
    }

    fun resetearEvaluacion() {
        resultados.clear()
        intentosPorTest.clear()
        puntajesTemporales.clear()
        performanceLog.clear() // Limpiar el nuevo log
        operadorActual = null
    }

    // --- Lógica de bloqueo (sin cambios) ---
    private var appContext: Context? = null
    fun inicializarContexto(context: Context) { appContext = context.applicationContext }
    fun estaBloqueado(): Boolean {
        val lockUntil = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0
        return lockUntil > System.currentTimeMillis()
    }
    fun obtenerTiempoDesbloqueo(): Long = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0
    fun bloquearSistema(context: Context) {
        val unlockTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putLong(KEY_LOCK_UNTIL, unlockTime)?.apply()
    }
    fun desbloquearSistema(context: Context) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.remove(KEY_LOCK_UNTIL)?.apply()
    }
    fun verificarCodigoSupervisor(codigo: String): Boolean = codigo == "1007"
}

data class RegistroData(
    val fecha: String, val hora: String, val supervisor: String,
    val nombre: String, val dni: String, val equipo: String,
    val nota: Int, val estado: String
)
