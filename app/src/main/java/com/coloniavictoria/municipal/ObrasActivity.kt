package com.coloniavictoria.municipal

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ObrasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_obras)

        val lista = findViewById<LinearLayout>(R.id.listaObras)
        val obras = ObraStorage.obtener(this)

        if (obras.isEmpty()) {
            lista.addView(TextView(this).apply {
                text = "No hay obras publicadas."
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, 40, 0, 0)
            })
            return
        }

        obras.forEach { obra ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.WHITE)
            }

            tarjeta.addView(TextView(this).apply {
                text = obra.nombre
                textSize = 20f
                setTextColor(Color.rgb(27, 94, 32))
                setTypeface(null, android.graphics.Typeface.BOLD)
            })

            tarjeta.addView(TextView(this).apply {
                text = "\nUbicación: ${obra.ubicacion}\n\n${obra.descripcion}\n\nEstado: ${obra.estado}\nAvance: ${obra.avance}%\nActualización: ${obra.fecha}"
                textSize = 16f
                setTextColor(Color.DKGRAY)
            })

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 12, 0, 0)
            lista.addView(tarjeta, params)
        }
    }
}
