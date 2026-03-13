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

class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        CortexManager.inicializarContexto(this)
        AudioManager.inicializar(this)
        sessionManager = SessionManager(this)

        val logoContainer = findViewById<ConstraintLayout>(R.id.logo_container)
        val iconChip = findViewById<TextView>(R.id.icon_chip)
        val iconCar = findViewById<ImageView>(R.id.icon_dumptruck)
        val txtTitle = findViewById<TextView>(R.id.txt_title)

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

        Handler(Looper.getMainLooper()).postDelayed({
            verificarYRedirigir()
        }, 3000)
    }

    private fun verificarYRedirigir() {
        val intent = if (sessionManager.tieneSesionActiva()) {
            Intent(this, AdminActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}