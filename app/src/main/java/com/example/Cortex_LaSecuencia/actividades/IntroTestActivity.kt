package com.example.Cortex_LaSecuencia.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.AudioManager.TipoSonido

class IntroTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_test)

        val testId = intent.getStringExtra("TEST_ID") ?: "t1"
        val testInfo = CortexManager.obtenerInfoTest(testId)

        val txtIcon = findViewById<TextView>(R.id.intro_icon)
        val txtTitle = findViewById<TextView>(R.id.intro_title)
        val txtDesc = findViewById<TextView>(R.id.intro_desc)
        val btnEntendido = findViewById<Button>(R.id.btn_entendido)

        // Configurar icono (usando emojis como en HTML)
        txtIcon.text = testInfo.icon
        txtTitle.text = testInfo.title
        txtDesc.text = testInfo.desc

        // Hablar descripción (como en HTML: speak)
        AudioManager.hablar("${testInfo.title}. ${testInfo.desc}")

        // Botón "¡ENTENDIDO!" - navega al test correspondiente
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
}

