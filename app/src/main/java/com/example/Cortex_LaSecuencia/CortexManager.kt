package com.example.Cortex_LaSecuencia

import com.example.Cortex_LaSecuencia.actividades.ReflejosTestActivity
import com.example.Cortex_LaSecuencia.actividades.SecuenciaTestActivity
import android.content.Context
import android.content.Intent
// Importamos las actividades desde su nueva ubicación


object CortexManager {

    // Mapa para guardar los resultados de cada test (t1 al t10)
    private val resultados = mutableMapOf<String, Int>()

    // Lista ordenada de los tests según tu HTML
    private val listaDeTests = listOf(
        "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10"
    )

    private var indiceActual = 0

    var operadorActual: Operador? = null // Aquí guardamos al trabajador logueado

    // Guarda el puntaje y prepara el siguiente paso
    fun guardarPuntaje(testId: String, puntaje: Int) {
        resultados[testId] = puntaje
    }

    // Navegación automática entre actividades
    fun navegarAlSiguiente(context: Context) {
        if (indiceActual < listaDeTests.size) {
            val siguienteTestId = listaDeTests[indiceActual]
            indiceActual++

            val intent = when (siguienteTestId) {
                "t1" -> Intent(context, ReflejosTestActivity::class.java)
                "t2" -> Intent(context, SecuenciaTestActivity::class.java)
                //"t3" -> Intent(context, AnticipacionTestActivity::class.java) // Crearemos esta luego
                else -> null
            }

            intent?.let { context.startActivity(it) }
        } else {
            // Aquí irá la pantalla de Reporte Final PDF
        }
    }

    fun resetear() {
        resultados.clear()
        indiceActual = 0
    }
}