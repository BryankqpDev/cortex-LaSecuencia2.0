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
import com.example.Cortex_LaSecuencia.SessionManager  // ✅ AGREGAR
import com.example.Cortex_LaSecuencia.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager  // ✅ AGREGAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Inicializaciones básicas
        CortexManager.inicializarContexto(this)
        AudioManager.inicializar(this)
        sessionManager = SessionManager(this)  // ✅ AGREGAR

        // 2. Verificar si el sistema está bloqueado por 24h
        if (CortexManager.estaBloqueado()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, LockedActivity::class.java)
                startActivity(intent)
                finish()
            }, 1500)
            return
        }

        // 3. Referencias para animaciones
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

        // 4. ✅ VERIFICAR SESIÓN Y REDIRIGIR INTELIGENTEMENTE
        Handler(Looper.getMainLooper()).postDelayed({
            verificarYRedirigir()
        }, 3000)
    }

    private fun verificarYRedirigir() {
        // 🔥 Verificar si hay sesión de admin activa
        val intent = if (sessionManager.tieneSesionActiva()) {
            // ✅ HAY SESIÓN → Ir directo al panel de administrador
            android.util.Log.d("SplashActivity", "✅ Sesión activa detectada → AdminActivity")
            Intent(this, AdminActivity::class.java)
        } else {
            // ❌ SIN SESIÓN → Ir al registro de conductores
            android.util.Log.d("SplashActivity", "❌ Sin sesión → MainActivity (Conductores)")
            Intent(this, MainActivity::class.java)
        }

        // Limpiar el stack de actividades
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}