package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.AudioManager.TipoSonido

class IntroTestActivity : AppCompatActivity() {

    private lateinit var btnEntendido: Button
    private lateinit var txtDesc: TextView
    private lateinit var testId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_test)

        testId = intent.getStringExtra("TEST_ID") ?: "t1"
        val testInfo = CortexManager.obtenerInfoTest(testId)

        val txtIcon = findViewById<TextView>(R.id.intro_icon)
        val txtTitle = findViewById<TextView>(R.id.intro_title)
        txtDesc = findViewById(R.id.intro_desc)
        btnEntendido = findViewById(R.id.btn_entendido)

        // --- LÓGICA SIMPLIFICADA ---
        txtIcon.text = testInfo.icon
        txtTitle.text = testInfo.title
        txtDesc.text = testInfo.desc
        AudioManager.hablar("${testInfo.title}. ${testInfo.desc}")
        // --- FIN LÓGICA SIMPLIFICADA ---

        // Lógica específica para la orientación del Test 3
        if (testId == "t3") {
            verificarOrientacionParaT3()
        }

        btnEntendido.setOnClickListener {
            AudioManager.reproducirSonido(TipoSonido.CLICK)
            AudioManager.hablar("") // Cancelar TTS anterior
            val intent = CortexManager.obtenerIntentTest(this, testId)
            if (intent != null) {
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (this::testId.isInitialized && testId == "t3") {
            verificarOrientacionParaT3()
        }
    }

    private fun verificarOrientacionParaT3() {
        val testInfo = CortexManager.obtenerInfoTest("t3")
        val orientationWarning = "\n\n⚠️ ¡GIRA EL DISPOSITIVO EN HORIZONTAL PARA CONTINUAR!"

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnEntendido.isEnabled = true
            btnEntendido.text = "¡ENTENDIDO! 👍"
            txtDesc.text = testInfo.desc // Restaurar descripción original
        } else {
            btnEntendido.isEnabled = false
            btnEntendido.text = "BLOQUEADO (GIRAR)"
            txtDesc.text = testInfo.desc + orientationWarning
        }
    }
}