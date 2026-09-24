package com.coloniavictoria.municipal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReclamoStorage {

    private const val PREFS = "reclamos"
    private const val KEY = "lista"

    fun guardar(context: Context, reclamo: Reclamo) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))

        val obj = JSONObject().apply {
            put("numero", reclamo.numero)
            put("tipo", reclamo.tipo)
            put("descripcion", reclamo.descripcion)
            put("fecha", reclamo.fecha)
            put("estado", reclamo.estado)
            put("latitud", reclamo.latitud)
            put("longitud", reclamo.longitud)
            put("fotoUri", reclamo.fotoUri)
        }

        lista.put(obj)
        prefs.edit().putString(KEY, lista.toString()).apply()
    }

    fun obtener(context: Context): List<Reclamo> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return convertir(JSONArray(prefs.getString(KEY, "[]")))
    }

    fun actualizarEstado(
        context: Context,
        numero: String,
        nuevoEstado: String
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lista = JSONArray(prefs.getString(KEY, "[]"))

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)

            if (obj.getString("numero") == numero) {
                obj.put("estado", nuevoEstado)
                break
            }
        }

        prefs.edit().putString(KEY, lista.toString()).apply()
    }


    fun eliminar(context: android.content.Context, numero: String) {
        val reclamos = obtener(context).filter { it.numero != numero }

        val json = org.json.JSONArray()

        reclamos.forEach { reclamo ->
            val obj = org.json.JSONObject().apply {
                put("numero", reclamo.numero)
                put("tipo", reclamo.tipo)
                put("descripcion", reclamo.descripcion)
                put("fecha", reclamo.fecha)
                put("estado", reclamo.estado)
                put("latitud", reclamo.latitud)
                put("longitud", reclamo.longitud)
                put("fotoUri", reclamo.fotoUri)
            }
            json.put(obj)
        }

        context.getSharedPreferences("reclamos", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("lista", json.toString())
            .apply()
    }

    private fun convertir(lista: JSONArray): List<Reclamo> {
        val resultado = mutableListOf<Reclamo>()

        for (i in 0 until lista.length()) {
            val obj = lista.getJSONObject(i)

            resultado.add(
                Reclamo(
                    numero = obj.getString("numero"),
                    tipo = obj.getString("tipo"),
                    descripcion = obj.getString("descripcion"),
                    fecha = obj.getString("fecha"),
                    estado = obj.getString("estado"),
                    latitud = if (obj.isNull("latitud")) null else obj.getDouble("latitud"),
                    longitud = if (obj.isNull("longitud")) null else obj.getDouble("longitud"),
                    fotoUri = if (obj.isNull("fotoUri")) null else obj.getString("fotoUri")
                )
            )
        }

        return resultado
    }
}
