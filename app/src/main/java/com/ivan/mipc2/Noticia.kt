package com.ivan.mipc2

data class Noticia(
    val titulo: String,
    val contenido: String,
    val fecha: String,
    val urgente: Boolean = false
)
