package com.coloniavictoria.municipal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TramiteStorage {

    private const val PREFS = "tramites"
    private const val KEY = "lista"

    fun guardar(context: Context, tramite: Tramite) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))

        lista.put(JSONObject().apply {
            put("nombre", tramite.nombre)
            put("descripcion", tramite.descripcion)
            put("requisitos", tramite.requisitos)
            put("horario", tramite.horario)
            put("contacto", tramite.contacto)
        })

        prefs.edit().putString(KEY, lista.toString()).apply()
    }

    fun obtener(context: Context): List<Tramite> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))
        val resultado = mutableListOf<Tramite>()

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)

            resultado.add(
                Tramite(
                    nombre = obj.getString("nombre"),
                    descripcion = obj.getString("descripcion"),
                    requisitos = obj.getString("requisitos"),
                    horario = obj.getString("horario"),
                    contacto = obj.getString("contacto")
                )
            )
        }

        return resultado
    }
}
