package com.example.Cortex_LaSecuencia.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.Operador
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PDFGenerator {
    fun generarPDF(context: Context, operador: Operador, resultados: Map<String, Int>): File {
        val fecha = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Cortex_${operador.dni}_$fecha.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // Encabezado
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
        headerTable.setWidth(UnitValue.createPercentValue(100f))
        headerTable.setBackgroundColor(DeviceRgb(11, 17, 33))
        headerTable.addCell(Paragraph("CORTEX REPORTE TÉCNICO")
            .setFontSize(22f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(10f))
        document.add(headerTable)

        // Información del operador
        document.add(Paragraph("FECHA: ${operador.fecha} ${operador.hora}").setMarginTop(15f))
        document.add(Paragraph("EMPRESA: ${operador.empresa}"))
        document.add(Paragraph("SUPERVISOR: ${operador.supervisor}"))
        document.add(Paragraph("OPERADOR: ${operador.nombre}"))
        document.add(Paragraph("DNI: ${operador.dni}"))
        document.add(Paragraph("EQUIPO: ${operador.equipo}"))

        // Foto del operador (si existe)
        operador.fotoPerfil?.let { fotoBase64 ->
            try {
                val fotoBytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.size)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val imageData = ImageDataFactory.create(stream.toByteArray())
                val image = Image(imageData)
                image.setWidth(80f)
                image.setHeight(80f)
                image.setHorizontalAlignment(HorizontalAlignment.RIGHT)
                document.add(image)
            } catch (e: Exception) {
                // Ignorar si no se puede agregar la imagen
            }
        }

        // Resultados agrupados (como en HTML)
        val grupos = listOf(
            Triple("I. ATENCIÓN Y PROCESAMIENTO", listOf("t2", "t5", "t6", "t10"), 
                listOf("Memoria", "Atención (Stroop)", "Escaneo", "Flexibilidad")),
            Triple("II. PERCEPCIÓN ESPACIAL", listOf("t3", "t8", "t9"),
                listOf("Anticipación (TTC)", "Rastreo (MOT)", "Orientación")),
            Triple("III. INTEGRACIÓN SENSORIOMOTORA", listOf("t1", "t4", "t7"),
                listOf("Reflejos (PVT)", "Coordinación", "Impulso"))
        )

        grupos.forEach { (nombreGrupo, keys, labels) ->
            document.add(Paragraph(nombreGrupo).setBold().setMarginTop(15f))
            keys.forEachIndexed { idx, key ->
                val valor = resultados[key] ?: 0
                document.add(Paragraph("- ${labels[idx]}: $valor%"))
            }
            document.add(Paragraph(""))
        }

        // Estado final
        val promedio = resultados.values.sum() / resultados.size.coerceAtLeast(1)
        val esApto = promedio >= 75
        val estadoTexto = if (esApto) "APTO ($promedio%)" else "NO APTO ($promedio%)"
        val estadoColor = if (esApto) ColorConstants.GREEN else ColorConstants.RED

        document.add(Paragraph("ESTADO FINAL: $estadoTexto")
            .setBold()
            .setFontSize(16f)
            .setFontColor(estadoColor)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20f))

        // Créditos
        document.add(Paragraph("CREADOR DEL PROYECTO: ING. BRAYAM OMAR VILLENA CUBA")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30f))
        document.add(Paragraph("SOPORTE: CELL 933665602")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER))

        document.close()
        return file
    }
}

