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
//import com.example.Cortex_LaSecuencia.actividades.RastreoTestActivity
import com.example.Cortex_LaSecuencia.actividades.EspacialTestActivity
import com.example.Cortex_LaSecuencia.actividades.DecisionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReporteFinalActivity
import com.example.Cortex_LaSecuencia.actividades.IntroTestActivity

object CortexManager {

    private const val PREFS_NAME = "CortexPrefs"
    private const val KEY_LOCK_UNTIL = "cortex_lock_until"
    private const val KEY_HISTORIAL = "cortex_historial"

    // Datos del operador actual
    var operadorActual: Operador? = null

    // Mapa para guardar los puntajes (Ej: "t1" -> 100)
    private val resultados = mutableMapOf<String, Int>()
    
    // Sistema de intentos (como en HTML: currentAttempt, tempScores)
    private val intentosPorTest = mutableMapOf<String, Int>() // "t1" -> 1 o 2
    private val puntajesTemporales = mutableMapOf<String, MutableList<Int>>() // "t1" -> [85, 90]

    // --- ✅ NUEVO: LISTA PARA EL HISTORIAL GLOBAL (ADMIN) ---
    val historialGlobal = mutableListOf<RegistroData>()

    // LISTA ORDENADA DE TESTS (10 tests como en HTML)
    private val listaDeTests = listOf(
        "t1", // Reflejos
        "t2", // Memoria Secuencial
        "t3", // Anticipación
        "t4", // Coordinación
        "t5", // Atención
        "t6", // Escaneo
        "t7", // Impulso
        "t8", // Rastreo (MOT)
        "t9", // Espacial
        "t10" // Decisión
    )

    // Información de cada test (igual que en HTML)
    data class TestInfo(
        val title: String,
        val icon: String,
        val desc: String
    )

    private val testInfos = mapOf(
        "t1" to TestInfo(
            title = "1. REFLEJOS",
            icon = "⚡",
            desc = "🧠 ANÁLISIS CEREBRAL: Medición de fatiga y vía retino-cortical.\n\n🏁 TU MISIÓN: El botón está GRIS. Espera... Cuando se ponga VERDE 🟩 ¡Presiona al instante! Si tardas, el sistema detecta 'microsueño'."
        ),
        "t2" to TestInfo(
            title = "2. MEMORIA",
            icon = "🧠",
            desc = "🧠 ANÁLISIS CEREBRAL: Memoria de trabajo y retención visual.\n\n🏁 TU MISIÓN: Memoriza el patrón de luces que se encienden ✨. Luego toca los mismos cuadros en el mismo orden."
        ),
        "t3" to TestInfo(
            title = "3. ANTICIPACIÓN",
            icon = "⏱️",
            desc = "🧠 ANÁLISIS CEREBRAL: Lóbulo Parietal (Cálculo Espacio-Tiempo).\n\n🏁 TU MISIÓN: El camión 🚛 acelerará por la pista. Calcula bien su movimiento y frena 🛑 exactamente cuando esté dentro de la ZONA VERDE."
        ),
        "t4" to TestInfo(
            title = "4. COORDINACIÓN",
            icon = "🎯",
            desc = "🧠 ANÁLISIS CEREBRAL: Precisión motora fina y cerebelo.\n\n🏁 TU MISIÓN: Aparecerán bolitas amarillas 🟡. Sé rápido y atrapa exactamente 5 con tu dedo."
        ),
        "t5" to TestInfo(
            title = "5. ATENCIÓN",
            icon = "👁️",
            desc = "🧠 ANÁLISIS CEREBRAL: Inhibición de distractores (Stroop).\n\n🏁 TU MISIÓN: ¡Cuidado con la trampa! Mira el COLOR DE LA TINTA 🎨, no leas la palabra. Si dice 'ROJO' pero es verde, presiona VERDE."
        ),
        "t6" to TestInfo(
            title = "6. ESCANEO",
            icon = "🔍",
            desc = "🧠 ANÁLISIS CEREBRAL: Búsqueda visual en entornos complejos.\n\n🏁 TU MISIÓN: Te diré un número arriba (ej: 45). Búscalo rápido 🔎 entre todos los números de abajo y tócalo."
        ),
        "t7" to TestInfo(
            title = "7. IMPULSO",
            icon = "✋",
            desc = "🧠 ANÁLISIS CEREBRAL: Control de impulsos (Corteza Prefrontal).\n\n🏁 TU MISIÓN:\n💎 AZUL = ¡TOCA!\n❌ NARANJA = ¡NO TOQUES! (Contrólate)."
        ),
        "t8" to TestInfo(
            title = "8. RASTREO",
            icon = "📍",
            desc = "🧠 ANÁLISIS CEREBRAL: Conciencia Situacional (MOT).\n\n🏁 TU MISIÓN: Mira las 2 bolitas AZULES 🔵🔵. Se volverán blancas y se mezclarán. Síguelas con la vista 👀. Al final, dime cuáles eran."
        ),
        "t9" to TestInfo(
            title = "9. ESPACIAL",
            icon = "🧭",
            desc = "🧠 ANÁLISIS CEREBRAL: Orientación y conflicto direccional.\n\n🏁 TU MISIÓN:\n🟦 Flecha AZUL: Marca su dirección real.\n🟥 Flecha ROJA: ¡Marca la dirección CONTRARIA!"
        ),
        "t10" to TestInfo(
            title = "10. DECISIÓN",
            icon = "⚖️",
            desc = "🧠 ANÁLISIS CEREBRAL: Flexibilidad Cognitiva.\n\n🏁 TU MISIÓN: La regla cambia:\n🟦 Si es AZUL = Toca el número MAYOR.\n🟧 Si es NARANJA = Toca el número MENOR."
        )
    )

    fun obtenerInfoTest(testId: String): TestInfo {
        return testInfos[testId] ?: testInfos["t1"]!!
    }

    // Función para guardar notas individuales (con sistema de intentos)
    fun guardarPuntaje(testId: String, puntaje: Int, esPrimerIntento: Boolean = true) {
        val puntajeClamp = puntaje.coerceIn(0, 100)
        
        if (esPrimerIntento) {
            val intentoActual = intentosPorTest[testId] ?: 1
            
            if (intentoActual == 1) {
                // Primer intento
                if (puntajeClamp >= 95) {
                    // Excelente, pasa directo (como en HTML)
                    resultados[testId] = puntajeClamp
                    intentosPorTest[testId] = 1
                    puntajesTemporales.remove(testId)
                } else {
                    // Necesita segundo intento
                    puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
                    intentosPorTest[testId] = 2
                }
            } else {
                // Segundo intento - promediar
                puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
                val promedio = (puntajesTemporales[testId]!!.sum() / puntajesTemporales[testId]!!.size).coerceIn(0, 100)
                resultados[testId] = promedio
                intentosPorTest[testId] = 2
                puntajesTemporales.remove(testId)
            }
        } else {
            // Guardado directo (sin sistema de intentos)
            resultados[testId] = puntajeClamp
        }
    }
    
    fun obtenerIntentoActual(testId: String): Int {
        return intentosPorTest[testId] ?: 1
    }
    
    fun obtenerPuntajeTemporal(testId: String): Int? {
        return puntajesTemporales[testId]?.lastOrNull()
    }

    fun obtenerResultados(): Map<String, Int> {
        return resultados
    }

    // --- ✅ NUEVO: FUNCIÓN PARA GUARDAR EN LA BASE DE DATOS (HISTORIAL) ---
    fun registrarEvaluacion(notaFinal: Int, esApto: Boolean) {
        operadorActual?.let { op ->
            val estadoTexto = if (esApto) "APTO" else "NO APTO"

            // Creamos el registro con todos los datos
            val nuevoRegistro = RegistroData(
                fecha = op.fecha,
                hora = op.hora,
                supervisor = op.supervisor,
                nombre = op.nombre,
                dni = op.dni,
                equipo = op.equipo,
                nota = notaFinal,
                estado = estadoTexto
            )

            // Lo guardamos en la lista global
            historialGlobal.add(nuevoRegistro)
        }
    }

    // --- LÓGICA DE NAVEGACIÓN INTELIGENTE ---
    fun navegarAlSiguiente(context: Context) {
        var ultimoIndiceCompletado = -1

        for (i in listaDeTests.indices) {
            val idTest = listaDeTests[i]
            if (resultados.containsKey(idTest)) {
                ultimoIndiceCompletado = i
            }
        }

        val siguienteIndice = ultimoIndiceCompletado + 1

        if (siguienteIndice < listaDeTests.size) {
            val siguienteTestId = listaDeTests[siguienteIndice]
            
            // Navegar primero a IntroTestActivity (como en HTML: prepTest)
            val intent = Intent(context, IntroTestActivity::class.java)
            intent.putExtra("TEST_ID", siguienteTestId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        } else {
            // FIN DE TODO -> IR AL REPORTE FINAL
            val intent = Intent(context, ReporteFinalActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    // Obtener Intent del test correspondiente (usado desde IntroTestActivity)
    fun obtenerIntentTest(context: Context, testId: String): Intent? {
        val intent = when (testId) {
            "t1" -> Intent(context, ReflejosTestActivity::class.java)
            "t2" -> Intent(context, SecuenciaTestActivity::class.java)
            "t3" -> Intent(context, AnticipacionTestActivity::class.java)
            "t4" -> Intent(context, CoordinacionTestActivity::class.java)
            "t5" -> Intent(context, AtencionTestActivity::class.java)
            "t6" -> Intent(context, EscaneoTestActivity::class.java)
            "t7" -> Intent(context, ImpulsoTestActivity::class.java)
            //"t8" -> Intent(context, RastreoTestActivity::class.java)
            "t9" -> Intent(context, EspacialTestActivity::class.java)
            "t10" -> Intent(context, DecisionTestActivity::class.java)
            else -> null
        }
        return intent
    }

    // Limpia todo para un nuevo operador
    fun resetearEvaluacion() {
        resultados.clear()
        intentosPorTest.clear()
        puntajesTemporales.clear()
        operadorActual = null
    }

    // --- SISTEMA DE BLOQUEO (24h) ---
    // Variable estática para contexto de aplicación (se inicializa en SplashActivity)
    private var appContext: Context? = null

    fun inicializarContexto(context: Context) {
        appContext = context.applicationContext
    }

    fun estaBloqueado(): Boolean {
        val context = appContext ?: return false
        val prefs = getSharedPreferences(context)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0)
        return lockUntil > System.currentTimeMillis()
    }

    fun obtenerTiempoDesbloqueo(): Long {
        val context = appContext ?: return 0
        val prefs = getSharedPreferences(context)
        return prefs.getLong(KEY_LOCK_UNTIL, 0)
    }

    fun bloquearSistema(context: Context) {
        val prefs = getSharedPreferences(context)
        val unlockTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 horas
        prefs.edit().putLong(KEY_LOCK_UNTIL, unlockTime).apply()
    }

    fun desbloquearSistema(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().remove(KEY_LOCK_UNTIL).apply()
    }

    fun verificarCodigoSupervisor(codigo: String): Boolean {
        return codigo == "1007"
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- PERSISTENCIA DE HISTORIAL ---
    fun cargarHistorial(context: Context) {
        val prefs = getSharedPreferences(context)
        val historialJson = prefs.getString(KEY_HISTORIAL, null)
        if (historialJson != null) {
            // TODO: Implementar deserialización JSON si es necesario
            // Por ahora mantenemos en memoria
        }
    }

    fun guardarHistorial(context: Context) {
        val prefs = getSharedPreferences(context)
        // TODO: Implementar serialización JSON si es necesario
        // Por ahora mantenemos en memoria
    }
}

// --- ✅ NUEVO: LA CLASE DE DATOS PARA LA TABLA ---
data class RegistroData(
    val fecha: String,
    val hora: String,
    val supervisor: String,
    val nombre: String,
    val dni: String,
    val equipo: String,
    val nota: Int,
    val estado: String
)