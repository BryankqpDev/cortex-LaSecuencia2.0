package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import com.example.Cortex_LaSecuencia.actividades.AnticipacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity
import com.example.Cortex_LaSecuencia.actividades.CoordinacionTestActivity // ✅ Importante

object CortexManager {

    // Datos del operador actual
    var operadorActual: Operador? = null

    // Mapa para guardar los puntajes (Ej: "t1" -> 100)
    private val resultados = mutableMapOf<String, Int>()

    // LISTA ORDENADA DE TESTS
    // Es vital que el orden sea exacto: T1, T2, T3, T4...
    private val listaDeTests = listOf(
        "t1", // Reflejos
        "t2", // Memoria Secuencial
        "t3", // Anticipación (Camión)
        "t4", // Coordinación (Bolitas)
        "t5",
        "t6"
    )

    // Función para guardar notas
    fun guardarPuntaje(testId: String, puntaje: Int) {
        resultados[testId] = puntaje
    }

    // --- LÓGICA DE NAVEGACIÓN INTELIGENTE ---
    fun navegarAlSiguiente(context: Context) {
        // 1. Buscamos el índice del test más avanzado que ya se completó.
        var ultimoIndiceCompletado = -1

        for (i in listaDeTests.indices) {
            val idTest = listaDeTests[i]
            if (resultados.containsKey(idTest)) {
                ultimoIndiceCompletado = i
            }
        }

        // 2. Calculamos cuál toca ahora
        val siguienteIndice = ultimoIndiceCompletado + 1

        // 3. Si todavía hay tests en la lista, lanzamos el siguiente
        if (siguienteIndice < listaDeTests.size) {
            val siguienteTestId = listaDeTests[siguienteIndice]

            val intent = when (siguienteTestId) {
                "t1" -> Intent(context, ReflejosTestActivity::class.java)
                "t2" -> Intent(context, SecuenciaTestActivity::class.java)
                "t3" -> Intent(context, AnticipacionTestActivity::class.java)

                // ✅ AQUÍ ESTÁ LA INTEGRACIÓN DEL T4:
                "t4" -> Intent(context, CoordinacionTestActivity::class.java)

                else -> null
            }

            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        } else {
            // 4. SI YA NO HAY MÁS TESTS (FIN DE LA EVALUACIÓN)
            // Aquí irá el ReporteFinalActivity más adelante
            // Intent(context, ReporteFinalActivity::class.java).also { context.startActivity(it) }
        }
    }

    // Limpia todo para un nuevo operador
    fun resetearEvaluacion() {
        resultados.clear()
        operadorActual = null
    }
}