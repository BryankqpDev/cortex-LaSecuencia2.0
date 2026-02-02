package com.example.Cortex_LaSecuencia.actividades

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.RegistroData
import com.example.Cortex_LaSecuencia.MainActivity
import com.google.firebase.database.FirebaseDatabase
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AdminActivity : AppCompatActivity() {

    private val guardarExcelLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { escribirDatosEnExcel(it) }
    }

    private var modoPapelera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val tabla = findViewById<TableLayout>(R.id.table_registros)
        val btnMenu = findViewById<View>(R.id.btn_menu)
        val menuDrawer = findViewById<LinearLayout>(R.id.menu_drawer)
        val btnCerrarMenu = findViewById<View>(R.id.btn_cerrar_menu)

        val menuVolver = findViewById<LinearLayout>(R.id.menu_volver)
        val menuExcel = findViewById<LinearLayout>(R.id.menu_descargar_excel)
        val menuEnviar = findViewById<LinearLayout>(R.id.menu_enviar_reporte)
        val menuConfigEmail = findViewById<LinearLayout>(R.id.menu_config_email)
        val menuPapelera = findViewById<LinearLayout>(R.id.menu_papelera)

        btnMenu.setOnClickListener { menuDrawer.visibility = View.VISIBLE }
        btnCerrarMenu.setOnClickListener { menuDrawer.visibility = View.GONE }

        cargarDesdeFirebase(tabla)

        menuVolver.setOnClickListener {
            if (modoPapelera) {
                modoPapelera = false
                cargarDesdeFirebase(tabla)
                menuDrawer.visibility = View.GONE
            } else {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        menuPapelera.setOnClickListener {
            modoPapelera = true
            cargarDesdeFirebase(tabla, "papelera")
            menuDrawer.visibility = View.GONE
        }

        menuExcel.setOnClickListener {
            if (CortexManager.historialGlobal.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            } else {
                val nombreArchivo = "Reporte_Cortex_${System.currentTimeMillis()}.xlsx"
                guardarExcelLauncher.launch(nombreArchivo)
            }
        }

        menuEnviar.setOnClickListener { prepararYEnviarEmail() }
        menuConfigEmail.setOnClickListener { mostrarDialogoConfigEmail() }
    }

    // ✅ FUNCIÓN CORREGIDA - Ahora detecta errores de permisos
    private fun cargarDesdeFirebase(tabla: TableLayout, ruta: String = "registros") {
        val ref = FirebaseDatabase.getInstance().getReference(ruta)
        ref.get().addOnSuccessListener { snapshot ->
            CortexManager.historialGlobal.clear()
            for (child in snapshot.children) {
                val fecha = child.child("fecha").getValue(String::class.java) ?: ""
                val hora = child.child("hora").getValue(String::class.java) ?: ""
                val supervisor = child.child("supervisor").getValue(String::class.java) ?: ""
                val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                val dni = child.child("dni").getValue(String::class.java) ?: ""
                val equipo = child.child("equipo").getValue(String::class.java) ?: ""
                val nota = child.child("nota").getValue(Int::class.java) ?: 0
                val estado = child.child("estado").getValue(String::class.java) ?: ""
                CortexManager.historialGlobal.add(RegistroData(fecha, hora, supervisor, nombre, dni, equipo, nota, estado))
            }
            llenarTabla(tabla)
        }.addOnFailureListener { e ->
            // 🔥 DETECCIÓN ESPECÍFICA DE ERRORES DE PERMISOS
            val msg = if (e.message?.contains("Permission denied", ignoreCase = true) == true) {
                "❌ PERMISO DENEGADO en Firebase.\n\n" +
                        "Debes agregar la carpeta '$ruta' en las reglas de Firebase:\n" +
                        "Consola Firebase > Realtime Database > Rules"
            } else {
                "❌ Error al cargar: ${e.message}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun llenarTabla(tabla: TableLayout) {
        val lista = CortexManager.historialGlobal.reversed()
        tabla.removeViews(1, tabla.childCount - 1)

        for ((index, dato) in lista.withIndex()) {
            val row = TableRow(this).apply {
                setPadding(10, 20, 10, 20)
                setBackgroundColor(if (modoPapelera) Color.parseColor("#1E1B4B") else Color.parseColor("#0F172A"))
            }

            fun addCell(t: String, c: Int = Color.WHITE) {
                row.addView(TextView(this).apply { text = t; setTextColor(c); textSize = 12f; setPadding(0, 0, 40, 0) })
            }

            addCell(dato.fecha); addCell(dato.hora); addCell(dato.supervisor)
            addCell(dato.nombre); addCell(dato.dni); addCell(dato.equipo)
            addCell("${dato.nota}%", if (dato.nota >= 75) Color.GREEN else Color.RED)
            addCell(dato.estado, if (dato.estado == "APTO") Color.GREEN else Color.RED)

            val btnAccion = Button(this).apply {
                text = if (modoPapelera) "♻️" else "🗑️"
                textSize = 16f
                setBackgroundColor(if (modoPapelera) Color.parseColor("#10B981") else Color.parseColor("#DC2626"))
                setTextColor(Color.WHITE)
                setPadding(20, 10, 20, 10)
                setOnClickListener {
                    if (modoPapelera) restaurarRegistro(dato)
                    else mostrarDialogoEliminar(dato)
                }
            }
            row.addView(btnAccion)
            tabla.addView(row)
        }
    }

    private fun mostrarDialogoEliminar(dato: RegistroData) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ ENVIAR A PAPELERA")
            .setMessage("¿Deseas mover a la papelera el registro de:\n${dato.nombre}?")
            .setPositiveButton("MOVER") { _, _ -> eliminarRegistro(dato) }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    // ✅ FUNCIÓN COMPLETAMENTE REESCRITA - Ahora es 100% confiable
    private fun eliminarRegistro(dato: RegistroData) {
        val ref = FirebaseDatabase.getInstance().getReference("registros")
        val papeleraRef = FirebaseDatabase.getInstance().getReference("papelera")

        ref.get().addOnSuccessListener { snapshot ->
            var registroEncontrado: com.google.firebase.database.DataSnapshot? = null

            // 🔍 Paso 1: Buscar el registro exacto
            for (child in snapshot.children) {
                if (child.child("dni").getValue(String::class.java) == dato.dni &&
                    child.child("hora").getValue(String::class.java) == dato.hora &&
                    child.child("fecha").getValue(String::class.java) == dato.fecha) {
                    registroEncontrado = child
                    break
                }
            }

            if (registroEncontrado == null) {
                Toast.makeText(this, "❌ No se encontró el registro en Firebase", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // 💾 Paso 2: Guardar en papelera PRIMERO
            papeleraRef.push().setValue(dato)
                .addOnSuccessListener {
                    // ✅ Paso 3: SOLO si se guardó exitosamente, eliminar del original
                    registroEncontrado.ref.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Movido a papelera exitosamente", Toast.LENGTH_SHORT).show()
                            // 🔄 Recargar la tabla automáticamente
                            cargarDesdeFirebase(findViewById(R.id.table_registros))
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "⚠️ Guardado en papelera pero no se pudo eliminar del original: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    // 🚨 Error crítico: no se pudo guardar en papelera
                    if (e.message?.contains("Permission denied", ignoreCase = true) == true) {
                        Toast.makeText(
                            this,
                            "❌ PERMISO DENEGADO\n\nDebes agregar 'papelera' en las reglas de Firebase.\n\nVe a: Consola Firebase > Realtime Database > Rules",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this, "❌ Error al guardar en papelera: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Error al acceder a Firebase: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ✅ FUNCIÓN COMPLETAMENTE REESCRITA - Ahora restaura correctamente
    private fun restaurarRegistro(dato: RegistroData) {
        val ref = FirebaseDatabase.getInstance().getReference("registros")
        val papeleraRef = FirebaseDatabase.getInstance().getReference("papelera")

        papeleraRef.get().addOnSuccessListener { snapshot ->
            var registroEncontrado: com.google.firebase.database.DataSnapshot? = null

            // 🔍 Paso 1: Buscar el registro en papelera
            for (child in snapshot.children) {
                if (child.child("dni").getValue(String::class.java) == dato.dni &&
                    child.child("hora").getValue(String::class.java) == dato.hora &&
                    child.child("fecha").getValue(String::class.java) == dato.fecha) {
                    registroEncontrado = child
                    break
                }
            }

            if (registroEncontrado == null) {
                Toast.makeText(this, "❌ No se encontró el registro en papelera", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // 💾 Paso 2: Restaurar a "registros" PRIMERO
            ref.push().setValue(dato)
                .addOnSuccessListener {
                    // ✅ Paso 3: SOLO si se restauró exitosamente, eliminar de papelera
                    registroEncontrado.ref.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "✅ Registro restaurado exitosamente", Toast.LENGTH_SHORT).show()
                            // 🔄 Recargar la tabla de papelera automáticamente
                            cargarDesdeFirebase(findViewById(R.id.table_registros), "papelera")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "⚠️ Restaurado pero no se pudo eliminar de papelera: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "❌ Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Error al acceder a papelera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoConfigEmail() {
        val prefs = getSharedPreferences("CortexAdmin", Context.MODE_PRIVATE)
        val emailActual = prefs.getString("email_reportes", "")
        val input = EditText(this).apply { hint = "correo@ejemplo.com"; setText(emailActual); setPadding(50, 40, 50, 40) }
        AlertDialog.Builder(this).setTitle("CONFIGURAR DESTINATARIO").setMessage("Ingrese el correo:").setView(input)
            .setPositiveButton("GUARDAR") { _, _ ->
                val nuevoEmail = input.text.toString().trim()
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) {
                    prefs.edit().putString("email_reportes", nuevoEmail).apply()
                    Toast.makeText(this, "✅ Guardado", Toast.LENGTH_SHORT).show()
                } else { Toast.makeText(this, "❌ Inválido", Toast.LENGTH_SHORT).show() }
            }.setNegativeButton("CANCELAR", null).show()
    }

    private fun prepararYEnviarEmail() {
        if (CortexManager.historialGlobal.isEmpty()) return
        val prefs = getSharedPreferences("CortexAdmin", Context.MODE_PRIVATE)
        val destinatario = prefs.getString("email_reportes", "")
        if (destinatario.isNullOrEmpty()) { mostrarDialogoConfigEmail(); return }
        try {
            val tempFile = File(externalCacheDir, "Reporte_Cortex_Temp.xlsx")
            val outputStream = FileOutputStream(tempFile)
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Historial")
            val header = sheet.createRow(0)
            val columnas = listOf("FECHA", "HORA", "SUPERVISOR", "NOMBRE", "DNI", "EQUIPO", "NOTA", "ESTADO")
            columnas.forEachIndexed { i, t -> header.createCell(i).setCellValue(t) }
            CortexManager.historialGlobal.forEachIndexed { i, dato ->
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(dato.fecha); row.createCell(1).setCellValue(dato.hora)
                row.createCell(2).setCellValue(dato.supervisor); row.createCell(3).setCellValue(dato.nombre)
                row.createCell(4).setCellValue(dato.dni); row.createCell(5).setCellValue(dato.equipo)
                row.createCell(6).setCellValue("${dato.nota}%"); row.createCell(7).setCellValue(dato.estado)
            }
            workbook.write(outputStream); workbook.close(); outputStream.close()
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", tempFile)
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(destinatario))
                putExtra(Intent.EXTRA_SUBJECT, "REPORTE CORTEX")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(emailIntent, "Enviar reporte..."))
        } catch (e: Exception) { }
    }

    private fun escribirDatosEnExcel(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Historial")
                val header = sheet.createRow(0)
                val columnas = listOf("FECHA", "HORA", "SUPERVISOR", "NOMBRE", "DNI", "EQUIPO", "NOTA", "ESTADO")
                columnas.forEachIndexed { i, t -> header.createCell(i).setCellValue(t) }
                CortexManager.historialGlobal.forEachIndexed { i, dato ->
                    val row = sheet.createRow(i + 1)
                    row.createCell(0).setCellValue(dato.fecha); row.createCell(1).setCellValue(dato.hora)
                    row.createCell(2).setCellValue(dato.supervisor); row.createCell(3).setCellValue(dato.nombre)
                    row.createCell(4).setCellValue(dato.dni); row.createCell(5).setCellValue(dato.equipo)
                    row.createCell(6).setCellValue("${dato.nota}%"); row.createCell(7).setCellValue(dato.estado)
                }
                workbook.write(outputStream); workbook.close()
                Toast.makeText(this, "✅ Excel guardado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { }
    }
}