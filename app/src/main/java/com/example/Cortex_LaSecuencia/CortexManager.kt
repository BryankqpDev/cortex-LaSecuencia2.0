package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import com.example.Cortex_LaSecuencia.actividades.AnticipacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity
import com.example.Cortex_LaSecuencia.actividades.CoordinacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.AtencionTestActivity
import com.example.Cortex_LaSecuencia.actividades.EscaneoTestActivity
import com.example.Cortex_LaSecuencia.actividades.ImpulsoTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReporteFinalActivity

object CortexManager {

    // Datos del operador actual
    var operadorActual: Operador? = null

    // Mapa para guardar los puntajes (Ej: "t1" -> 100)
    private val resultados = mutableMapOf<String, Int>()

    // --- ✅ NUEVO: LISTA PARA EL HISTORIAL GLOBAL (ADMIN) ---
    val historialGlobal = mutableListOf<RegistroData>()

    // LISTA ORDENADA DE TESTS
    private val listaDeTests = listOf(
        "t1", // Reflejos
        "t2", // Memoria Secuencial
        "t3", // Anticipación
        "t4", // Coordinación
        "t5", // Atención
        "t6", // Escaneo
        "t7"  // Impulso
    )

    // Función para guardar notas individuales
    fun guardarPuntaje(testId: String, puntaje: Int) {
        resultados[testId] = puntaje
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

            val intent = when (siguienteTestId) {
                "t1" -> Intent(context, ReflejosTestActivity::class.java)
                "t2" -> Intent(context, SecuenciaTestActivity::class.java)
                "t3" -> Intent(context, AnticipacionTestActivity::class.java)
                "t4" -> Intent(context, CoordinacionTestActivity::class.java)
                "t5" -> Intent(context, AtencionTestActivity::class.java)
                "t6" -> Intent(context, EscaneoTestActivity::class.java)
                "t7" -> Intent(context, ImpulsoTestActivity::class.java)
                else -> null
            }

            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        } else {
            // FIN DE TODO -> IR AL REPORTE FINAL
            val intent = Intent(context, ReporteFinalActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    // Limpia todo para un nuevo operador
    fun resetearEvaluacion() {
        resultados.clear()
        operadorActual = null
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