package com.coloniavictoria.municipal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NoticiaStorage {

    private const val PREFS = "noticias"
    private const val KEY = "lista"

    fun guardar(context: Context, noticia: Noticia) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))

        lista.put(
            JSONObject().apply {
                put("titulo", noticia.titulo)
                put("contenido", noticia.contenido)
                put("fecha", noticia.fecha)
                put("urgente", noticia.urgente)
            }
        )

        prefs.edit().putString(KEY, lista.toString()).apply()
    }

    fun obtener(context: Context): List<Noticia> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))
        val resultado = mutableListOf<Noticia>()

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)

            resultado.add(
                Noticia(
                    titulo = obj.getString("titulo"),
                    contenido = obj.getString("contenido"),
                    fecha = obj.getString("fecha"),
                    urgente = obj.getBoolean("urgente")
                )
            )
        }

        return resultado.reversed()
    }
}
