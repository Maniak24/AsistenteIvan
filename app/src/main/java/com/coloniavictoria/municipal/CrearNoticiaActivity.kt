package com.coloniavictoria.municipal

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrearNoticiaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_noticia)

        val titulo = findViewById<EditText>(R.id.etTitulo)
        val contenido = findViewById<EditText>(R.id.etContenido)
        val urgente = findViewById<CheckBox>(R.id.checkUrgente)
        val guardar = findViewById<Button>(R.id.btnGuardarNoticia)

        guardar.setOnClickListener {

            val tituloTexto = titulo.text.toString().trim()
            val contenidoTexto = contenido.text.toString().trim()

            if (tituloTexto.isEmpty()) {
                titulo.error = "Ingresá un título"
                return@setOnClickListener
            }

            if (contenidoTexto.isEmpty()) {
                contenido.error = "Ingresá el contenido"
                return@setOnClickListener
            }

            val fecha = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date())

            NoticiaStorage.guardar(
                this,
                Noticia(
                    titulo = tituloTexto,
                    contenido = contenidoTexto,
                    fecha = fecha,
                    urgente = urgente.isChecked
                )
            )

            Toast.makeText(
                this,
                "Aviso publicado",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
