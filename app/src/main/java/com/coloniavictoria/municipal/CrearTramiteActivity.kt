package com.coloniavictoria.municipal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrearTramiteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_tramite)

        val nombre = findViewById<EditText>(R.id.etNombreTramite)
        val descripcion = findViewById<EditText>(R.id.etDescripcionTramite)
        val requisitos = findViewById<EditText>(R.id.etRequisitosTramite)
        val horario = findViewById<EditText>(R.id.etHorarioTramite)
        val contacto = findViewById<EditText>(R.id.etContactoTramite)

        findViewById<Button>(R.id.btnGuardarTramite).setOnClickListener {

            val nombreTexto = nombre.text.toString().trim()
            val descripcionTexto = descripcion.text.toString().trim()
            val requisitosTexto = requisitos.text.toString().trim()
            val horarioTexto = horario.text.toString().trim()
            val contactoTexto = contacto.text.toString().trim()

            if (nombreTexto.isEmpty()) {
                nombre.error = "Ingresá el nombre"
                return@setOnClickListener
            }

            if (descripcionTexto.isEmpty()) {
                descripcion.error = "Ingresá una descripción"
                return@setOnClickListener
            }

            TramiteStorage.guardar(
                this,
                Tramite(
                    nombre = nombreTexto,
                    descripcion = descripcionTexto,
                    requisitos = requisitosTexto.ifEmpty { "Consultar en la municipalidad" },
                    horario = horarioTexto.ifEmpty { "Consultar" },
                    contacto = contactoTexto.ifEmpty { "Consultar" }
                )
            )

            Toast.makeText(
                this,
                "Trámite publicado",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
