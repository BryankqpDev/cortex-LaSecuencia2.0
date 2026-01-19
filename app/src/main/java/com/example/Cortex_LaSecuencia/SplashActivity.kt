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

        // Inicializar contexto para bloqueo
        CortexManager.inicializarContexto(this)
        
        // Inicializar AudioManager (TTS y sonidos)
        AudioManager.inicializar(this)

        // Verificar bloqueo primero
        if (CortexManager.estaBloqueado()) {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, LockedActivity::class.java)
                startActivity(intent)
                finish()
            }, 1500) // Mostrar splash un poco antes de redirigir
            return
        }

        // Animaciones
        val logoContainer = findViewById<ConstraintLayout>(R.id.logo_container)
        val iconChip = findViewById<TextView>(R.id.icon_chip)
        val iconCar = findViewById<ImageView>(R.id.icon_dumptruck)
        val txtTitle = findViewById<TextView>(R.id.txt_title)

        // Animación de entrada del logo
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 800
        logoContainer.startAnimation(fadeIn)

        // Animación del chip (pulse)
        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        pulse.duration = 2000
        pulse.repeatMode = Animation.REVERSE
        pulse.repeatCount = Animation.INFINITE
        iconChip.startAnimation(pulse)

        // Animación del auto (drive-in)
        val driveIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        driveIn.duration = 1500
        iconCar.startAnimation(driveIn)

        // Animación del título
        val titleFade = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        titleFade.duration = 1000
        titleFade.startOffset = 500
        txtTitle.startAnimation(titleFade)

        // Redirigir después de 3 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
