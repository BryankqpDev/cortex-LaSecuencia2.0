package com.example.Cortex_LaSecuencia

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

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Inicializaciones básicas
        CortexManager.inicializarContexto(this)
        AudioManager.inicializar(this)

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

        // 4. 🔴 SOLUCIÓN: SIEMPRE IR A MAINACTIVITY (CONDUCTORES) 🔴
        // Ignoramos cualquier sesión activa de Administrador al inicio.
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            // Aseguramos que sea una tarea limpia
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 3000)
    }
}
