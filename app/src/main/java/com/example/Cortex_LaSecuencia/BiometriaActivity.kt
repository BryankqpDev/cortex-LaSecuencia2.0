package com.example.Cortex_LaSecuencia

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.Cortex_LaSecuencia.actividades.WelcomeActivity
import com.example.Cortex_LaSecuencia.utils.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BIOMETRIA ACTIVITY - VERSIÓN CON LÓGICA COMPLETA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * FLUJO AUTOMÁTICO:
 * 1. Al iniciar, verifica si el DNI ya tiene registro en Firebase
 * 2. Si NO existe → Modo REGISTRO (guarda datos)
 * 3. Si SÍ existe → Modo VALIDACIÓN (compara con datos guardados)
 *
 * BYPASS TEMPORAL:
 * - Para desarrollo, puedes desactivar liveness con flag
 * - En producción, liveness debe estar activo
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
class BiometriaActivity : AppCompatActivity() {

    // --- Elementos de UI ---
    private lateinit var previewView: PreviewView
    private lateinit var btnCapturar: View
    private lateinit var imgPreview: ImageView
    private lateinit var txtInstruccion: TextView
    private lateinit var cardVerificacion: CardView
    private lateinit var cardConfirmacion: CardView
    private lateinit var btnConfirmar: View
    private lateinit var btnReintentar: View
    private lateinit var iconCheck1: ImageView
    private lateinit var iconCheck2: ImageView
    private lateinit var iconCheck3: ImageView
    private lateinit var iconCheck4: ImageView
    private lateinit var progressCheck4: ProgressBar

    // --- Lógica de Cámara y AI ---
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var biometricValidator: BiometricValidator
    private var fotoBitmap: Bitmap? = null

    // --- Estado ---
    private var estaAutenticado = false

    // ═══════════════════════════════════════════════════════════════════════
    // ⚙️ CONFIGURACIÓN DE BYPASS (Solo para desarrollo)
    // ═══════════════════════════════════════════════════════════════════════
    private val BYPASS_LIVENESS_DESARROLLO = true // ← false en producción

    private val operadorDni: String
        get() = CortexManager.operadorActual?.dni ?: "invitado_pruebas"

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 MODO AUTOMÁTICO: Se determina según si ya existe registro
    // ═══════════════════════════════════════════════════════════════════════
    private var modo = "registro" // Se actualizará automáticamente

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "BiometriaActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometria)

        initViews()
        cameraExecutor = Executors.newSingleThreadExecutor()
        biometricValidator = BiometricValidator(this)

        autenticarUsuario()
    }

    private fun autenticarUsuario() {
        txtInstruccion.text = "Conectando con Cortex..."

        CortexManager.autenticarConductorAnonimo(
            onSuccess = { user ->
                estaAutenticado = true

                // ═══════════════════════════════════════════════════════════
                // 🔍 VERIFICAR SI YA EXISTE REGISTRO BIOMÉTRICO
                // ═══════════════════════════════════════════════════════════
                verificarRegistroExistente()

                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error de conexión: $error", Toast.LENGTH_LONG).show()
                txtInstruccion.text = "Error de conexión. Revisa tu internet."
            }
        )
    }

    /**
     * ════════════════════════════════════════════════════════════════════════
     * 🔍 VERIFICA SI EL USUARIO YA TIENE DATOS BIOMÉTRICOS GUARDADOS
     * ════════════════════════════════════════════════════════════════════════
     */
    private fun verificarRegistroExistente() {
        lifecycleScope.launch {
            val existeRegistro = biometricValidator.usuarioTieneRegistro(operadorDni)

            if (existeRegistro) {
                modo = "autenticacion"
                txtInstruccion.text = "Valida tu identidad con tu rostro"
                Log.i(TAG, "✅ Usuario $operadorDni ya registrado → Modo AUTENTICACIÓN")
            } else {
                modo = "registro"
                txtInstruccion.text = "Primera vez: Registra tu rostro"
                Log.i(TAG, "🆕 Usuario $operadorDni nuevo → Modo REGISTRO")
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                setupListeners()
            } catch (exc: Exception) {
                Log.e(TAG, "Error binding camera", exc)
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturarFoto() {
        if (!estaAutenticado) {
            Toast.makeText(this, "Esperando conexión...", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: return
        btnCapturar.isEnabled = false
        txtInstruccion.text = "Capturando..."

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                fotoBitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()

                if (fotoBitmap != null) {
                    mostrarUIProcesando()
                    procesarBiometriaReal(fotoBitmap!!)
                } else {
                    handleError("Error al procesar la imagen")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                handleError("Error captura: ${exception.message}")
            }
        })
    }

    private fun mostrarUIProcesando() {
        imgPreview.setImageBitmap(fotoBitmap)
        imgPreview.visibility = View.VISIBLE
        previewView.visibility = View.GONE
        btnCapturar.visibility = View.GONE

        cardVerificacion.visibility = View.VISIBLE
        cardVerificacion.alpha = 0f
        cardVerificacion.animate().alpha(1f).setDuration(300).start()

        resetVerificationIcons()
    }

    /**
     * ════════════════════════════════════════════════════════════════════════
     * 🧠 PROCESAMIENTO BIOMÉTRICO CON BYPASS OPCIONAL
     * ════════════════════════════════════════════════════════════════════════
     */
    private fun procesarBiometriaReal(bitmap: Bitmap) {
        lifecycleScope.launch {
            // --- PASO 1: CALIDAD DE IMAGEN ---
            txtInstruccion.text = "Analizando calidad..."
            val qualityResult = biometricValidator.validateCaptureQuality(bitmap)

            if (qualityResult is CaptureQualityResult.Valid) {
                activarCheck(iconCheck1)
            } else {
                val errorMsg = if (qualityResult is CaptureQualityResult.PoorQuality)
                    qualityResult.issues.first() else "Rostro no detectado"
                handleError(errorMsg)
                return@launch
            }

            // --- PASO 2: LIVENESS (con bypass opcional) ---
            activarCheck(iconCheck2)

            if (BYPASS_LIVENESS_DESARROLLO) {
                Log.w(TAG, "⚠️ DESARROLLO: Bypass de liveness activo")
                txtInstruccion.text = "Liveness verificado (BYPASS)"
            } else {
                txtInstruccion.text = "Verificando prueba de vida..."
            }

            // --- PASO 3: RECONOCIMIENTO / REGISTRO ---
            txtInstruccion.text = if (modo == "registro") "Registrando identidad..." else "Validando identidad..."
            activarCheck(iconCheck3)

            progressCheck4.visibility = View.VISIBLE
            iconCheck4.visibility = View.GONE

            // ═══════════════════════════════════════════════════════════════
            // 🎯 DECISIÓN AUTOMÁTICA: Registro o Autenticación
            // ═══════════════════════════════════════════════════════════════
            if (modo == "registro") {
                Log.i(TAG, "📝 Iniciando REGISTRO para DNI: $operadorDni")
                registrarUsuario(bitmap)
            } else {
                Log.i(TAG, "🔐 Iniciando AUTENTICACIÓN para DNI: $operadorDni")
                autenticarUsuario(bitmap)
            }
        }
    }

    /**
     * ════════════════════════════════════════════════════════════════════════
     * 📝 REGISTRO: Guarda hash facial + foto en Firebase
     * ════════════════════════════════════════════════════════════════════════
     */
    private suspend fun registrarUsuario(bitmap: Bitmap) {
        val resultado = if (BYPASS_LIVENESS_DESARROLLO) {
            // Versión sin liveness (para desarrollo)
            biometricValidator.enrollUser(operadorDni, listOf(bitmap), verificarLiveness = false)
        } else {
            // Versión completa con liveness (producción)
            biometricValidator.enrollUser(operadorDni, listOf(bitmap), verificarLiveness = true)
        }

        progressCheck4.visibility = View.GONE
        iconCheck4.visibility = View.VISIBLE

        when (resultado) {
            is EnrollmentResult.Success -> {
                activarCheck(iconCheck4)
                txtInstruccion.text = "✅ Registro exitoso"

                // Guardar foto localmente también (opcional)
                guardarFotoLocalmente(bitmap, operadorDni)

                Log.i(TAG, "✅ Usuario $operadorDni registrado exitosamente")
                mostrarConfirmacion("Identidad registrada correctamente")
            }
            is EnrollmentResult.SpoofDetected -> {
                Log.w(TAG, "⚠️ Spoofing detectado: ${resultado.reason}")
                handleError("⚠️ ALERTA: ${resultado.reason}")
            }
            is EnrollmentResult.Failed -> {
                Log.e(TAG, "❌ Error en registro: ${resultado.reason}")
                handleError("Error: ${resultado.reason}")
            }
        }
    }

    /**
     * ════════════════════════════════════════════════════════════════════════
     * 🔐 AUTENTICACIÓN: Compara hash actual con el guardado en Firebase
     * ════════════════════════════════════════════════════════════════════════
     */
    private suspend fun autenticarUsuario(bitmap: Bitmap) {
        val resultado = if (BYPASS_LIVENESS_DESARROLLO) {
            // Versión sin liveness (para desarrollo)
            biometricValidator.authenticateUser(operadorDni, listOf(bitmap), verificarLiveness = false)
        } else {
            // Versión completa con liveness (producción)
            biometricValidator.authenticateUser(operadorDni, listOf(bitmap), verificarLiveness = true)
        }

        progressCheck4.visibility = View.GONE
        iconCheck4.visibility = View.VISIBLE

        when (resultado) {
            is AuthenticationResult.Success -> {
                activarCheck(iconCheck4)
                val confianza = (resultado.confidence * 100).toInt()
                txtInstruccion.text = "✅ Identidad Verificada ($confianza%)"

                Log.i(TAG, "✅ Usuario $operadorDni autenticado (confianza: $confianza%)")
                mostrarConfirmacion("Identidad verificada correctamente")
            }
            is AuthenticationResult.NoMatch -> {
                Log.w(TAG, "❌ Rostro no coincide con el registro")
                handleError("❌ Rostro no coincide con tu registro")
            }
            is AuthenticationResult.SpoofDetected -> {
                Log.w(TAG, "⚠️ Spoofing detectado: ${resultado.reason}")
                handleError("⚠️ ALERTA: ${resultado.reason}")
            }
            else -> {
                Log.e(TAG, "❌ Error en autenticación")
                handleError("Error en autenticación")
            }
        }
    }

    private fun mostrarConfirmacion(mensaje: String) {
        cardVerificacion.animate().alpha(0f).setDuration(300).withEndAction {
            cardVerificacion.visibility = View.GONE
            cardConfirmacion.visibility = View.VISIBLE
            cardConfirmacion.alpha = 0f
            cardConfirmacion.animate().alpha(1f).setDuration(300).start()

            // Mostrar mensaje personalizado
            txtInstruccion.text = mensaje
        }.start()
    }

    private fun confirmarYGuardar() {
        Toast.makeText(this, "✓ Proceso finalizado", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Funciones Auxiliares ---

    private fun activarCheck(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_check_circle)
        icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light))
        animateCheckIcon(icon)
    }

    private fun handleError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        txtInstruccion.text = msg
        btnCapturar.isEnabled = true
        reintentar()
    }

    private fun reintentar() {
        cardVerificacion.visibility = View.GONE
        cardConfirmacion.visibility = View.GONE
        imgPreview.visibility = View.GONE
        previewView.visibility = View.VISIBLE
        btnCapturar.visibility = View.VISIBLE
        btnCapturar.isEnabled = true

        resetVerificationIcons()
        txtInstruccion.text = if (modo == "registro") "Inténtalo de nuevo" else "Valida tu identidad"
    }

    private fun resetVerificationIcons() {
        val grayColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val icons = listOf(iconCheck1, iconCheck2, iconCheck3, iconCheck4)

        icons.forEach { icon ->
            icon.setImageResource(R.drawable.ic_circle_outline)
            icon.clearColorFilter()
            icon.setColorFilter(grayColor)
        }
        progressCheck4.visibility = View.GONE
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            matrix.postScale(-1f, 1f)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    private fun guardarFotoLocalmente(bitmap: Bitmap, dni: String) {
        try {
            val fileName = "selfie_${dni}_${System.currentTimeMillis()}.jpg"
            val file = File(getExternalFilesDir(null), fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.i(TAG, "📸 Foto guardada localmente: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando foto local", e)
        }
    }

    // --- Inicialización de Vistas ---

    private fun initViews() {
        previewView = findViewById(R.id.preview_view)
        btnCapturar = findViewById(R.id.btn_capturar)
        imgPreview = findViewById(R.id.img_preview)
        txtInstruccion = findViewById(R.id.txt_instruccion)
        cardVerificacion = findViewById(R.id.card_verificacion)
        cardConfirmacion = findViewById(R.id.card_confirmacion)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        btnReintentar = findViewById(R.id.btn_reintentar)
        iconCheck1 = findViewById(R.id.icon_check_1)
        iconCheck2 = findViewById(R.id.icon_check_2)
        iconCheck3 = findViewById(R.id.icon_check_3)
        iconCheck4 = findViewById(R.id.icon_check_4)
        progressCheck4 = findViewById(R.id.progress_check_4)
    }

    private fun setupListeners() {
        btnCapturar.setOnClickListener { animateButton(btnCapturar); capturarFoto() }
        btnConfirmar.setOnClickListener { confirmarYGuardar() }
        btnReintentar.setOnClickListener { reintentar() }
    }

    private fun animateCheckIcon(icon: ImageView) {
        icon.scaleX = 0f; icon.scaleY = 0f
        icon.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
    }

    private fun animateButton(button: View) {
        ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f, 1f).apply { duration = 200; start() }
        ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f, 1f).apply { duration = 200; start() }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        biometricValidator.cleanup()
    }
}