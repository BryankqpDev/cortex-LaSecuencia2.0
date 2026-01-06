package com.example.Cortex_LaSecuencia.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PDFGenerator {

    /**
     * Generar PDF del reporte completo
     * @return File si se generó exitosamente, null si hubo error
     */
    fun generarPDF(
        context: Context,
        operador: Operador,
        resultados: Map<String, Int>,
        fotoBitmap: Bitmap? = null
    ): File? {
        try {
            val fecha = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Cortex_${operador.dni}_$fecha.pdf"

            // === CREAR ARCHIVO SEGÚN VERSIÓN DE ANDROID ===
            val file: File
            val outputStream: OutputStream

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw Exception("No se pudo crear URI")

                outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw Exception("No se pudo abrir OutputStream")

                // Para retornar el archivo (aunque en Android 10+ se usa URI)
                file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

            } else {
                // Android 9 y anteriores
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            // === CREAR DOCUMENTO PDF ===
            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            // === 1. ENCABEZADO AZUL ===
            agregarEncabezado(document)

            // === 2. INFORMACIÓN DEL OPERADOR ===
            agregarDatosOperador(document, operador, fotoBitmap)

            // === 3. RESULTADOS AGRUPADOS ===
            agregarResultados(document, resultados)

            // === 4. ESTADO FINAL ===
            agregarEstadoFinal(document, resultados)

            // === 5. FOOTER CON CRÉDITOS ===
            agregarFooter(document)

            // === CERRAR DOCUMENTO ===
            document.close()
            outputStream.close()

            // Notificar al usuario
            Toast.makeText(
                context,
                "PDF generado: $fileName\nGuardado en Descargas",
                Toast.LENGTH_LONG
            ).show()

            return file

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al generar PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    /**
     * Agregar encabezado azul oscuro
     */
    private fun agregarEncabezado(document: Document) {
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
        headerTable.setWidth(UnitValue.createPercentValue(100f))
        headerTable.setBackgroundColor(DeviceRgb(11, 17, 33))

        val headerCell = headerTable.addCell(
            Paragraph("CORTEX REPORTE TÉCNICO")
                .setFontSize(22f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.WHITE)
                .setBold()
                .setPaddingTop(15f)
                .setPaddingBottom(15f)
        )

        document.add(headerTable)
    }

    /**
     * Agregar datos del operador
     */
    private fun agregarDatosOperador(
        document: Document,
        operador: Operador,
        fotoBitmap: Bitmap?
    ) {
        // Fecha actual de generación
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        document.add(Paragraph("FECHA DE EVALUACIÓN: $fechaHoy")
            .setBold()
            .setMarginTop(15f))

        document.add(Paragraph(""))

        document.add(Paragraph("EMPRESA: ${operador.empresa}"))
        document.add(Paragraph("SUPERVISOR: ${operador.supervisor}"))
        document.add(Paragraph("OPERADOR: ${operador.nombre}").setBold())
        document.add(Paragraph("DNI: ${operador.dni}"))
        document.add(Paragraph("EQUIPO: ${operador.equipo}"))
        document.add(Paragraph("UNIDAD: ${operador.unidad}"))

        // === AGREGAR FOTO SI EXISTE ===
        // Primero intentar con el Bitmap pasado como parámetro
        val fotoParaUsar = fotoBitmap ?: operador.fotoPerfil?.let { fotoBase64 ->
            try {
                val fotoBytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
                BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        fotoParaUsar?.let { bitmap ->
            try {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val imageData = ImageDataFactory.create(stream.toByteArray())
                val image = Image(imageData)
                image.setWidth(80f)
                image.setHeight(80f)
                image.setHorizontalAlignment(HorizontalAlignment.RIGHT)
                image.setFixedPosition(450f, 680f) // Posición en esquina superior derecha
                document.add(image)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        document.add(Paragraph(""))
    }

    /**
     * Agregar resultados agrupados por categoría
     */
    private fun agregarResultados(document: Document, resultados: Map<String, Int>) {
        document.add(Paragraph("RESULTADOS DETALLADOS")
            .setBold()
            .setFontSize(14f)
            .setMarginTop(20f))

        document.add(Paragraph(""))

        // Grupos de tests (igual que en HTML)
        val grupos = listOf(
            Triple(
                "I. ATENCIÓN Y PROCESAMIENTO",
                listOf("t2", "t5", "t6", "t10"),
                listOf("Memoria", "Atención (Stroop)", "Escaneo", "Flexibilidad")
            ),
            Triple(
                "II. PERCEPCIÓN ESPACIAL",
                listOf("t3", "t8", "t9"),
                listOf("Anticipación (TTC)", "Rastreo (MOT)", "Orientación")
            ),
            Triple(
                "III. INTEGRACIÓN SENSORIOMOTORA",
                listOf("t1", "t4", "t7"),
                listOf("Reflejos (PVT)", "Coordinación", "Impulso")
            )
        )

        grupos.forEach { (nombreGrupo, keys, labels) ->
            // Título del grupo
            document.add(Paragraph(nombreGrupo)
                .setBold()
                .setFontSize(12f)
                .setMarginTop(15f))

            // Crear tabla para los resultados
            val table = Table(UnitValue.createPercentArray(floatArrayOf(80f, 20f)))
            table.setWidth(UnitValue.createPercentValue(100f))

            keys.forEachIndexed { idx, key ->
                val valor = resultados[key] ?: 0

                // Columna 1: Nombre del test
                table.addCell(Paragraph("    - ${labels[idx]}")
                    .setFontSize(10f))

                // Columna 2: Puntaje
                val colorPuntaje = when {
                    valor >= 90 -> DeviceRgb(16, 185, 129) // Verde
                    valor >= 75 -> DeviceRgb(245, 158, 11) // Naranja
                    else -> DeviceRgb(239, 68, 68) // Rojo
                }

                table.addCell(Paragraph("$valor%")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontColor(colorPuntaje)
                    .setBold())
            }

            document.add(table)
            document.add(Paragraph(""))
        }
    }

    /**
     * Agregar estado final (APTO/NO APTO)
     */
    private fun agregarEstadoFinal(document: Document, resultados: Map<String, Int>) {
        // Calcular promedio
        val promedio = if (resultados.isEmpty()) 0
        else resultados.values.sum() / resultados.size

        val esApto = promedio >= 75
        val estadoTexto = if (esApto) "APTO" else "NO APTO"
        val estadoColor = if (esApto) {
            DeviceRgb(16, 185, 129) // Verde
        } else {
            DeviceRgb(239, 68, 68) // Rojo
        }

        document.add(Paragraph("ESTADO FINAL: $estadoTexto ($promedio%)")
            .setBold()
            .setFontSize(16f)
            .setFontColor(estadoColor)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30f))

        // Mensaje adicional
        val mensaje = if (esApto) {
            "El operador ha demostrado capacidades cognitivas\nadecuadas para la operación segura de equipos."
        } else {
            "El operador requiere descanso o evaluación adicional\nantes de operar equipos pesados."
        }

        document.add(Paragraph(mensaje)
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
            .setItalic())
    }

    /**
     * Agregar footer con créditos
     */
    private fun agregarFooter(document: Document) {
        document.add(Paragraph("")
            .setMarginTop(40f))

        document.add(Paragraph("────────────────────────────────────────────")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY))

        document.add(Paragraph("CREADOR DEL PROYECTO: ING. BRAYAM OMAR VILLENA CUBA")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10f)
            .setFontColor(ColorConstants.GRAY))

        document.add(Paragraph("SOPORTE: CELL 933665602")
            .setFontSize(8f)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY))
    }
}