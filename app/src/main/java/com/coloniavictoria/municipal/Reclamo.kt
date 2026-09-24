package com.coloniavictoria.municipal

data class Reclamo(
    val numero: String,
    val tipo: String,
    val descripcion: String,
    val fecha: String,
    val estado: String = "Pendiente",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val fotoUri: String? = null
)
