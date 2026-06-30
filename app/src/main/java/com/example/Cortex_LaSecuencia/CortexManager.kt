package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.Cortex_LaSecuencia.actividades.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

// ✅ TestMetricLog se queda aquí (o puedes moverla a otro archivo si quieres después)
data class TestMetricLog(
    val testId: String,
    val attempt: Int,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any> = mapOf()
)

data class RegistroData(
    val fecha: String = "",
    val hora: String = "",
    val supervisor: String = "",
    val nombre: String = "",
    val dni: String = "",
    val equipo: String = "",
    val nota: Int = 0,
    val estado: String = "",
    val tiempoTotal: String = "",  //
    var enPapelera: Boolean = false
)

// ❌ AQUÍ BORRAMOS 'data class SolicitudDesbloqueo' PORQUE YA TIENES TU ARCHIVO PROPIO
// El código ahora buscará automáticamente el archivo SolicitudDesbloqueo.kt que creaste.

object CortexManager {

    private const val PREFS_NAME = "CortexPrefs"
    private const val KEY_LOCK_UNTIL = "cortex_lock_until"
    private const val KEY_YA_ENVIADO = "eval_ya_enviado"
    
    private const val KEY_EVAL_EN_PROGRESO = "eval_en_progreso"
    private const val KEY_RESULTADOS = "eval_resultados"
    private const val KEY_INTENTOS = "eval_intentos"
    private const val KEY_PUNTAJES_TEMP = "eval_puntajes_temp"
    private const val KEY_OP_NOMBRE = "op_nombre"
    private const val KEY_OP_DNI = "op_dni"
    private const val KEY_OP_EMPRESA = "op_empresa"
    private const val KEY_OP_SUPERVISOR = "op_supervisor"
    private const val KEY_OP_EQUIPO = "op_equipo"
    private const val KEY_OP_UNIDAD = "op_unidad"
    private const val KEY_OP_FECHA = "op_fecha"
    private const val KEY_OP_HORA = "op_hora"
    private const val KEY_OP_TIMESTAMP_INICIO = "op_timestamp_inicio"
    private const val KEY_OP_TIMESTAMP_FIN = "op_timestamp_fin"

    var operadorActual: Operador? = null
    var usuarioAdmin: FirebaseUser? = null
    var usuarioConductor: FirebaseUser? = null

    private var fotoFacialUrl: String? = null
    private var fotoFacialVerificada: Boolean = false

    private val resultados = mutableMapOf<String, Int>()
    private val intentosPorTest = mutableMapOf<String, Int>()
    private val puntajesTemporales = mutableMapOf<String, MutableList<Int>>()
    val performanceLog = mutableListOf<TestMetricLog>()
    val historialGlobal = mutableListOf<RegistroData>()

    // ✅ Correos de admin autorizados (SIN vacíos para evitar bypass)
    private val emailsAdminAutorizados = listOf("limmpu@gmail.com", "fabiogomez2482@gmail.com","seguridad.catalinah@bjgrupo.com","asste.seguridadcatalinah@bjgrupo.com","residencia.catalinah@bjgrupo.com","Operaciones.catalinah@bjgrupo.com","facilitadoradelsigcatalina@bjgrupo.com")
    
    fun esEmailAutorizado(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return emailsAdminAutorizados.any { it.equals(email.trim(), ignoreCase = true) }
    }

    private val listaDeTests = listOf("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10")

    fun obtenerInfoTest(testId: String): TestInfo = testInfos[testId]!!

    private val testInfos = mapOf(
        "t1" to TestInfo("1. REFLEJOS", "⚡", "Medición de fatiga..."),
        "t2" to TestInfo("2. MEMORIA", "🧠", "Medición de fatiga..."), //
        "t3" to TestInfo("3. ANTICIPACIÓN", "⏱️", "Lóbulo Parietal..."),
        "t4" to TestInfo("4. COORDINACIÓN", "🎯", "Precisión motora..."),
        "t5" to TestInfo("5. ATENCIÓN", "👁️", "Inhibición distractores..."),
        "t6" to TestInfo("6. ESCANEO", "🔍", "Búsqueda visual..."),
        "t7" to TestInfo("7. IMPULSO", "✋", "Control impulsos..."),
        "t8" to TestInfo("8. RASTREO", "📍", "Conciencia Situacional..."),
        "t9" to TestInfo("9. ESPACIAL", "🧭", "Orientación..."),
        "t10" to TestInfo("10. DECISIÓN", "⚖️", "Flexibilidad Cognitiva...")
    )

    data class TestInfo(val title: String, val icon: String, val desc: String)

    // --- Autenticación ---
    fun autenticarConductorAnonimo(onSuccess: (FirebaseUser) -> Unit, onError: (String) -> Unit) {
        FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener { onSuccess(it.user!!) }.addOnFailureListener { onError(it.localizedMessage ?: "Error") }
    }

    // --- Storage (Fotos) ---
    fun guardarFotoFacial(imagenBytes: ByteArray, dni: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().getReference("fotos_faciales/${dni}_${System.currentTimeMillis()}.jpg")
        storageRef.putBytes(imagenBytes).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                fotoFacialUrl = uri.toString()
                onSuccess(uri.toString())
            }
        }.addOnFailureListener { onError(it.localizedMessage ?: "Error") }
    }

    // --- GESTIÓN DE SOLICITUDES DE DESBLOQUEO ---
    fun enviarSolicitudDesbloqueo(motivo: String) {
        val op = operadorActual ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo")

        // ✅ Aquí usará tu nueva CLASE EXTERNA 'SolicitudDesbloqueo'
        val solicitud = SolicitudDesbloqueo(
            dni = op.dni,
            nombre = op.nombre,
            fecha = op.fecha + " " + op.hora,
            motivo = motivo
        )
        dbRef.child(op.dni).setValue(solicitud)
    }

    fun eliminarSolicitudDesbloqueo(dni: String) {
        FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo").child(dni).removeValue()
    }

    // --- Lógica de Tests ---
    fun obtenerIntentoActual(testId: String): Int = intentosPorTest.getOrPut(testId) { 1 }

    fun logPerformanceMetric(testId: String, score: Int, details: Map<String, Any>) {
        performanceLog.add(TestMetricLog(testId, obtenerIntentoActual(testId), score, details = details))
    }

    fun guardarPuntaje(testId: String, puntaje: Int, esPrimerIntento: Boolean = true) {
        val puntajeClamp = puntaje.coerceIn(0, 100)
        val intentoActual = intentosPorTest.getOrPut(testId) { 1 }
        if (intentoActual == 1) {
            // ✅ CAMBIO: Umbral de 95% (igual que MVP JavaScript)
            if (puntajeClamp >= 95) {
                resultados[testId] = puntajeClamp
            } else {
                puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
                intentosPorTest[testId] = 2
            }
        } else {
            puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
            val promedio = (puntajesTemporales[testId]!!.sum() / puntajesTemporales[testId]!!.size).coerceIn(0, 100)
            resultados[testId] = promedio
        }

        // Capturar timestamp de finalización cuando se completan todos los tests
        if (resultados.size == listaDeTests.size && (operadorActual?.timestampFin ?: 0L) == 0L) {
            operadorActual?.timestampFin = System.currentTimeMillis()
            Log.d("CortexManager", "⏱️ Tiempo final capturado: ${operadorActual?.timestampFin}")
        }
        guardarProgreso()
    }

    fun obtenerResultados(): Map<String, Int> = resultados

    // --- Registro Histórico ---
    fun registrarEvaluacion(notaFinal: Int, esApto: Boolean) {
        operadorActual?.let {
            val reg = RegistroData(
                fecha = it.fecha,
                hora = it.hora,
                supervisor = it.supervisor,
                nombre = it.nombre,
                dni = it.dni,
                equipo = it.equipo,
                nota = notaFinal,
                estado = if (esApto) "APTO" else "NO APTO"
            )
            historialGlobal.add(reg)
        }
    }

    fun registrarNube(notaFinal: Int, esApto: Boolean) {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs?.getBoolean(KEY_YA_ENVIADO, false) == true) {
            Log.d("CortexManager", "⚠️ Registro ya enviado anteriormente, omitiendo duplicado")
            return
        }

        operadorActual?.let { op ->
            val tiempoMs = System.currentTimeMillis() - op.timestampInicio
            val minutos = (tiempoMs / 1000 / 60).toInt()
            val segundos = ((tiempoMs / 1000) % 60).toInt()
            val tiempoFormateado = "${minutos}m ${segundos}s"

            val reg = RegistroData(
                fecha = op.fecha,
                hora = op.hora,
                supervisor = op.supervisor,
                nombre = op.nombre,
                dni = op.dni,
                equipo = op.equipo,
                nota = notaFinal,
                estado = if (esApto) "APTO" else "NO APTO",
                tiempoTotal = tiempoFormateado
            )
            val dbRef = FirebaseDatabase.getInstance().getReference("registros")
            dbRef.push().setValue(reg)

            // Al final, marcar como enviado:
            prefs?.edit()?.putBoolean(KEY_YA_ENVIADO, true)?.apply()
        }
    }

    // --- Navegación ---
    fun navegarAlSiguiente(context: Context) {
        val ultimo = listaDeTests.lastOrNull { resultados.containsKey(it) }
        val indice = if (ultimo == null) -1 else listaDeTests.indexOf(ultimo)
        val proximo = if (indice + 1 < listaDeTests.size) listaDeTests[indice + 1] else null
        if (proximo != null) {
            val intent = Intent(context, IntroTestActivity::class.java).apply { putExtra("TEST_ID", proximo); flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
            context.startActivity(intent)
        } else {
            val intent = Intent(context, ReporteFinalActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
            context.startActivity(intent)
        }
    }

    // --- Sistema de Bloqueo ---
    private var appContext: Context? = null
    fun inicializarContexto(context: Context) { appContext = context.applicationContext }

    fun estaBloqueado(): Boolean = (appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0) > System.currentTimeMillis()

    fun obtenerTiempoDesbloqueo(): Long = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0

    private const val KEY_LOCKED_DNI = "cortex_locked_dni"
    // 1. Modifica bloquearSistema para guardar el DNI
    fun bloquearSistema(context: Context) {
        val unlockTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        val dniOperador = operadorActual?.dni ?: "" // Obtenemos el DNI actual

        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putLong(KEY_LOCK_UNTIL, unlockTime)
            ?.putString(KEY_LOCKED_DNI, dniOperador) // <--- GUARDAMOS EL DNI
            ?.apply()
    }

    fun desbloquearSistema(context: Context) {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.remove(KEY_LOCK_UNTIL)
            ?.remove(KEY_LOCKED_DNI) // <--- BORRAMOS EL DNI
            ?.apply()

        // Si tenemos el dato en memoria, borramos la solicitud de la nube
        val dni = operadorActual?.dni ?: obtenerDniBloqueado()
        if (!dni.isNullOrEmpty()) eliminarSolicitudDesbloqueo(dni)
    }
    fun obtenerDniBloqueado(): String? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_LOCKED_DNI, null)
    }

    fun verificarCodigoSupervisor(codigo: String): Boolean = codigo == "1007"

    // --- Utilidades de Intents ---
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
        resultados.clear(); intentosPorTest.clear(); puntajesTemporales.clear(); performanceLog.clear(); operadorActual = null; limpiarProgreso()
    }

    fun guardarProgreso() {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val editor = prefs.edit()
        editor.putBoolean(KEY_EVAL_EN_PROGRESO, true)
        editor.putString(KEY_RESULTADOS, resultados.entries.joinToString(",") { "${it.key}:${it.value}" })
        editor.putString(KEY_INTENTOS, intentosPorTest.entries.joinToString(",") { "${it.key}:${it.value}" })
        editor.putString(KEY_PUNTAJES_TEMP, puntajesTemporales.entries.joinToString(",") { entry ->
            "${entry.key}:${entry.value.joinToString("|")}"
        })
        operadorActual?.let { op ->
            editor.putString(KEY_OP_NOMBRE, op.nombre)
            editor.putString(KEY_OP_DNI, op.dni)
            editor.putString(KEY_OP_EMPRESA, op.empresa)
            editor.putString(KEY_OP_SUPERVISOR, op.supervisor)
            editor.putString(KEY_OP_EQUIPO, op.equipo)
            editor.putString(KEY_OP_UNIDAD, op.unidad)
            editor.putString(KEY_OP_FECHA, op.fecha)
            editor.putString(KEY_OP_HORA, op.hora)
            editor.putLong(KEY_OP_TIMESTAMP_INICIO, op.timestampInicio)
            editor.putLong(KEY_OP_TIMESTAMP_FIN, op.timestampFin)
        }
        editor.apply()
        Log.d("CortexManager", "✅ Progreso guardado: ${resultados.size} tests completados")
    }

    fun restaurarProgreso(): Boolean {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return false
        if (!prefs.getBoolean(KEY_EVAL_EN_PROGRESO, false)) return false
        val nombre = prefs.getString(KEY_OP_NOMBRE, null) ?: return false
        val dni = prefs.getString(KEY_OP_DNI, null) ?: return false
        operadorActual = Operador(
            nombre = nombre,
            dni = dni,
            empresa = prefs.getString(KEY_OP_EMPRESA, "") ?: "",
            supervisor = prefs.getString(KEY_OP_SUPERVISOR, "") ?: "",
            equipo = prefs.getString(KEY_OP_EQUIPO, "") ?: "",
            unidad = prefs.getString(KEY_OP_UNIDAD, "") ?: "",
            fecha = prefs.getString(KEY_OP_FECHA, "") ?: "",
            hora = prefs.getString(KEY_OP_HORA, "") ?: "",
            timestampInicio = prefs.getLong(KEY_OP_TIMESTAMP_INICIO, 0L),
            timestampFin = prefs.getLong(KEY_OP_TIMESTAMP_FIN, 0L)
        )
        resultados.clear()
        prefs.getString(KEY_RESULTADOS, "")?.split(",")?.forEach { entry ->
            if (entry.contains(":")) {
                val (key, value) = entry.split(":")
                resultados[key] = value.toIntOrNull() ?: 0
            }
        }
        intentosPorTest.clear()
        prefs.getString(KEY_INTENTOS, "")?.split(",")?.forEach { entry ->
            if (entry.contains(":")) {
                val (key, value) = entry.split(":")
                intentosPorTest[key] = value.toIntOrNull() ?: 1
            }
        }
        puntajesTemporales.clear()
        prefs.getString(KEY_PUNTAJES_TEMP, "")?.split(",")?.forEach { entry ->
            if (entry.contains(":")) {
                val parts = entry.split(":")
                val key = parts[0]
                val values = parts.getOrNull(1)?.split("|")
                    ?.mapNotNull { it.toIntOrNull() }?.toMutableList() ?: mutableListOf()
                if (values.isNotEmpty()) puntajesTemporales[key] = values
            }
        }
        Log.d("CortexManager", "✅ Progreso restaurado: ${resultados.size} tests, operador: $nombre")
        return true
    }

    private fun limpiarProgreso() {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        prefs.edit()
            .remove(KEY_EVAL_EN_PROGRESO)
            .remove(KEY_RESULTADOS)
            .remove(KEY_INTENTOS)
            .remove(KEY_PUNTAJES_TEMP)
            .remove(KEY_OP_NOMBRE)
            .remove(KEY_OP_DNI)
            .remove(KEY_OP_EMPRESA)
            .remove(KEY_OP_SUPERVISOR)
            .remove(KEY_OP_EQUIPO)
            .remove(KEY_OP_UNIDAD)
            .remove(KEY_OP_FECHA)
            .remove(KEY_OP_HORA)
            .remove(KEY_OP_TIMESTAMP_INICIO)
            .remove(KEY_OP_TIMESTAMP_FIN)
            .remove(KEY_YA_ENVIADO)
            .apply()
        Log.d("CortexManager", "🗑️ Progreso limpiado")
    }

    fun tieneProgresoGuardado(): Boolean {
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return false
        return prefs.getBoolean(KEY_EVAL_EN_PROGRESO, false)
    }
}