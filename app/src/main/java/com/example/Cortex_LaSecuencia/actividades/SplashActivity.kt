package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.SessionManager
import com.example.Cortex_LaSecuencia.MainActivity

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SPLASH ACTIVITY - VERSIÓN CORREGIDA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * CAMBIO CRÍTICO:
 * ❌ ELIMINADA la verificación de bloqueo en onCreate()
 * ✅ El bloqueo se verifica SOLO en MainActivity al presionar SIGUIENTE
 *
 * Flujo:
 * 1. Splash con animaciones
 * 2. Verifica sesión de admin
 * 3. Redirige a MainActivity o AdminActivity
 * 4. NO verifica bloqueo aquí
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Inicializaciones básicas
        CortexManager.inicializarContexto(this)
        AudioManager.inicializar(this)
        sessionManager = SessionManager(this)

        // ═══════════════════════════════════════════════════════════════════
        // ✅ ELIMINADO: Verificación de bloqueo
        // ═══════════════════════════════════════════════════════════════════
        // La verificación ahora ocurre SOLO cuando el usuario presiona
        // "SIGUIENTE" en MainActivity después de llenar el formulario
        // ═══════════════════════════════════════════════════════════════════

        // 2. Referencias para animaciones
        val logoContainer = findViewById<ConstraintLayout>(R.id.logo_container)
        val iconChip = findViewById<TextView>(R.id.icon_chip)
        val iconCar = findViewById<ImageView>(R.id.icon_dumptruck)
        val txtTitle = findViewById<TextView>(R.id.txt_title)

        // --- ANIMACIONES ---
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 800
        logoContainer.startAnimation(fadeIn)

        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        pulse.duration = 2000
        pulse.repeatMode = Animation.REVERSE
        pulse.repeatCount = Animation.INFINITE
        iconChip.startAnimation(pulse)

        val driveIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        driveIn.duration = 1500
        iconCar.startAnimation(driveIn)

        val titleFade = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        titleFade.duration = 1000
        titleFade.startOffset = 500
        txtTitle.startAnimation(titleFade)

        // 3. Verificar sesión y redirigir
        Handler(Looper.getMainLooper()).postDelayed({
            verificarYRedirigir()
        }, 3000)
    }

    /**
     * ════════════════════════════════════════════════════════════════════════
     * REDIRECCIÓN INTELIGENTE (Sin verificar bloqueo)
     * ════════════════════════════════════════════════════════════════════════
     */
    private fun verificarYRedirigir() {
        val intent = if (sessionManager.tieneSesionActiva()) {
            // Hay sesión de admin → Ir al panel
            android.util.Log.d("SplashActivity", "✅ Sesión activa → AdminActivity")
            Intent(this, AdminActivity::class.java)
        } else {
            // Sin sesión → Ir al registro de conductores
            android.util.Log.d("SplashActivity", "❌ Sin sesión → MainActivity")
            Intent(this, MainActivity::class.java)
        }

        // Limpiar stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}