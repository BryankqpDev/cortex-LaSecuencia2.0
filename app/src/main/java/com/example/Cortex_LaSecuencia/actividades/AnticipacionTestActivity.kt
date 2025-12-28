package com.example.Cortex_LaSecuencia.actividades

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import kotlin.math.abs

/**
 * ACTIVIDAD: Test de Anticipación (Módulo 3)
 * OBJETIVO: Evaluar la capacidad del operador para calcular tiempo y distancia (TTC).
 * LÓGICA: Un vehículo se mueve a velocidad constante y el usuario debe detenerlo
 * dentro de una zona objetivo (Zona Verde).
 */
class AnticipacionTestActivity : AppCompatActivity() {

    // --- ELEMENTOS DE LA INTERFAZ (UI) ---
    private lateinit var vehiculo: ImageView // El camión o vehículo que se mueve
    private lateinit var zonaMeta: View      // La zona verde donde se debe frenar
    private lateinit var btnFrenar: Button   // El botón gigante de frenado
    private lateinit var pista: View         // El contenedor que define el largo del recorrido

    // --- VARIABLES DE LÓGICA ---
    private var animador: ObjectAnimator? = null // Objeto que controla la animación de movimiento
    private var juegoActivo = false              // Bandera para evitar frenar dos veces o antes de tiempo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anticipacion_test)

        // 1. VINCULACIÓN DE VISTAS
        // Conectamos las variables con los IDs del archivo XML (activity_anticipacion_test.xml)
        vehiculo = findViewById(R.id.img_vehiculo)
        zonaMeta = findViewById(R.id.zona_meta)
        btnFrenar = findViewById(R.id.btn_frenar)
        pista = findViewById(R.id.pista_container)

        // 2. LISTENER DEL BOTÓN
        // Solo permitimos frenar si el juego ya comenzó (juegoActivo = true)
        btnFrenar.setOnClickListener {
            if (juegoActivo) frenarVehiculo()
        }

        // 3. INICIO AUTOMÁTICO
        // Damos 1 segundo (1000ms) de espera para que el usuario se prepare antes de mover el camión
        Handler(Looper.getMainLooper()).postDelayed({
            iniciarCarrera()
        }, 1000)
    }

    /**
     * Inicia la animación del vehículo de izquierda a derecha.
     */
    private fun iniciarCarrera() {
        juegoActivo = true

        // Calculamos cuánto debe recorrer el vehículo (Ancho de la pista - Ancho del propio vehículo)
        val anchoPista = pista.width.toFloat()
        val anchoVehiculo = vehiculo.width.toFloat()

        // Configuración de la Animación (TranslationX = Movimiento horizontal)
        animador = ObjectAnimator.ofFloat(vehiculo, "translationX", 0f, anchoPista - anchoVehiculo).apply {
            duration = 2500 // TIEMPO DE CRUCE: 2.5 segundos (Modificar este valor cambia la dificultad)
            interpolator = LinearInterpolator() // Velocidad constante (sin acelerar ni frenar)

            // Listener para detectar si la animación termina por sí sola
            doOnEnd {
                // Si la animación termina y el juego seguía activo, significa que el usuario NO frenó
                if (juegoActivo) evaluarFrenado(true)
            }
            start() // ¡Arranca el motor!
        }
    }

    /**
     * Detiene el vehículo inmediatamente.
     */
    private fun frenarVehiculo() {
        juegoActivo = false // Bloqueamos el botón para no recibir más clics
        animador?.pause()   // Congelamos la animación en el punto exacto
        evaluarFrenado(false) // Evaluamos la posición actual
    }

    /**
     * Calcula la precisión del frenado.
     * @param falloTotal: True si el usuario dejó pasar el camión sin tocar el botón.
     */
    private fun evaluarFrenado(falloTotal: Boolean) {
        // CASO 1: El usuario se durmió y no frenó
        if (falloTotal) {
            mostrarResultado(0, "¡REACCIÓN TARDÍA! ❌")
            return
        }

        // CASO 2: El usuario frenó, calculamos la precisión matemática

        // Obtenemos el centro geométrico del vehículo (Posición X + mitad del ancho)
        val centroVehiculo = vehiculo.x + (vehiculo.width / 2)

        // Obtenemos el centro geométrico de la zona meta
        val centroMeta = zonaMeta.x + (zonaMeta.width / 2)

        // Calculamos la distancia absoluta (sin negativo) entre los dos centros
        val diferencia = abs(centroVehiculo - centroMeta)

        // Definimos el radio de tolerancia (mitad del ancho de la zona verde)
        val radioMeta = zonaMeta.width / 2

        // SISTEMA DE PUNTUACIÓN INDUSTRIAL
        val puntaje = when {
            diferencia < (radioMeta * 0.5) -> 100 // Precisión quirúrgica (Centro exacto)
            diferencia < radioMeta -> 80      // Dentro de la zona verde (Aprobado)
            diferencia < (radioMeta * 1.5) -> 40 // Rozando el borde (Zona de peligro)
            else -> 0                              // Muy lejos (Fallo)
        }

        // Mensaje de feedback según el resultado
        val mensaje = if (puntaje >= 80) "¡BUEN CÁLCULO! 😎" else "CALIBRACIÓN NECESARIA ⚠️"
        mostrarResultado(puntaje, mensaje)
    }

    /**
     * Muestra el resultado final y gestiona la navegación.
     */
    private fun mostrarResultado(puntaje: Int, mensaje: String) {
        // 1. Guardamos el resultado en el "Cerebro" central de la app
        CortexManager.guardarPuntaje("t3", puntaje)

        // 2. Mostramos el diálogo informativo
        AlertDialog.Builder(this)
            .setTitle("RESULTADO T3")
            .setMessage("Precisión: $puntaje%\n$mensaje")
            .setCancelable(false) // Obligamos a usar el botón
            .setPositiveButton("SIGUIENTE") { _, _ ->
                // 3. El Manager decide cuál es el siguiente test (Test 4)
                CortexManager.navegarAlSiguiente(this)
                finish() // Cerramos esta actividad para liberar memoria
            }
            .show()
    }
}