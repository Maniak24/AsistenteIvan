package com.coloniavictoria.municipal

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalleReclamoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reclamo)

        val numero = intent.getStringExtra("numero") ?: return
        val reclamo = ReclamoStorage.obtener(this)
            .firstOrNull { it.numero == numero } ?: return

        findViewById<TextView>(R.id.txtNumeroDetalle).text = reclamo.numero
        findViewById<TextView>(R.id.txtTipoDetalle).text = reclamo.tipo
        findViewById<TextView>(R.id.txtDescripcionDetalle).text = reclamo.descripcion
        findViewById<TextView>(R.id.txtFechaDetalle).text = "Fecha: ${reclamo.fecha}"
        findViewById<TextView>(R.id.txtEstadoDetalle).text =
            "Estado: ${reclamo.estado}"

        val imagen = findViewById<ImageView>(R.id.imagenDetalle)

        reclamo.fotoUri?.let {
            try {
                imagen.setImageURI(Uri.parse(it))
                imagen.visibility = ImageView.VISIBLE
            } catch (_: Exception) {
                imagen.visibility = ImageView.GONE
            }
        } ?: run {
            imagen.visibility = ImageView.GONE
        }

        val botonUbicacion = findViewById<Button>(R.id.btnUbicacionDetalle)

        if (reclamo.latitud != null && reclamo.longitud != null) {
            botonUbicacion.visibility = Button.VISIBLE

            botonUbicacion.setOnClickListener {
                val uri = Uri.parse(
                    "geo:${reclamo.latitud},${reclamo.longitud}?q=${reclamo.latitud},${reclamo.longitud}"
                )

                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    uri
                )

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        } else {
            botonUbicacion.visibility = Button.GONE
        }
    }
}
