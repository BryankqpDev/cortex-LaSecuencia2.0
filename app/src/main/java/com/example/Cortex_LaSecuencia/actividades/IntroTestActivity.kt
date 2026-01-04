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
        val intentoActual = CortexManager.obtenerIntentoActual(testId)

        val txtIcon = findViewById<TextView>(R.id.intro_icon)
        val txtTitle = findViewById<TextView>(R.id.intro_title)
        txtDesc = findViewById(R.id.intro_desc)
        btnEntendido = findViewById(R.id.btn_entendido)

        txtIcon.text = testInfo.icon

        // Configura el texto basado en el número de intento
        if (intentoActual == 2) {
            txtTitle.text = "${testInfo.title} (INTENTO 2)"
            txtDesc.text = "Continuamos con el segundo intento.\n\n${testInfo.desc}"
            btnEntendido.text = "¡LISTO! (INTENTO 2)"
            AudioManager.hablar("Continuamos con el segundo intento. ${testInfo.desc}")
        } else {
            txtTitle.text = testInfo.title
            txtDesc.text = testInfo.desc
            AudioManager.hablar("${testInfo.title}. ${testInfo.desc}")
        }

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
        // Vuelve a verificar la orientación si es el test relevante
        if (this::testId.isInitialized && testId == "t3") {
            verificarOrientacionParaT3()
        }
    }

    private fun verificarOrientacionParaT3() {
        val testInfo = CortexManager.obtenerInfoTest("t3")
        val intentoActual = CortexManager.obtenerIntentoActual("t3")
        val baseDesc = if (intentoActual == 2) {
            "Continuamos con el segundo intento.\n\n${testInfo.desc}"
        } else {
            testInfo.desc
        }
        val orientationWarning = "\n\n⚠️ ¡GIRA EL DISPOSITIVO EN HORIZONTAL PARA CONTINUAR!"

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnEntendido.isEnabled = true
            btnEntendido.text = if (intentoActual == 2) "¡LISTO! (INTENTO 2)" else "¡ENTENDIDO! 👍"
            txtDesc.text = baseDesc // Restaura la descripción original
        } else {
            btnEntendido.isEnabled = false
            btnEntendido.text = "BLOQUEADO (GIRAR)"
            txtDesc.text = baseDesc + orientationWarning // Añade el aviso
        }
    }
}