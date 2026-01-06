package com.example.Cortex_LaSecuencia.actividades

import android.content.res.Configuration
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.utils.AudioManager
import com.example.Cortex_LaSecuencia.utils.AudioManager.TipoSonido

class IntroTestActivity : AppCompatActivity() {

    private lateinit var btnEntendido: Button
    private lateinit var txtDescAnalys: TextView
    private lateinit var txtDescMision: TextView
    private lateinit var txtIcon: TextView
    private lateinit var txtTitle: TextView
    private lateinit var testId: String

    // Variables para guardar los recursos originales del test actual
    private var currentMissionResId: Int = 0
    private var currentAnalysisResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_test)

        testId = intent.getStringExtra("TEST_ID") ?: "t1"

        txtIcon = findViewById(R.id.intro_icon)
        txtTitle = findViewById(R.id.intro_title)
        txtDescAnalys = findViewById(R.id.desc_analysis)
        txtDescMision = findViewById(R.id.desc_mission)
        btnEntendido = findViewById(R.id.btn_entendido)

        cargarDatosDelTest(testId)

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

    private fun cargarDatosDelTest(id: String) {
        // Mapeo directo de ID a Recursos (Strings y Logos)
        // Esto asegura que jale exactamente lo que definimos en strings.xml
        val data = when (id) {
            "t1" -> TestData("⚡", R.string.t1_title, R.string.t1_analysis, R.string.t1_mission)
            "t2" -> TestData("🧠", R.string.t2_title, R.string.t2_analysis, R.string.t2_mission)
            "t3" -> TestData("⏱️", R.string.t3_title, R.string.t3_analysis, R.string.t3_mission)
            "t4" -> TestData("🎯", R.string.t4_title, R.string.t4_analysis, R.string.t4_mission)
            "t5" -> TestData("👁️", R.string.t5_title, R.string.t5_analysis, R.string.t5_mission)
            "t6" -> TestData("田", R.string.t6_title, R.string.t6_analysis, R.string.t6_mission)
            "t7" -> TestData("✋", R.string.t7_title, R.string.t7_analysis, R.string.t7_mission)
            "t8" -> TestData("🗺️", R.string.t8_title, R.string.t8_analysis, R.string.t8_mission)
            "t9" -> TestData("🧭", R.string.t9_title, R.string.t9_analysis, R.string.t9_mission)
            "t10" -> TestData("⚖️", R.string.t10_title, R.string.t10_analysis, R.string.t10_mission)
            else -> TestData("⚡", R.string.t1_title, R.string.t1_analysis, R.string.t1_mission)
        }

        // Guardamos los IDs para usarlos en caso de rotación (resetear texto)
        currentAnalysisResId = data.analysisRes
        currentMissionResId = data.missionRes

        // Asignamos textos usando Html.fromHtml para que funcionen las negritas <b>
        txtIcon.text = data.icon
        txtTitle.text = getString(data.titleRes)
        txtDescAnalys.text = Html.fromHtml(getString(data.analysisRes), Html.FROM_HTML_MODE_LEGACY)
        txtDescMision.text = Html.fromHtml(getString(data.missionRes), Html.FROM_HTML_MODE_LEGACY)

        // Lectura por voz (Opcional: Solo lee el título y la misión para no ser muy largo)
        val textoVoz = "${getString(data.titleRes)}. ${getString(data.missionRes)}"
        AudioManager.hablar(textoVoz)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (testId == "t3") {
            verificarOrientacionParaT3()
        }
    }

    private fun verificarOrientacionParaT3() {
        val orientationWarning = "<br/><br/><b><font color='#ef4444'>⚠️ ¡GIRA EL DISPOSITIVO EN HORIZONTAL PARA CONTINUAR!</font></b>"

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            btnEntendido.isEnabled = true
            btnEntendido.text = "¡ENTENDIDO! 👍"
            // Restaurar texto original de la misión
            txtDescMision.text = Html.fromHtml(getString(currentMissionResId), Html.FROM_HTML_MODE_LEGACY)
        } else {
            btnEntendido.isEnabled = false
            btnEntendido.text = "BLOQUEADO (GIRAR)"
            // Añadir advertencia al texto de la misión
            val originalText = getString(currentMissionResId)
            txtDescMision.text = Html.fromHtml(originalText + orientationWarning, Html.FROM_HTML_MODE_LEGACY)
        }
    }

    // Clase de datos simple para organizar la info
    data class TestData(val icon: String, val titleRes: Int, val analysisRes: Int, val missionRes: Int)
}