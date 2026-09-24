package com.coloniavictoria.municipal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReclamoActivity : AppCompatActivity() {

    private var ubicacion: Location? = null
    private var fotoUri: Uri? = null

    companion object {
        private const val LOCATION_REQUEST = 100
        private const val CAMERA_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reclamo)

        val spinner = findViewById<Spinner>(R.id.spinnerTipo)
        val descripcion = findViewById<EditText>(R.id.etDescripcion)
        val btnFoto = findViewById<Button>(R.id.btnFoto)
        val btnUbicacion = findViewById<Button>(R.id.btnUbicacion)
        val btnEnviar = findViewById<Button>(R.id.btnEnviar)

        val tipos = arrayOf(
            "Alumbrado público",
            "Calles y caminos",
            "Residuos",
            "Poda / espacios verdes",
            "Agua",
            "Limpieza",
            "Otros"
        )

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )

        btnUbicacion.setOnClickListener {
            obtenerUbicacion()
        }

        btnFoto.setOnClickListener {
            abrirCamara()
        }

        btnEnviar.setOnClickListener {
            if (descripcion.text.toString().trim().isEmpty()) {
                descripcion.error = "Describí el problema"
                return@setOnClickListener
            }

            val numero = "CV-" + System.currentTimeMillis().toString().takeLast(8)

            val fecha = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(Date())

            val reclamo = Reclamo(
                numero = numero,
                tipo = spinner.selectedItem.toString(),
                descripcion = descripcion.text.toString().trim(),
                fecha = fecha,
                latitud = ubicacion?.latitude,
                longitud = ubicacion?.longitude,
                fotoUri = fotoUri?.toString()
            )

            ReclamoStorage.guardar(this, reclamo)

            Toast.makeText(
                this,
                "Reclamo $numero registrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun obtenerUbicacion() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST
            )
            return
        }

        val manager = getSystemService(LOCATION_SERVICE) as LocationManager

        val gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        ubicacion = gps ?: network

        if (ubicacion != null) {
            Toast.makeText(
                this,
                "Ubicación agregada",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "No se pudo obtener la ubicación. Activá el GPS.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirCamara() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST
            )
            return
        }

        val archivo = File.createTempFile(
            "reclamo_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )

        fotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            archivo
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST)
        } else {
            Toast.makeText(
                this,
                "No se encontró una cámara",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == CAMERA_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamara()
        }

        if (
            requestCode == LOCATION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacion()
        }
    }

    @Deprecated("Deprecated Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK && fotoUri != null) {
                Toast.makeText(
                    this,
                    "Foto agregada",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                fotoUri = null
            }
        }
    }
}
