package com.coloniavictoria.municipal

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NoticiasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_noticias)

        val lista = findViewById<LinearLayout>(R.id.listaNoticias)
        val noticias = NoticiaStorage.obtener(this)

        if (noticias.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay avisos publicados."
                textSize = 16f
                setTextColor(Color.DKGRAY)
                setPadding(0, 40, 0, 0)
            }

            lista.addView(vacio)
            return
        }

        noticias.forEach { noticia ->

            val tarjeta = TextView(this).apply {
                text = buildString {
                    if (noticia.urgente) {
                        append("AVISO IMPORTANTE\n\n")
                    }

                    append(noticia.titulo)
                    append("\n\n")
                    append(noticia.contenido)
                    append("\n\n")
                    append(noticia.fecha)
                }

                textSize = 16f
                setTextColor(Color.DKGRAY)
                setBackgroundColor(Color.WHITE)
                setPadding(20, 20, 20, 20)
            }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 12, 0, 0)
            lista.addView(tarjeta, params)
        }
    }
}
