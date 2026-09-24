package com.coloniavictoria.municipal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ObraStorage {

    private const val PREFS = "obras"
    private const val KEY = "lista"

    fun guardar(context: Context, obra: Obra) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))

        lista.put(
            JSONObject().apply {
                put("nombre", obra.nombre)
                put("ubicacion", obra.ubicacion)
                put("descripcion", obra.descripcion)
                put("estado", obra.estado)
                put("avance", obra.avance)
                put("fecha", obra.fecha)
            }
        )

        prefs.edit().putString(KEY, lista.toString()).apply()
    }

    fun obtener(context: Context): List<Obra> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))
        val resultado = mutableListOf<Obra>()

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)

            resultado.add(
                Obra(
                    nombre = obj.getString("nombre"),
                    ubicacion = obj.getString("ubicacion"),
                    descripcion = obj.getString("descripcion"),
                    estado = obj.getString("estado"),
                    avance = obj.getInt("avance"),
                    fecha = obj.getString("fecha")
                )
            )
        }

        return resultado.reversed()
    }
}
