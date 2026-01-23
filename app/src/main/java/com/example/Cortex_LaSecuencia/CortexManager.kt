package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.Cortex_LaSecuencia.actividades.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

data class TestMetricLog(
    val testId: String,
    val attempt: Int,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, Any> = mapOf()
)

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

object CortexManager {

    private const val PREFS_NAME = "CortexPrefs"
    private const val KEY_LOCK_UNTIL = "cortex_lock_until"

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

    private val listaDeTests = listOf("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10")

    data class TestInfo(val title: String, val icon: String, val desc: String)

    private val testInfos = mapOf(
        "t1" to TestInfo("1. REFLEJOS", "⚡", "Medición de fatiga..."),
        "t2" to TestInfo("2. MEMORIA", "🧠", "Memoria de trabajo..."),
        "t3" to TestInfo("3. ANTICIPACIÓN", "⏱️", "Lóbulo Parietal..."),
        "t4" to TestInfo("4. COORDINACIÓN", "🎯", "Precisión motora..."),
        "t5" to TestInfo("5. ATENCIÓN", "👁️", "Inhibición distractores..."),
        "t6" to TestInfo("6. ESCANEO", "🔍", "Búsqueda visual..."),
        "t7" to TestInfo("7. IMPULSO", "✋", "Control impulsos..."),
        "t8" to TestInfo("8. RASTREO", "📍", "Conciencia Situacional..."),
        "t9" to TestInfo("9. ESPACIAL", "🧭", "Orientación..."),
        "t10" to TestInfo("10. DECISIÓN", "⚖️", "Flexibilidad Cognitiva...")
    )

    fun obtenerInfoTest(testId: String): TestInfo = testInfos[testId]!!

    // ============================================================
    // ✅ NUEVO: AUTENTICACIÓN ANÓNIMA (Para Conductores)
    // ============================================================
    fun autenticarConductorAnonimo(onSuccess: (FirebaseUser) -> Unit, onError: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                usuarioConductor = result.user
                onSuccess(result.user!!)
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Error desconocido")
            }
    }

    fun guardarFotoFacial(imagenBytes: ByteArray, dni: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().getReference("fotos_faciales/${dni}_${System.currentTimeMillis()}.jpg")
        storageRef.putBytes(imagenBytes).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                fotoFacialUrl = uri.toString()
                fotoFacialVerificada = true
                onSuccess(uri.toString())
            }
        }.addOnFailureListener { error -> onError(error.localizedMessage ?: "Error al subir foto") }
    }

    fun obtenerIntentoActual(testId: String): Int = intentosPorTest.getOrPut(testId) { 1 }

    fun logPerformanceMetric(testId: String, score: Int, details: Map<String, Any>) {
        val log = TestMetricLog(testId, obtenerIntentoActual(testId), score, details = details)
        performanceLog.add(log)
    }

    fun guardarPuntaje(testId: String, puntaje: Int, esPrimerIntento: Boolean = true) {
        val puntajeClamp = puntaje.coerceIn(0, 100)
        val intentoActual = intentosPorTest.getOrPut(testId) { 1 }
        if (intentoActual == 1) {
            if (puntajeClamp >= 80) resultados[testId] = puntajeClamp
            else {
                puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
                intentosPorTest[testId] = 2
            }
        } else {
            puntajesTemporales.getOrPut(testId) { mutableListOf() }.add(puntajeClamp)
            val promedio = (puntajesTemporales[testId]!!.sum() / puntajesTemporales[testId]!!.size).coerceIn(0, 100)
            resultados[testId] = promedio
        }
    }

    fun obtenerResultados(): Map<String, Int> = resultados

    fun registrarEvaluacion(notaFinal: Int, esApto: Boolean) {
        operadorActual?.let {
            val reg = RegistroData(it.fecha, it.hora, it.supervisor, it.nombre, it.dni, it.equipo, notaFinal, if (esApto) "APTO" else "NO APTO")
            historialGlobal.add(reg)
        }
    }

    fun registrarNube(notaFinal: Int, esApto: Boolean) {
        operadorActual?.let { op ->
            val reg = RegistroData(op.fecha, op.hora, op.supervisor, op.nombre, op.dni, op.equipo, notaFinal, if (esApto) "APTO" else "NO APTO")
            val dbRef = FirebaseDatabase.getInstance().getReference("registros")
            val key = dbRef.push().key
            if (key != null) {
                dbRef.child(key).setValue(reg)
                    .addOnSuccessListener { Log.d("FIREBASE", "✓ Sincronizado") }
                    .addOnFailureListener { e -> Log.e("FIREBASE", "✗ Error", e) }
            }
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
        resultados.clear(); intentosPorTest.clear(); puntajesTemporales.clear(); performanceLog.clear(); operadorActual = null
    }

    private var appContext: Context? = null
    fun inicializarContexto(context: Context) { appContext = context.applicationContext }
    fun estaBloqueado(): Boolean = (appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0) > System.currentTimeMillis()
    fun obtenerTiempoDesbloqueo(): Long = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.getLong(KEY_LOCK_UNTIL, 0) ?: 0
    fun bloquearSistema(context: Context) {
        val unlockTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.putLong(KEY_LOCK_UNTIL, unlockTime)?.apply()
    }
    fun desbloquearSistema(context: Context) { appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.remove(KEY_LOCK_UNTIL)?.apply() }
    fun verificarCodigoSupervisor(codigo: String): Boolean = codigo == "1007"
    // ✅ NUEVA FUNCIÓN: Guardar hash facial en Realtime Database
    fun guardarHashFacial(
        hashFacial: String,
        dni: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val conductorRef = database.getReference("conductores").child(dni)

        val datos = mapOf(
            "hashFacial" to hashFacial,
            "fechaRegistro" to System.currentTimeMillis(),
            "verificado" to true
        )

        conductorRef.updateChildren(datos)
            .addOnSuccessListener {
                fotoFacialVerificada = true
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Error al guardar en base de datos")
            }
    }

    // ✅ OPCIONAL: Si prefieres guardar un ID numérico en vez de hash
    fun guardarIdFacialNumerico(
        idFacial: Long,
        dni: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance()
        val conductorRef = database.getReference("conductores").child(dni)

        val datos = mapOf(
            "idFacial" to idFacial,
            "fechaRegistro" to System.currentTimeMillis(),
            "verificado" to true
        )

        conductorRef.updateChildren(datos)
            .addOnSuccessListener {
                fotoFacialVerificada = true
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Error al guardar en base de datos")
            }
    }
}