package com.coloniavictoria.municipal

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MisReclamosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reclamos)
    }

    override fun onResume() {
        super.onResume()
        mostrarReclamos()
    }

    private fun mostrarReclamos() {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorReclamos)
        contenedor.removeAllViews()

        val reclamos = ReclamoStorage.obtener(this)

        if (reclamos.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "Todavía no tenés reclamos registrados."
                textSize = 17f
                setTextColor(Color.DKGRAY)
                setPadding(20, 20, 20, 20)
            }

            contenedor.addView(vacio)
            return
        }

        reclamos.reversed().forEach { reclamo ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.WHITE)
                isClickable = true
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@MisReclamosActivity,
                            DetalleReclamoActivity::class.java
                        ).apply {
                            putExtra("numero", reclamo.numero)
                        }
                    )
                }
            }

            val info = TextView(this).apply {
                text = """
                    ${reclamo.numero}
                    ${reclamo.tipo}

                    ${reclamo.descripcion}

                    Fecha: ${reclamo.fecha}
                    Estado: ${reclamo.estado}

                    Tocá para ver el detalle
                """.trimIndent()

                textSize = 16f
                setTextColor(Color.DKGRAY)
            }

            tarjeta.addView(info)

            reclamo.fotoUri?.let { uriString ->
                try {
                    val imagen = ImageView(this).apply {
                        setImageURI(Uri.parse(uriString))
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                    val parametros = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        350
                    )

                    parametros.topMargin = 16
                    tarjeta.addView(imagen, parametros)
                } catch (_: Exception) {
                }
            }

            val parametrosTarjeta = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            parametrosTarjeta.setMargins(0, 12, 0, 0)
            contenedor.addView(tarjeta, parametrosTarjeta)
        }
    }
}
