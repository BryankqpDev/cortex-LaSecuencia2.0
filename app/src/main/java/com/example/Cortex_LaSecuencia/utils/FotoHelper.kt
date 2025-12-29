package com.example.Cortex_LaSecuencia.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object FotoHelper {
    fun capturarDesdeImageView(imageView: ImageView): String? {
        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache()
        val bitmap = imageView.drawingCache ?: return null
        
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val bytes = stream.toByteArray()
        
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
    
    fun convertirImageProxyABase64(imageProxy: ImageProxy): String? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val compressedBytes = stream.toByteArray()
            
            Base64.encodeToString(compressedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}

