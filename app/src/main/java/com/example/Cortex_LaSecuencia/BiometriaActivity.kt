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

class BiometriaActivity : AppCompatActivity() {

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

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var biometricValidator: BiometricValidator
    private var fotoBitmap: Bitmap? = null
    private var estaAutenticado = false
    private val BYPASS_LIVENESS_DESARROLLO = true 
    private var modo = "registro"

    private val operadorDni: String
        get() = CortexManager.operadorActual?.dni ?: "invitado"

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
        CortexManager.autenticarConductorAnonimo(
            onSuccess = { 
                estaAutenticado = true
                verificarRegistroExistente()
                if (allPermissionsGranted()) startCamera()
                else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            },
            onError = { Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show() }
        )
    }

    private fun verificarRegistroExistente() {
        lifecycleScope.launch {
            modo = if (biometricValidator.usuarioTieneRegistro(operadorDni)) "autenticacion" else "registro"
            txtInstruccion.text = if (modo == "registro") "Registra tu rostro" else "Valida tu identidad"
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                setupListeners()
            } catch (exc: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturarFoto() {
        val imageCapture = imageCapture ?: return
        btnCapturar.isEnabled = false
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                fotoBitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()
                if (fotoBitmap != null) {
                    mostrarUIProcesando()
                    procesarBiometriaReal(fotoBitmap!!)
                }
            }
            override fun onError(exception: ImageCaptureException) { btnCapturar.isEnabled = true }
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

    private fun procesarBiometriaReal(bitmap: Bitmap) {
        lifecycleScope.launch {
            val qualityResult = biometricValidator.validateCaptureQuality(bitmap)
            if (qualityResult is CaptureQualityResult.Valid) {
                activarCheck(iconCheck1); activarCheck(iconCheck2); activarCheck(iconCheck3)
                if (modo == "registro") registrarUsuario(bitmap) else autenticarUsuario(bitmap)
            } else { handleError("Rostro no detectado") }
        }
    }

    private suspend fun registrarUsuario(bitmap: Bitmap) {
        val resultado = biometricValidator.enrollUser(operadorDni, listOf(bitmap), !BYPASS_LIVENESS_DESARROLLO)
        if (resultado is EnrollmentResult.Success) {
            activarCheck(iconCheck4)
            // ✅ GUARDAR CON NOMBRE FIJO PARA EL REPORTE
            guardarFotoLocalmente(bitmap, operadorDni)
            mostrarConfirmacion("Registro completado")
        } else { handleError("Fallo en registro") }
    }

    private suspend fun autenticarUsuario(bitmap: Bitmap) {
        val resultado = biometricValidator.authenticateUser(operadorDni, listOf(bitmap), !BYPASS_LIVENESS_DESARROLLO)
        if (resultado is AuthenticationResult.Success) {
            activarCheck(iconCheck4)
            // ✅ TAMBIÉN ACTUALIZAMOS LA FOTO LOCAL PARA EL REPORTE ACTUAL
            guardarFotoLocalmente(bitmap, operadorDni)
            mostrarConfirmacion("Identidad verificada")
        } else { handleError("Identidad no coincide") }
    }

    private fun guardarFotoLocalmente(bitmap: Bitmap, dni: String) {
        try {
            // ✅ RUTA INTERNA Y NOMBRE FIJO: selfie_DNI.jpg
            val file = File(filesDir, "selfie_$dni.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.i(TAG, "📸 Foto guardada para reporte: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando foto", e)
        }
    }

    private fun mostrarConfirmacion(mensaje: String) {
        progressCheck4.visibility = View.GONE
        iconCheck4.visibility = View.VISIBLE
        cardVerificacion.animate().alpha(0f).setDuration(300).withEndAction {
            cardVerificacion.visibility = View.GONE
            cardConfirmacion.visibility = View.VISIBLE
            cardConfirmacion.alpha = 0f
            cardConfirmacion.animate().alpha(1f).setDuration(300).start()
            txtInstruccion.text = mensaje
        }.start()
    }

    private fun confirmarYGuardar() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    private fun activarCheck(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_check_circle)
        icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light))
        animateCheckIcon(icon)
    }

    private fun handleError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
    }

    private fun resetVerificationIcons() {
        val icons = listOf(iconCheck1, iconCheck2, iconCheck3, iconCheck4)
        icons.forEach { it.setImageResource(R.drawable.ic_circle_outline); it.clearColorFilter() }
        progressCheck4.visibility = View.GONE
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()); postScale(-1f, 1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun initViews() {
        previewView = findViewById(R.id.preview_view); btnCapturar = findViewById(R.id.btn_capturar)
        imgPreview = findViewById(R.id.img_preview); txtInstruccion = findViewById(R.id.txt_instruccion)
        cardVerificacion = findViewById(R.id.card_verificacion); cardConfirmacion = findViewById(R.id.card_confirmacion)
        btnConfirmar = findViewById(R.id.btn_confirmar); btnReintentar = findViewById(R.id.btn_reintentar)
        iconCheck1 = findViewById(R.id.icon_check_1); iconCheck2 = findViewById(R.id.icon_check_2)
        iconCheck3 = findViewById(R.id.icon_check_3); iconCheck4 = findViewById(R.id.icon_check_4)
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        biometricValidator.cleanup()
    }
}