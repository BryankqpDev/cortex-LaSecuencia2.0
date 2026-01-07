package com.example.Cortex_LaSecuencia

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import com.google.firebase.database.FirebaseDatabase

class AdminActivity : AppCompatActivity() {

    private val guardarExcelLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { escribirDatosEnExcel(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val tabla = findViewById<TableLayout>(R.id.table_registros)
        val btnVolver = findViewById<Button>(R.id.btn_volver_inicio)
        val btnBorrar = findViewById<Button>(R.id.btn_borrar_todo)
        val btnExcel = findViewById<Button>(R.id.btn_descargar_excel)

        // 🔥 CARGAR DESDE FIREBASE
        cargarDesdeFirebase(tabla)

        btnVolver.setOnClickListener { finish() }

        btnBorrar.setOnClickListener {
            if (CortexManager.historialGlobal.isEmpty()) return@setOnClickListener
            CortexManager.historialGlobal.clear()
            tabla.removeViews(1, tabla.childCount - 1)
            Toast.makeText(this, "Base de datos borrada", Toast.LENGTH_SHORT).show()
        }

        btnExcel.setOnClickListener {
            if (CortexManager.historialGlobal.isEmpty()) {
                Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            } else {
                val nombreArchivo = "Reporte_Cortex_${System.currentTimeMillis()}.xlsx"
                guardarExcelLauncher.launch(nombreArchivo)
            }
        }
    }

    // 🔥 NUEVA FUNCIÓN: Cargar desde Firebase
    private fun cargarDesdeFirebase(tabla: TableLayout) {
        val ref = FirebaseDatabase.getInstance("https://cortex-lasecuencia-default-rtdb.firebaseio.com/")
            .getReference("registros")

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

                CortexManager.historialGlobal.add(
                    RegistroData(fecha, hora, supervisor, nombre, dni, equipo, nota, estado)
                )
            }

            llenarTabla(tabla)
            Toast.makeText(this, "✅ ${CortexManager.historialGlobal.size} registros cargados", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escribirDatosEnExcel(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Historial Operadores")

                val headerRow = sheet.createRow(0)
                val headerStyle = workbook.createCellStyle()
                val font = workbook.createFont()
                font.bold = true
                headerStyle.setFont(font)

                val columnas = listOf("FECHA", "HORA", "SUPERVISOR", "NOMBRE", "DNI", "EQUIPO", "NOTA", "ESTADO")
                for ((index, titulo) in columnas.withIndex()) {
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(titulo)
                    cell.cellStyle = headerStyle
                }

                val lista = CortexManager.historialGlobal
                for ((index, dato) in lista.withIndex()) {
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(dato.fecha)
                    row.createCell(1).setCellValue(dato.hora)
                    row.createCell(2).setCellValue(dato.supervisor)
                    row.createCell(3).setCellValue(dato.nombre)
                    row.createCell(4).setCellValue(dato.dni)
                    row.createCell(5).setCellValue(dato.equipo)
                    row.createCell(6).setCellValue("${dato.nota}%")
                    row.createCell(7).setCellValue(dato.estado)
                }

                workbook.write(outputStream)
                workbook.close()

                Toast.makeText(this, "✅ Excel guardado con éxito", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun llenarTabla(tabla: TableLayout) {
        val lista = CortexManager.historialGlobal.reversed()
        for (dato in lista) {
            val row = TableRow(this)
            row.setPadding(10, 20, 10, 20)
            row.setBackgroundColor(Color.parseColor("#0F172A"))

            fun agregarCelda(texto: String, color: Int = Color.WHITE) {
                val tv = TextView(this)
                tv.text = texto
                tv.setTextColor(color)
                tv.textSize = 12f
                tv.setPadding(0, 0, 40, 0)
                row.addView(tv)
            }

            agregarCelda(dato.fecha)
            agregarCelda(dato.hora)
            agregarCelda(dato.supervisor)
            agregarCelda(dato.nombre)
            agregarCelda(dato.dni)
            agregarCelda(dato.equipo)

            val colorNota = if (dato.nota >= 75) Color.GREEN else Color.RED
            agregarCelda("${dato.nota}%", colorNota)

            val colorEstado = if (dato.estado == "APTO") Color.GREEN else Color.RED
            agregarCelda(dato.estado, colorEstado)

            tabla.addView(row)
        }
    }
}