package com.example.Cortex_LaSecuencia

import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val btnExcel = findViewById<Button>(R.id.btn_descargar_excel)
        val btnVolver = findViewById<Button>(R.id.btn_volver_inicio)
        val btnBorrar = findViewById<Button>(R.id.btn_borrar_todo)
        val tabla = findViewById<TableLayout>(R.id.table_registros)

        // 1. Botón VOLVER AL INICIO
        btnVolver.setOnClickListener {
            finish() // Cierra esta pantalla y vuelve al Login
        }

        // 2. Botón DESCARGAR EXCEL
        btnExcel.setOnClickListener {
            // Aquí iría la lógica de Apache POI (Próxima tarea)
            Toast.makeText(this, "Generando Excel... (Pendiente de librería)", Toast.LENGTH_SHORT).show()
        }

        // 3. Botón BORRAR TODO (Con seguridad)
        btnBorrar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("⚠️ ¿ESTÁS SEGURO?")
                .setMessage("Esto borrará permanentemente todo el historial de evaluaciones.")
                .setPositiveButton("SÍ, BORRAR") { _, _ ->
                    // Aquí borraremos la base de datos local
                    Toast.makeText(this, "Base de datos reseteada", Toast.LENGTH_SHORT).show()
                    tabla.removeViews(1, tabla.childCount - 1) // Limpia la tabla visualmente (deja la cabecera)
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }

        // Aquí llamaremos a una función cargarDatos() para llenar la tabla
    }
}