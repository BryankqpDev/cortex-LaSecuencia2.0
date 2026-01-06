package com.example.Cortex_LaSecuencia.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.example.Cortex_LaSecuencia.CortexManager

abstract class TestBaseActivity : AppCompatActivity() {
    
    protected lateinit var sentinelManager: SentinelManager
    protected var previewView: PreviewView? = null
    protected var statusTextView: TextView? = null
    protected var testId: String = ""
    protected var testFinalizado = false
    
    // --- ✅ NUEVO: GESTIÓN DE INTERRUPCIONES ---
    private var fueInterrumpido = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        testId = intent.getStringExtra("TEST_ID") ?: obtenerTestId()
        sentinelManager = SentinelManager(this)

        // --- ✅ NUEVO: BLOQUEO DEL BOTÓN ATRÁS ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!testFinalizado) {
                    Toast.makeText(this@TestBaseActivity, "Termina la prueba antes de salir", Toast.LENGTH_SHORT).show()
                } else {
                    // Si el test ya terminó, permite salir
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // --- ✅ NUEVO: CICLO DE VIDA PARA DETECTAR INTERRUPCIONES ---
    override fun onPause() {
        super.onPause()
        if (!testFinalizado) {
            // Si la app pierde el foco (llamada, minimiza, etc.) y el test no ha terminado, se marca
            fueInterrumpido = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (fueInterrumpido && !testFinalizado) {
            // Al volver, si fue interrumpido, se anula el intento
            fueInterrumpido = false
            anularIntentoPorInterrupcion()
        }
    }

    /**
     * ✅ NUEVA FUNCIÓN: Centraliza la lógica de penalización por interrupción.
     */
    private fun anularIntentoPorInterrupcion() {
        if (testFinalizado) return
        testFinalizado = true // Marcar como finalizado para evitar doble lógica

        val eraPrimerIntento = CortexManager.obtenerIntentoActual(testId) == 1

        // Guardar puntaje 0 y registrar la métrica de fallo
        CortexManager.guardarPuntaje(testId, 0)
        CortexManager.logPerformanceMetric(testId, 0, mapOf("causa" to "interrupcion_externa"))

        if (eraPrimerIntento) {
            // Si le quedan intentos, se lo notificamos y reiniciamos la prueba
            Toast.makeText(this, "Intento anulado por interrupción. Tienes 1 intento más.", Toast.LENGTH_LONG).show()
            recreate() // Reinicia la actividad para el segundo intento
        } else {
            // Si ya no le quedan intentos, se lo notificamos y avanzamos
            Toast.makeText(this, "Prueba anulada por interrupción.", Toast.LENGTH_LONG).show()
            CortexManager.navegarAlSiguiente(this)
            finish()
        }
    }
    
    abstract fun obtenerTestId(): String
    
    protected fun iniciarSentinel(previewView: PreviewView, statusTextView: TextView) {
        this.previewView = previewView
        this.statusTextView = statusTextView
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return
        }
        
        sentinelManager.iniciarSentinel(
            previewView = previewView,
            onFaceDetected = { presente ->
                if (!presente && !testFinalizado) {
                   // Lógica de ausencia (desactivada temporalmente para no interferir)
                }
            },
            onAbsenceTimeout = {
                if (!testFinalizado) {
                   // fallarPorSeguridad()
                }
            },
            statusTextView = statusTextView
        )
    }
    
    protected fun fallarPorSeguridad() {
        if (testFinalizado) return
        testFinalizado = true
        sentinelManager.detenerSentinel()
        Toast.makeText(this, "Prueba anulada por seguridad (ausencia)", Toast.LENGTH_LONG).show()
        CortexManager.guardarPuntaje(testId, 0)
        CortexManager.logPerformanceMetric(testId, 0, mapOf("causa" to "ausencia_prolongada"))
        CortexManager.navegarAlSiguiente(this)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sentinelManager.detenerSentinel()
    }
}