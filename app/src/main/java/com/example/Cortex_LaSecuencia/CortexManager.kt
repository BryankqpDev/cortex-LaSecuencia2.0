package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import com.example.Cortex_LaSecuencia.actividades.AnticipacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity

object CortexManager {

    var operadorActual: Operador? = null

    // Mapa de resultados
    private val resultados = mutableMapOf<String, Int>()

    // Lista ordenada de tests
    private val listaDeTests = listOf(
        "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"
    )

    fun guardarPuntaje(testId: String, puntaje: Int) {
        resultados[testId] = puntaje
    }

    // --- AQUÍ ESTÁ LA CORRECCIÓN MÁGICA ---
    fun navegarAlSiguiente(context: Context) {
        // En lugar de usar un contador ciego, buscamos el primer test que NO tenga nota
        var siguienteTestId: String? = null

        for (test in listaDeTests) {
            if (!resultados.containsKey(test)) {
                siguienteTestId = test
                break // Encontramos el primero que falta, ese es el que toca
            }
        }

        // Si ya hicimos todos, siguienteTestId será null -> Vamos al Reporte
        // Si falta alguno, lanzamos ese Intent
        val intent = when (siguienteTestId) {
            "t1" -> Intent(context, ReflejosTestActivity::class.java)
            "t2" -> Intent(context, SecuenciaTestActivity::class.java)
            "t3" -> Intent(context, AnticipacionTestActivity::class.java)
            // Aquí agregarás "t4", "t5" cuando los creemos...
            else -> null // Aquí iría el ReporteFinalActivity
        }

        if (intent != null) {
            context.startActivity(intent)
        } else {
            // TODO: Crear y lanzar ReporteFinalActivity
        }
    }

    fun resetearEvaluacion() {
        resultados.clear()
    }
}