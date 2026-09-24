package com.coloniavictoria.municipal

import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PanelMunicipalActivity : AppCompatActivity() {

    private val estados = arrayOf(
        "Pendiente",
        "En revisión",
        "En proceso",
        "Resuelto"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarReclamos()
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing.not()) {
            mostrarReclamos()
        }
    }

    private fun mostrarReclamos() {
        setContentView(R.layout.activity_panel_municipal)

        
        
        findViewById<android.widget.Button>(R.id.btnGestionTramites).setOnClickListener {
            startActivity(android.content.Intent(this, CrearTramiteActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnGestionObras).setOnClickListener {
            startActivity(android.content.Intent(this, CrearObraActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnGestionNoticias).setOnClickListener {
            startActivity(android.content.Intent(this, CrearNoticiaActivity::class.java))
        }

        val resumen = findViewById<TextView>(R.id.txtResumen)
        val lista = findViewById<LinearLayout>(R.id.listaMunicipal)

        val reclamos = ReclamoStorage.obtener(this)

        val pendientes = reclamos.count { it.estado == "Pendiente" }
        val revision = reclamos.count { it.estado == "En revisión" }
        val proceso = reclamos.count { it.estado == "En proceso" }
        val resueltos = reclamos.count { it.estado == "Resuelto" }

        resumen.text = """
            Reclamos registrados: ${reclamos.size}

            Pendientes: $pendientes
            En revisión: $revision
            En proceso: $proceso
            Resueltos: $resueltos
        """.trimIndent()

        reclamos.reversed().forEach { reclamo ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundColor(Color.WHITE)
            }

            val info = TextView(this).apply {
                text = """
                    ${reclamo.numero}
                    ${reclamo.tipo}

                    ${reclamo.descripcion}

                    Fecha: ${reclamo.fecha}
                """.trimIndent()

                textSize = 16f
                setTextColor(Color.DKGRAY)
            }

            val spinner = Spinner(this)

            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                estados
            )

            val posicion = estados.indexOf(reclamo.estado)
            if (posicion >= 0) {
                spinner.setSelection(posicion)
            }

            spinner.setOnItemSelectedListener(
                object : android.widget.AdapterView.OnItemSelectedListener {

                    private var primeraCarga = true

                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: android.view.View?,
                        position: Int,
                        id: Long
                    ) {
                        if (primeraCarga) {
                            primeraCarga = false
                            return
                        }

                        val nuevoEstado = estados[position]

                        ReclamoStorage.actualizarEstado(
                            this@PanelMunicipalActivity,
                            reclamo.numero,
                            nuevoEstado
                        )

                        NotificacionHelper.mostrar(
                            this@PanelMunicipalActivity,
                            reclamo.numero,
                            nuevoEstado
                        )
                    }

                    override fun onNothingSelected(
                        parent: android.widget.AdapterView<*>?
                    ) {}
                }
            )

            tarjeta.addView(info)

            reclamo.fotoUri?.let { uriString ->
                try {
                    val imagen = ImageView(this).apply {
                        setImageURI(Uri.parse(uriString))
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                    val imagenParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )

                    imagenParams.topMargin = 16
                    tarjeta.addView(imagen, imagenParams)
                } catch (_: Exception) {
                }
            }

            tarjeta.addView(spinner)

            val botonEliminar = android.widget.Button(this).apply {
                text = "ELIMINAR RECLAMO"
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@PanelMunicipalActivity)
                        .setTitle("Eliminar reclamo")
                        .setMessage("¿Querés eliminar ${reclamo.numero}?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ ->
                            ReclamoStorage.eliminar(
                                this@PanelMunicipalActivity,
                                reclamo.numero
                            )
                            mostrarReclamos()
                        }
                        .show()
                }
            }

            tarjeta.addView(botonEliminar)

            if (reclamo.latitud != null && reclamo.longitud != null) {
                val botonUbicacion = android.widget.Button(this).apply {
                    text = "VER UBICACIÓN"
                    setOnClickListener {
                        val uri = Uri.parse(
                            "geo:${reclamo.latitud},${reclamo.longitud}?q=${reclamo.latitud},${reclamo.longitud}"
                        )

                        val intent = Intent(Intent.ACTION_VIEW, uri)

                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        }
                    }
                }

                tarjeta.addView(botonUbicacion)
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
