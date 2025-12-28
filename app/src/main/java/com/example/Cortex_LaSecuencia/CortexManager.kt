package com.example.Cortex_LaSecuencia

import android.content.Context
import android.content.Intent
import com.example.Cortex_LaSecuencia.actividades.AnticipacionTestActivity
import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity

object CortexManager {

    // Datos del operador actual
    var operadorActual: Operador? = null

    // Mapa para guardar los puntajes (Ej: "t1" -> 100)
    private val resultados = mutableMapOf<String, Int>()

    // LISTA ORDENADA DE TESTS
    // Es vital que el orden sea exacto: T1, T2, T3...
    private val listaDeTests = listOf(
        "t1", // Reflejos
        "t2", // Memoria Secuencial
        "t3", // Anticipación (Camión)
        "t4", // Coordinación (Próximamente)
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
        //    Esto evita que nos regrese al T1 si ya estamos en el T2.
        var ultimoIndiceCompletado = -1

        for (i in listaDeTests.indices) {
            val idTest = listaDeTests[i]
            // Si este test ya tiene nota, actualizamos el marcador
            if (resultados.containsKey(idTest)) {
                ultimoIndiceCompletado = i
            }
        }

        // 2. Calculamos cuál toca ahora (el siguiente en la lista)
        val siguienteIndice = ultimoIndiceCompletado + 1

        // 3. Si todavía hay tests en la lista, lanzamos el siguiente
        if (siguienteIndice < listaDeTests.size) {
            val siguienteTestId = listaDeTests[siguienteIndice]

            val intent = when (siguienteTestId) {
                "t1" -> Intent(context, ReflejosTestActivity::class.java)
                "t2" -> Intent(context, SecuenciaTestActivity::class.java)
                "t3" -> Intent(context, AnticipacionTestActivity::class.java)
                // Aquí agregaremos "t4" cuando lo crees
                else -> null
            }

            if (intent != null) {
                // Importante: Flags para limpiar la pila y evitar volver atrás con el botón físico
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            } else {
                // Si el intent es null, significa que el test existe en la lista pero no tiene Activity aún
                // Por seguridad, podemos ir al reporte o mostrar un aviso
            }
        } else {
            // 4. SI YA NO HAY MÁS TESTS (siguienteIndice >= tamaño de lista)
            // Significa que terminó todo. Aquí lanzaremos el ReporteFinalActivity.
            // Por ahora, puedes poner un Toast o cerrar.
            // Intent(context, ReporteFinalActivity::class.java).also { context.startActivity(it) }
        }
    }

    // Limpia todo para un nuevo operador
    fun resetearEvaluacion() {
        resultados.clear()
        operadorActual = null
    }
}