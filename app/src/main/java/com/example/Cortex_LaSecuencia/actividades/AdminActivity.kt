package com.example.Cortex_LaSecuencia.actividades

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.Cortex_LaSecuencia.CortexManager
import com.example.Cortex_LaSecuencia.R
import com.example.Cortex_LaSecuencia.RegistroData
import com.example.Cortex_LaSecuencia.SolicitudDesbloqueo
import com.example.Cortex_LaSecuencia.MainActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class AdminActivity : AppCompatActivity() {

    // Launcher para guardar Excel (INTACTO)
    private val guardarExcelLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri?.let { escribirDatosEnExcel(it) }
    }

    private var modoPapelera = false

    // Variables para la nueva sección de Solicitudes
    private lateinit var tableSolicitudes: TableLayout
    private lateinit var lblSolicitudes: TextView
    private lateinit var scrollSolicitudes: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Referencias UI existentes
        val tablaRegistros = findViewById<TableLayout>(R.id.table_registros)
        val btnMenu = findViewById<View>(R.id.btn_menu)
        val menuDrawer = findViewById<LinearLayout>(R.id.menu_drawer)
        val btnCerrarMenu = findViewById<View>(R.id.btn_cerrar_menu)

        // Referencias UI NUEVAS (Para la tabla de solicitudes)
        tableSolicitudes = findViewById(R.id.table_solicitudes)
        lblSolicitudes = findViewById(R.id.lbl_solicitudes)
        scrollSolicitudes = findViewById(R.id.scroll_solicitudes)

        // Menús (INTACTO)
        val menuVolver = findViewById<LinearLayout>(R.id.menu_volver)
        val menuExcel = findViewById<LinearLayout>(R.id.menu_descargar_excel)
        val menuEnviar = findViewById<LinearLayout>(R.id.menu_enviar_reporte)
        val menuConfigEmail = findViewById<LinearLayout>(R.id.menu_config_email)
        val menuPapelera = findViewById<LinearLayout>(R.id.menu_papelera)

        btnMenu.setOnClickListener { menuDrawer.visibility = View.VISIBLE }
        btnCerrarMenu.setOnClickListener { menuDrawer.visibility = View.GONE }

        // Cargar datos
        cargarDesdeFirebase(tablaRegistros)
        escucharSolicitudesEnTiempoReal() // <--- ESTO ES LO NUEVO (Reemplaza a escucharAlertasDesbloqueo)

        // Lógica de Botones del Menú (INTACTO)
        menuVolver.setOnClickListener {
            if (modoPapelera) {
                modoPapelera = false
                cargarDesdeFirebase(tablaRegistros)
                menuDrawer.visibility = View.GONE
            } else {
                startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
                finish()
            }
        }

        menuPapelera.setOnClickListener {
            modoPapelera = true
            cargarDesdeFirebase(tablaRegistros, "papelera")
            menuDrawer.visibility = View.GONE
        }

        menuExcel.setOnClickListener {
            if (CortexManager.historialGlobal.isEmpty()) Toast.makeText(this, "No hay datos", Toast.LENGTH_SHORT).show()
            else guardarExcelLauncher.launch("Reporte_Cortex_${System.currentTimeMillis()}.xlsx")
        }

        menuEnviar.setOnClickListener { prepararYEnviarEmail() }
        menuConfigEmail.setOnClickListener { mostrarDialogoConfigEmail() }
    }

    // ===================================================================================
    // 🚨 NUEVA LÓGICA: GESTIÓN DE SOLICITUDES EN TABLA (Reemplaza los Diálogos)
    // ===================================================================================

    private fun escucharSolicitudesEnTiempoReal() {
        FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listaPendientes = mutableListOf<SolicitudDesbloqueo>()
                    for (child in snapshot.children) {
                        val sol = child.getValue(SolicitudDesbloqueo::class.java)
                        // Solo traemos las que están PENDIENTES
                        if (sol != null && sol.estado == "pendiente") {
                            listaPendientes.add(sol)
                        }
                    }
                    actualizarTablaSolicitudes(listaPendientes)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, "Error al leer solicitudes", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun actualizarTablaSolicitudes(lista: List<SolicitudDesbloqueo>) {
        // 1. Limpiar la tabla (dejando solo la cabecera, índice 0)
        if (tableSolicitudes.childCount > 1) {
            tableSolicitudes.removeViews(1, tableSolicitudes.childCount - 1)
        }

        // 2. Controlar visibilidad: Si no hay solicitudes, ocultamos la sección naranja
        if (lista.isEmpty()) {
            lblSolicitudes.visibility = View.GONE
            scrollSolicitudes.visibility = View.GONE
            return
        }

        // Si hay datos, mostramos la sección
        lblSolicitudes.visibility = View.VISIBLE
        scrollSolicitudes.visibility = View.VISIBLE

        // 3. Rellenar filas
        for (sol in lista) {
            val row = TableRow(this).apply {
                setPadding(10, 20, 10, 20)
                setBackgroundColor(Color.parseColor("#451A03")) // Fondo oscuro rojizo/marrón
            }

            fun addText(text: String) {
                row.addView(TextView(this).apply {
                    this.text = text
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    setPadding(0, 0, 30, 0)
                })
            }

            addText(sol.dni)
            addText(sol.nombre)
            addText(sol.motivo)
            addText(sol.fecha)

            // --- BOTONES DE ACCIÓN ---
            val layoutBotones = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            // Botón APROBAR (✅)
            val btnAprobar = Button(this).apply {
                text = "✅"
                setBackgroundColor(Color.parseColor("#10B981")) // Verde
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(140, 110).apply { marginEnd = 10 }
                setOnClickListener { gestionarSolicitud(sol.dni, "autorizado") }
            }

            // Botón RECHAZAR (❌)
            val btnRechazar = Button(this).apply {
                text = "❌"
                setBackgroundColor(Color.parseColor("#EF4444")) // Rojo
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(140, 110)
                setOnClickListener { gestionarSolicitud(sol.dni, "rechazado") }
            }

            layoutBotones.addView(btnAprobar)
            layoutBotones.addView(btnRechazar)
            row.addView(layoutBotones)

            tableSolicitudes.addView(row)
        }
    }

    private fun gestionarSolicitud(dni: String, nuevoEstado: String) {
        // Actualizamos Firebase. El usuario en LockedActivity detectará el cambio automáticamente.
        FirebaseDatabase.getInstance().getReference("solicitudes_desbloqueo")
            .child(dni)
            .child("estado")
            .setValue(nuevoEstado)
            .addOnSuccessListener {
                val mensaje = if (nuevoEstado == "autorizado") "Usuario desbloqueado" else "Solicitud rechazada"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
    }

    // ===================================================================================
    // 📜 LÓGICA DE HISTORIAL, EXCEL Y EMAIL (INTACTA - NO SE HA TOCADO)
    // ===================================================================================

    private fun cargarDesdeFirebase(tabla: TableLayout, ruta: String = "registros") {
        FirebaseDatabase.getInstance().getReference(ruta).get().addOnSuccessListener { snapshot ->
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
        }.addOnFailureListener { Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun llenarTabla(tabla: TableLayout) {
        val lista = CortexManager.historialGlobal.reversed()
        // Limpiamos la tabla de registros (dejando la cabecera)
        if (tabla.childCount > 1) {
            tabla.removeViews(1, tabla.childCount - 1)
        }

        for (dato in lista) {
            val row = TableRow(this).apply { setPadding(10, 20, 10, 20); setBackgroundColor(if (modoPapelera) Color.parseColor("#1E1B4B") else Color.parseColor("#0F172A")) }
            fun addCell(t: String, c: Int = Color.WHITE) {
                row.addView(TextView(this).apply { text = t; setTextColor(c); textSize = 12f; setPadding(0, 0, 40, 0) })
            }
            addCell(dato.fecha); addCell(dato.hora); addCell(dato.supervisor); addCell(dato.nombre); addCell(dato.dni); addCell(dato.equipo)
            addCell("${dato.nota}%", if (dato.nota >= 75) Color.GREEN else Color.RED)
            addCell(dato.estado, if (dato.estado == "APTO") Color.GREEN else Color.RED)
            val btn = Button(this).apply {
                text = if (modoPapelera) "♻️" else "🗑️"
                setBackgroundColor(if (modoPapelera) Color.parseColor("#10B981") else Color.parseColor("#DC2626"))
                setOnClickListener { if (modoPapelera) restaurarRegistro(dato) else mostrarDialogoEliminar(dato) }
            }
            row.addView(btn); tabla.addView(row)
        }
    }

    private fun mostrarDialogoEliminar(dato: RegistroData) {
        AlertDialog.Builder(this).setTitle("🗑️ MOVER A PAPELERA").setMessage("¿Deseas mover el registro de ${dato.nombre}?").setPositiveButton("MOVER") { _, _ -> eliminarRegistro(dato) }.setNegativeButton("CANCELAR", null).show()
    }

    private fun eliminarRegistro(dato: RegistroData) {
        val ref = FirebaseDatabase.getInstance().getReference("registros")
        val papeleraRef = FirebaseDatabase.getInstance().getReference("papelera")
        ref.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                if (child.child("dni").getValue(String::class.java) == dato.dni && child.child("hora").getValue(String::class.java) == dato.hora) {
                    papeleraRef.push().setValue(dato).addOnSuccessListener { child.ref.removeValue().addOnSuccessListener { cargarDesdeFirebase(findViewById(R.id.table_registros)) } }
                    break
                }
            }
        }
    }

    private fun restaurarRegistro(dato: RegistroData) {
        val ref = FirebaseDatabase.getInstance().getReference("registros")
        val papeleraRef = FirebaseDatabase.getInstance().getReference("papelera")
        papeleraRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                if (child.child("dni").getValue(String::class.java) == dato.dni && child.child("hora").getValue(String::class.java) == dato.hora) {
                    ref.push().setValue(dato).addOnSuccessListener { child.ref.removeValue().addOnSuccessListener { cargarDesdeFirebase(findViewById(R.id.table_registros), "papelera") } }
                    break
                }
            }
        }
    }

    private fun mostrarDialogoConfigEmail() {
        val prefs = getSharedPreferences("CortexAdmin", Context.MODE_PRIVATE)
        val emailActual = prefs.getString("email_reportes", "")
        val input = EditText(this).apply { hint = "correo@ejemplo.com"; setText(emailActual); setPadding(50, 40, 50, 40) }
        AlertDialog.Builder(this).setTitle("CONFIGURAR DESTINATARIO").setMessage("Ingrese el correo:").setView(input).setPositiveButton("GUARDAR") { _, _ ->
            val nuevoEmail = input.text.toString().trim()
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches()) { prefs.edit().putString("email_reportes", nuevoEmail).apply(); Toast.makeText(this, "✅ Guardado", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("CANCELAR", null).show()
    }

    private fun prepararYEnviarEmail() {
        val destinatario = getSharedPreferences("CortexAdmin", Context.MODE_PRIVATE).getString("email_reportes", "")
        if (destinatario.isNullOrEmpty()) { mostrarDialogoConfigEmail(); return }
        try {
            val tempFile = File(externalCacheDir, "Reporte_Cortex_Temp.xlsx")
            val outputStream = FileOutputStream(tempFile)
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Historial")
            val header = sheet.createRow(0)
            listOf("FECHA", "HORA", "SUPERVISOR", "NOMBRE", "DNI", "EQUIPO", "NOTA", "ESTADO").forEachIndexed { i, t -> header.createCell(i).setCellValue(t) }
            CortexManager.historialGlobal.forEachIndexed { i, d -> val r = sheet.createRow(i + 1); r.createCell(0).setCellValue(d.fecha); r.createCell(1).setCellValue(d.hora); r.createCell(2).setCellValue(d.supervisor); r.createCell(3).setCellValue(d.nombre); r.createCell(4).setCellValue(d.dni); r.createCell(5).setCellValue(d.equipo); r.createCell(6).setCellValue("${d.nota}%"); r.createCell(7).setCellValue(d.estado) }
            workbook.write(outputStream); workbook.close(); outputStream.close()
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", tempFile)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; putExtra(Intent.EXTRA_EMAIL, arrayOf(destinatario)); putExtra(Intent.EXTRA_SUBJECT, "REPORTE CORTEX"); putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Enviar reporte..."))
        } catch (e: Exception) { }
    }

    private fun escribirDatosEnExcel(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { os ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Historial")
                val header = sheet.createRow(0)
                listOf("FECHA", "HORA", "SUPERVISOR", "NOMBRE", "DNI", "EQUIPO", "NOTA", "ESTADO").forEachIndexed { i, t -> header.createCell(i).setCellValue(t) }
                CortexManager.historialGlobal.forEachIndexed { i, d -> val r = sheet.createRow(i + 1); r.createCell(0).setCellValue(d.fecha); r.createCell(1).setCellValue(d.hora); r.createCell(2).setCellValue(d.supervisor); r.createCell(3).setCellValue(d.nombre); r.createCell(4).setCellValue(d.dni); r.createCell(5).setCellValue(d.equipo); r.createCell(6).setCellValue("${d.nota}%"); r.createCell(7).setCellValue(d.estado) }
                workbook.write(os); workbook.close()
                Toast.makeText(this, "✅ Excel guardado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { }
    }
}