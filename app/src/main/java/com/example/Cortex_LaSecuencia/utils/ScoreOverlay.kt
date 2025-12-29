package com.example.Cortex_LaSecuencia.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.Cortex_LaSecuencia.R

object ScoreOverlay {
    fun mostrar(
        context: Context,
        score: Int,
        mensaje: String,
        onContinuar: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_score_overlay)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val txtScore = dialog.findViewById<TextView>(R.id.overlay_score)
        val txtMensaje = dialog.findViewById<TextView>(R.id.overlay_msg)
        val btnContinuar = dialog.findViewById<Button>(R.id.btn_next_action)

        txtScore.text = "$score%"
        txtMensaje.text = mensaje

        // Color según score
        when {
            score >= 95 -> txtScore.setTextColor(Color.parseColor("#10B981")) // Verde
            score >= 75 -> txtScore.setTextColor(Color.parseColor("#F59E0B")) // Amarillo
            else -> txtScore.setTextColor(Color.parseColor("#EF4444")) // Rojo
        }

        btnContinuar.setOnClickListener {
            dialog.dismiss()
            onContinuar()
        }

        dialog.show()
    }
}

