package com.ivan.asistente

import android.content.Context
import org.json.JSONArray
import java.util.Locale

data class CatalogContext(
    val text: String,
    val hasProducts: Boolean
)

class CatalogRepository(context: Context) {

    private val prefs =
        context.getSharedPreferences("mi_pc_catalog", Context.MODE_PRIVATE)

    fun buildContext(userQuery: String): CatalogContext {
        val json = prefs.getString("products", "[]") ?: "[]"
        val products = mutableListOf<CatalogProduct>()

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                products.add(
                    CatalogProduct(
                        id = item.optLong("id"),
                        name = item.optString("name"),
                        category = item.optString("category"),
                        brand = item.optString("brand"),
                        price = item.optString("price"),
                        stock = item.optInt("stock"),
                        description = item.optString("description"),
                        promotion = item.optString("promotion")
                    )
                )
            }
        } catch (_: Exception) {
            return CatalogContext(
                text = "El catálogo no pudo ser leído. No inventes información de productos.",
                hasProducts = false
            )
        }

        if (products.isEmpty()) {
            return CatalogContext(
                text = """
                    CATÁLOGO DE MI PC:
                    No hay productos cargados actualmente.

                    Regla:
                    Si el cliente pregunta por un producto, precio, stock,
                    promoción o disponibilidad, indicá que no está cargado
                    en el catálogo de Mi PC. Nunca inventes esos datos.
                """.trimIndent(),
                hasProducts = false
            )
        }

        val query = normalize(userQuery)

        val matched = products.filter { product ->
            val searchable = normalize(
                listOf(
                    product.name,
                    product.category,
                    product.brand,
                    product.description,
                    product.promotion
                ).joinToString(" ")
            )

            val words = query
                .split(" ")
                .filter { it.length >= 3 }

            words.any { word -> searchable.contains(word) }
        }

        val queryLooksLikeCatalogQuestion =
            listOf(
                "precio",
                "cuanto",
                "tenes",
                "tienen",
                "stock",
                "queda",
                "quedan",
                "disponible",
                "disponibles",
                "producto",
                "productos",
                "celular",
                "celulares",
                "telefono",
                "telefonos",
                "notebook",
                "notebooks",
                "computadora",
                "computadoras",
                "cargador",
                "cargadores",
                "accesorio",
                "accesorios",
                "oferta",
                "promocion",
                "venta",
                "venden",
                "comprar"
            ).any { query.contains(it) }

        val selected =
            if (matched.isNotEmpty()) {
                matched
            } else if (queryLooksLikeCatalogQuestion) {
                products
            } else {
                emptyList()
            }

        if (selected.isEmpty()) {
            return CatalogContext(
                text = """
                    CATÁLOGO DE MI PC:
                    No se encontró un producto relacionado con esta consulta.

                    Regla:
                    No inventes productos, precios, stock, promociones ni
                    características. Si corresponde, indicá que no hay
                    información de ese producto cargada en el catálogo.
                """.trimIndent(),
                hasProducts = true
            )
        }

        val builder = StringBuilder()

        builder.appendLine("CATÁLOGO REAL Y ACTUAL DE MI PC:")
        builder.appendLine()
        builder.appendLine(
            "Usá únicamente estos datos para responder consultas sobre productos."
        )
        builder.appendLine(
            "No modifiques, completes ni inventes precios, stock o promociones."
        )
        builder.appendLine()

        selected.forEachIndexed { index, product ->
            builder.appendLine("PRODUCTO ${index + 1}:")
            builder.appendLine("Nombre: ${product.name}")
            builder.appendLine("Categoría: ${product.category}")
            builder.appendLine("Marca/modelo: ${product.brand}")
            builder.appendLine("Precio: ${product.price.ifBlank { "No informado" }}")
            builder.appendLine("Stock: ${product.stock}")
            builder.appendLine(
                "Descripción: ${
                    product.description.ifBlank { "No informada" }
                }"
            )
            builder.appendLine(
                "Promoción: ${
                    product.promotion.ifBlank { "Ninguna informada" }
                }"
            )
            builder.appendLine()
        }

        builder.appendLine("REGLAS DEL CATÁLOGO:")
        builder.appendLine("- El stock indicado arriba es el stock registrado.")
        builder.appendLine("- No afirmes disponibilidad si el stock es 0.")
        builder.appendLine("- No inventes precios ni promociones.")
        builder.appendLine("- Si falta un dato, decí que no está informado.")
        builder.appendLine("- Si el cliente pregunta por algo que no aparece, decí que no está cargado.")

        return CatalogContext(
            text = builder.toString().trim(),
            hasProducts = true
        )
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace(Regex("[^a-z0-9ñ ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
