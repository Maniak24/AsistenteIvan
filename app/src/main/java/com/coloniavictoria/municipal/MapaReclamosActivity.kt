package com.coloniavictoria.municipal

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MapaReclamosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_reclamos)

        val lista = findViewById<LinearLayout>(R.id.listaMapa)

        val reclamos = ReclamoStorage.obtener(this)
            .filter { it.latitud != null && it.longitud != null }
            .reversed()

        if (reclamos.isEmpty()) {
            lista.addView(TextView(this).apply {
                text = "Todavía no hay reclamos con ubicación."
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, 40, 0, 0)
            })
            return
        }

        reclamos.forEach { reclamo ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.WHITE)
            }

            tarjeta.addView(TextView(this).apply {
                text = "${reclamo.numero}\n${reclamo.tipo}"
                textSize = 19f
                setTextColor(Color.rgb(27, 94, 32))
                setTypeface(null, android.graphics.Typeface.BOLD)
            })

            tarjeta.addView(TextView(this).apply {
                text = "\n${reclamo.descripcion}\n\nEstado: ${reclamo.estado}"
                textSize = 16f
                setTextColor(Color.DKGRAY)
            })

            val boton = Button(this).apply {
                text = "VER UBICACIÓN"
                setOnClickListener {
                    val uri = Uri.parse(
                        "geo:${reclamo.latitud},${reclamo.longitud}?q=${reclamo.latitud},${reclamo.longitud}"
                    )

                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }

            tarjeta.addView(boton)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 12, 0, 0)
            lista.addView(tarjeta, params)
        }
    }
}
