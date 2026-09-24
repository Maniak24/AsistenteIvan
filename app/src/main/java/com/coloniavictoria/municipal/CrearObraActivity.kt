package com.coloniavictoria.municipal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrearObraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_obra)

        val nombre = findViewById<EditText>(R.id.etNombreObra)
        val ubicacion = findViewById<EditText>(R.id.etUbicacionObra)
        val descripcion = findViewById<EditText>(R.id.etDescripcionObra)
        val estado = findViewById<EditText>(R.id.etEstadoObra)
        val avance = findViewById<EditText>(R.id.etAvanceObra)

        findViewById<Button>(R.id.btnGuardarObra).setOnClickListener {

            val nombreTexto = nombre.text.toString().trim()
            val ubicacionTexto = ubicacion.text.toString().trim()
            val descripcionTexto = descripcion.text.toString().trim()
            val estadoTexto = estado.text.toString().trim()
            val avanceTexto = avance.text.toString().trim()

            if (nombreTexto.isEmpty()) {
                nombre.error = "Ingresá el nombre"
                return@setOnClickListener
            }

            if (ubicacionTexto.isEmpty()) {
                ubicacion.error = "Ingresá la ubicación"
                return@setOnClickListener
            }

            val porcentaje = avanceTexto.toIntOrNull()

            if (porcentaje == null || porcentaje !in 0..100) {
                avance.error = "Ingresá un porcentaje entre 0 y 100"
                return@setOnClickListener
            }

            val fecha = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date())

            ObraStorage.guardar(
                this,
                Obra(
                    nombre = nombreTexto,
                    ubicacion = ubicacionTexto,
                    descripcion = descripcionTexto,
                    estado = estadoTexto.ifEmpty { "Planificada" },
                    avance = porcentaje,
                    fecha = fecha
                )
            )

            Toast.makeText(
                this,
                "Obra publicada",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
