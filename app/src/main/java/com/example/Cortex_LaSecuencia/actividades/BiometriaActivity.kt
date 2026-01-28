package com.example.Cortex_LaSecuencia.actividades
import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.Cortex_LaSecuencia.actividades.WelcomeActivity
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Context
import com.example.Cortex_LaSecuencia.utils.FaceEmbeddingProcessor
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.CortexManager

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
    private var fotoBitmap: Bitmap? = null

    // ✅ NUEVO: Flag para saber si ya está autenticado
    private var estaAutenticado = false

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometria)
        initViews()
        cameraExecutor = Executors.newSingleThreadExecutor()

        // ✅ PRIMERO: Autenticar anónimamente
        autenticarUsuario()
    }

    // ✅ NUEVA FUNCIÓN: Autenticación anónima
    private fun autenticarUsuario() {
        txtInstruccion.text = "Preparando cámara..."

        CortexManager.autenticarConductorAnonimo(
            onSuccess = { user ->
                estaAutenticado = true
                // Ahora sí iniciamos la cámara
                if (allPermissionsGranted()) {
                    startCamera()
                    txtInstruccion.text = "Asegúrate de que tu rostro sea visible"
                } else {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error de autenticación: $error", Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }

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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                // ✅ Configurar listeners DESPUÉS de iniciar la cámara
                setupListeners()
            } catch (exc: Exception) {
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturarFoto() {
        // ✅ Verificar autenticación antes de capturar
        if (!estaAutenticado) {
            Toast.makeText(this, "Esperando autenticación...", Toast.LENGTH_SHORT).show()
            return
        }

        val imageCapture = imageCapture ?: return
        txtInstruccion.text = "Capturando..."
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                fotoBitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()
                iniciarVerificacion()
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@BiometriaActivity, "Error al capturar foto", Toast.LENGTH_SHORT).show()
                txtInstruccion.text = "Asegúrate de que tu rostro sea visible"
            }
        })
    }

    private fun iniciarVerificacion() {
        imgPreview.setImageBitmap(fotoBitmap)
        imgPreview.visibility = View.VISIBLE
        previewView.visibility = View.GONE
        btnCapturar.visibility = View.GONE
        cardVerificacion.visibility = View.VISIBLE
        cardVerificacion.alpha = 0f
        cardVerificacion.animate().alpha(1f).setDuration(300).start()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ animateCheckIcon(iconCheck1) }, 500)
        handler.postDelayed({ animateCheckIcon(iconCheck2) }, 1500)
        handler.postDelayed({ animateCheckIcon(iconCheck3) }, 2500)
        handler.postDelayed({ progressCheck4.visibility = View.VISIBLE; iconCheck4.visibility = View.GONE }, 3500)
        handler.postDelayed({
            progressCheck4.visibility = View.GONE
            iconCheck4.visibility = View.VISIBLE
            iconCheck4.setImageResource(R.drawable.ic_check_circle)
            iconCheck4.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light))
            animateCheckIcon(iconCheck4)
            mostrarConfirmacion()
        }, 4500)
    }

    private fun mostrarConfirmacion() {
        cardVerificacion.animate().alpha(0f).setDuration(300).withEndAction {
            cardVerificacion.visibility = View.GONE
            cardConfirmacion.visibility = View.VISIBLE
            cardConfirmacion.alpha = 0f
            cardConfirmacion.animate().alpha(1f).setDuration(300).start()
        }.start()
    }

    private fun confirmarYGuardar() {
        val operadorActual = CortexManager.operadorActual
        if (operadorActual == null) {
            Toast.makeText(this, "Error: Operador no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fotoBitmap?.let { bitmap ->
            btnConfirmar.isEnabled = false
            btnReintentar.isEnabled = false
            txtInstruccion.text = "Procesando biometría..."

            // ✅ OPCIÓN 1: Guardar como hash (String de 64 caracteres)
            FaceEmbeddingProcessor.generarHashFacial(
                bitmap,
                onSuccess = { hashFacial ->
                    // Guardar foto localmente
                    guardarFotoLocalmente(bitmap, operadorActual.dni)

                    // Guardar hash en Firebase
                    CortexManager.guardarHashFacial(
                        hashFacial,
                        operadorActual.dni,
                        onSuccess = {
                            Toast.makeText(this, "✓ Verificación completada", Toast.LENGTH_SHORT).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this, WelcomeActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }, 500)
                        },
                        onError = { error ->
                            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                            btnConfirmar.isEnabled = true
                            btnReintentar.isEnabled = true
                            txtInstruccion.text = "Error al guardar"
                        }
                    )
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    btnConfirmar.isEnabled = true
                    btnReintentar.isEnabled = true
                    txtInstruccion.text = "No se detectó rostro correctamente"
                }
            )

            /* ✅ OPCIÓN 2: Guardar como ID numérico (Long)
            FaceEmbeddingProcessor.generarIdNumerico(
                bitmap,
                onSuccess = { idFacial ->
                    guardarFotoLocalmente(bitmap, operadorActual.dni)
                    CortexManager.guardarIdFacialNumerico(
                        idFacial,
                        operadorActual.dni,
                        onSuccess = { /* mismo código de éxito */ },
                        onError = { /* mismo código de error */ }
                    )
                },
                onError = { /* mismo código de error */ }
            )
            */
        }
    }

    // ✅ NUEVA FUNCIÓN: Guardar foto localmente en el dispositivo
    private fun guardarFotoLocalmente(bitmap: Bitmap, dni: String) {
        try {
            val fileName = "selfie_${dni}_${System.currentTimeMillis()}.jpg"
            val fos = openFileOutput(fileName, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.close()

            // Opcional: Guardar la ruta en SharedPreferences
            val prefs = getSharedPreferences("CortexPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("foto_${dni}", fileName).apply()

        } catch (e: Exception) {
            // No es crítico si falla, el hash ya se guardó
        }
    }

    private fun reintentar() {
        fotoBitmap = null
        cardConfirmacion.animate().alpha(0f).setDuration(300).withEndAction {
            cardConfirmacion.visibility = View.GONE
            imgPreview.visibility = View.GONE
            previewView.visibility = View.VISIBLE
            btnCapturar.visibility = View.VISIBLE
            txtInstruccion.text = "Asegúrate de que tu rostro sea visible"
            resetVerificationIcons()
        }.start()
    }

    private fun resetVerificationIcons() {
        iconCheck1.setImageResource(R.drawable.ic_circle_outline)
        iconCheck2.setImageResource(R.drawable.ic_circle_outline)
        iconCheck3.setImageResource(R.drawable.ic_circle_outline)
        iconCheck4.setImageResource(R.drawable.ic_circle_outline)
        iconCheck1.clearColorFilter(); iconCheck2.clearColorFilter()
        iconCheck3.clearColorFilter(); iconCheck4.clearColorFilter()
        progressCheck4.visibility = View.GONE
    }

    private fun animateCheckIcon(icon: ImageView) {
        icon.scaleX = 0f; icon.scaleY = 0f
        icon.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
    }

    private fun animateButton(button: View) {
        ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.9f, 1f).apply { duration = 200; start() }
        ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.9f, 1f).apply { duration = 200; start() }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                txtInstruccion.text = "Asegúrate de que tu rostro sea visible"
            } else {
                Toast.makeText(this, "Permisos de cámara denegados", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}