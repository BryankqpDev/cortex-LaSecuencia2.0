package com.example.Cortex_LaSecuencia.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Obtener testId del intent
        testId = intent.getStringExtra("TEST_ID") ?: obtenerTestId()
        
        // Inicializar Sentinel
        sentinelManager = SentinelManager(this)
    }
    
    abstract fun obtenerTestId(): String
    
    protected fun iniciarSentinel(previewView: PreviewView, statusTextView: TextView) {
        this.previewView = previewView
        this.statusTextView = statusTextView
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return
        }
        
        sentinelManager.iniciarSentinel(
            previewView = previewView,
            onFaceDetected = { presente ->
                if (!presente && !testFinalizado) {
                    mostrarAlertaSeguridad()
                }
            },
            onAbsenceTimeout = {
                if (!testFinalizado) {
                    fallarPorSeguridad()
                }
            },
            statusTextView = statusTextView
        )
    }
    
    protected fun iniciarSentinelOpcional(previewView: PreviewView?, statusTextView: TextView?) {
        if (previewView != null && statusTextView != null) {
            iniciarSentinel(previewView, statusTextView)
        }
    }
    
    private fun mostrarAlertaSeguridad() {
        // Mostrar alerta de seguridad (como en HTML: security-alert)
        AlertDialog.Builder(this)
            .setTitle("ALERTA DE SEGURIDAD")
            .setMessage("NO SE DETECTA AL USUARIO\n\nREGRESE A LA CÁMARA")
            .setCancelable(false)
            .setPositiveButton("REINICIAR TEST") { _, _ ->
                fallarPorSeguridad()
            }
            .show()
    }
    
    protected fun fallarPorSeguridad() {
        testFinalizado = true
        sentinelManager.detenerSentinel()
        Toast.makeText(this, "Prueba anulada por seguridad", Toast.LENGTH_LONG).show()
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sentinelManager.detenerSentinel()
    }
}

