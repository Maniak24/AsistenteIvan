package com.coloniavictoria.municipal

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

data class CatalogProduct(
    val id: Long,
    var name: String,
    var category: String,
    var brand: String,
    var price: String,
    var stock: Int,
    var description: String,
    var promotion: String
)

class CatalogActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var search: EditText
    private lateinit var countText: TextView
    private lateinit var stockText: TextView
    private val products = mutableListOf<CatalogProduct>()

    private val prefs by lazy { getSharedPreferences("mi_pc_catalog", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        container = findViewById(R.id.products_container)
        search = findViewById(R.id.catalog_search)
        countText = findViewById(R.id.catalog_count)
        stockText = findViewById(R.id.catalog_stock)

        loadProducts()
        refreshProducts()

        findViewById<View>(R.id.btn_add_product).setOnClickListener { showProductEditor(null) }
        findViewById<View>(R.id.btn_import_catalog)?.setOnClickListener { showImportOptions() }
        findViewById<View>(R.id.btn_sort_catalog)?.setOnClickListener { showSortOptions() }

        search.setOnKeyListener { _, _, _ ->
            refreshProducts()
            false
        }
    }

    private fun loadProducts() {
        products.clear()
        try {
            val array = JSONArray(prefs.getString("products", "[]") ?: "[]")
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                products.add(CatalogProduct(
                    o.optLong("id", System.currentTimeMillis() + i),
                    o.optString("name"), o.optString("category"), o.optString("brand"),
                    o.optString("price"), o.optInt("stock"), o.optString("description"),
                    o.optString("promotion")
                ))
            }
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo cargar el catálogo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveProducts() {
        val array = JSONArray()
        products.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("category", p.category)
                put("brand", p.brand); put("price", p.price); put("stock", p.stock)
                put("description", p.description); put("promotion", p.promotion)
            })
        }
        prefs.edit().putString("products", array.toString()).apply()
    }

    private fun refreshProducts() {
        container.removeAllViews()
        val q = search.text.toString().trim().lowercase()
        val filtered = products.filter { p ->
            q.isEmpty() || listOf(p.name, p.category, p.brand, p.price, p.description, p.promotion)
                .any { it.lowercase().contains(q) }
        }
        countText.text = if (filtered.size == 1) "1 producto" else "${filtered.size} productos"
        stockText.text = "Stock: ${products.sumOf { it.stock }}"

        if (filtered.isEmpty()) {
            container.addView(TextView(this).apply {
                text = if (products.isEmpty()) "Todavía no hay productos.\n\nTocá + para agregar el primero." else "No encontramos productos con esa búsqueda."
                setTextColor(0xff9696a3.toInt()); textSize = 15f; gravity = Gravity.CENTER
                setPadding(24, 80, 24, 80)
            })
            return
        }
        filtered.forEach { container.addView(createProductCard(it)) }
    }

    private fun createProductCard(p: CatalogProduct): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_product_card)
            setPadding(16, 14, 16, 14)
            setOnClickListener { showProductActions(p) }
        }
        card.addView(TextView(this).apply { text = p.name; setTextColor(-1); textSize = 18f; setTypeface(null, 1) })
        if (p.brand.isNotBlank() || p.category.isNotBlank()) card.addView(TextView(this).apply {
            text = listOf(p.brand, p.category).filter { it.isNotBlank() }.joinToString(" • ")
            setTextColor(0xff9696a3.toInt()); textSize = 13f; setPadding(0, 4, 0, 0)
        })
        card.addView(TextView(this).apply {
            text = if (p.price.isBlank()) "Precio no cargado" else "$ ${p.price}"
            setTextColor(0xff9a7bff.toInt()); textSize = 18f; setTypeface(null, 1); setPadding(0, 12, 0, 0)
        })
        card.addView(TextView(this).apply {
            text = when { p.stock <= 0 -> "Sin stock"; p.stock == 1 -> "Última unidad"; else -> "Stock: ${p.stock}" }
            setTextColor(if (p.stock > 0) 0xff69d296.toInt() else 0xffeb6969.toInt())
            textSize = 13f; setTypeface(null, 1); setPadding(0, 4, 0, 0)
        })
        if (p.description.isNotBlank()) card.addView(TextView(this).apply {
            text = p.description; setTextColor(0xffbebec8.toInt()); textSize = 14f; setPadding(0, 10, 0, 0)
        })
        if (p.promotion.isNotBlank()) card.addView(TextView(this).apply {
            text = "Promoción: ${p.promotion}"; setTextColor(0xffffbe50.toInt()); textSize = 13f; setPadding(0, 8, 0, 0)
        })
        card.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 12) }
        return card
    }

    private fun showProductActions(p: CatalogProduct) {
        AlertDialog.Builder(this).setTitle(p.name)
            .setItems(arrayOf("Ver ficha completa", "Editar producto", "Eliminar producto")) { _, which ->
                when (which) { 0 -> showProductDetails(p); 1 -> showProductEditor(p); 2 -> confirmDelete(p) }
            }.show()
    }

    private fun showProductDetails(p: CatalogProduct) {
        val text = buildString {
            append(if (p.brand.isBlank()) "" else "Marca: ${p.brand}\n")
            append(if (p.category.isBlank()) "" else "Categoría: ${p.category}\n")
            append("Precio: ${if (p.price.isBlank()) "No cargado" else "$ ${p.price}"}\n")
            append("Stock: ${p.stock}\n")
            if (p.description.isNotBlank()) append("\n${p.description}\n")
            if (p.promotion.isNotBlank()) append("\nPromoción: ${p.promotion}")
        }
        AlertDialog.Builder(this).setTitle(p.name).setMessage(text).setPositiveButton("Cerrar", null).show()
    }

    private fun showSortOptions() {
        AlertDialog.Builder(this).setTitle("Ordenar catálogo")
            .setItems(arrayOf("Orden de carga", "Nombre A-Z", "Nombre Z-A", "Precio menor", "Precio mayor", "Stock mayor")) { _, which ->
                when (which) {
                    0 -> Unit
                    1 -> products.sortBy { it.name.lowercase() }
                    2 -> products.sortByDescending { it.name.lowercase() }
                    3 -> products.sortBy { priceNumber(it.price) }
                    4 -> products.sortByDescending { priceNumber(it.price) }
                    5 -> products.sortByDescending { it.stock }
                }
                refreshProducts()
            }.show()
    }

    private fun priceNumber(value: String): Double {
        val clean = value.replace("$", "").replace(" ", "").replace(".", "").replace(",", ".")
        return clean.toDoubleOrNull() ?: Double.MAX_VALUE
    }

    private fun showImportOptions() {
        AlertDialog.Builder(this).setTitle("Carga masiva")
            .setItems(arrayOf("Importar CSV", "Pegar una lista", "Ver formato")) { _, which ->
                when (which) { 0 -> openCsv(); 1 -> showPasteDialog(); 2 -> showFormat() }
            }.show()
    }

    private fun openCsv() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "text/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
        }, 7001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7001 && resultCode == RESULT_OK) data?.data?.let { importCsv(it) }
    }

    private fun importCsv(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val added = importRows(parseDelimited(text))
            afterImport(added)
        } catch (e: Exception) { Toast.makeText(this, "No se pudo leer el archivo.", Toast.LENGTH_LONG).show() }
    }

    private fun parseDelimited(text: String): List<List<String>> {
        return text.replace("\r", "").split("\n").filter { it.trim().isNotEmpty() }.map { line ->
            val delimiter = when { line.count { it == ';' } >= 2 -> ';'; line.count { it == '\t' } >= 2 -> '\t'; else -> ',' }
            splitCsvLine(line, delimiter)
        }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') { if (quoted && i + 1 < line.length && line[i + 1] == '"') { current.append('"'); i++ } else quoted = !quoted }
            else if (c == delimiter && !quoted) { out.add(current.toString().trim()); current.clear() }
            else current.append(c)
            i++
        }
        out.add(current.toString().trim()); return out
    }

    private fun importRows(rows: List<List<String>>): Int {
        var added = 0
        rows.forEachIndexed { index, row ->
            if (index == 0 && row.any { it.lowercase().contains("producto") || it.lowercase().contains("nombre") }) return@forEachIndexed
            if (row.isEmpty() || row[0].isBlank()) return@forEachIndexed
            products.add(CatalogProduct(System.currentTimeMillis() + index, row.getOrElse(0) { "" }, row.getOrElse(1) { "" }, row.getOrElse(2) { "" }, row.getOrElse(3) { "" }, row.getOrElse(4) { "0" }.toIntOrNull() ?: 0, row.getOrElse(5) { "" }, row.getOrElse(6) { "" }))
            added++
        }
        if (added > 0) saveProducts()
        return added
    }

    private fun showPasteDialog() {
        val input = EditText(this).apply { hint = "Producto;Categoría;Marca;Precio;Stock;Descripción;Promoción"; setTextColor(-1); setHintTextColor(0xff777783.toInt()); minLines = 8; gravity = Gravity.TOP }
        AlertDialog.Builder(this).setTitle("Pegar lista").setView(input).setNegativeButton("Cancelar", null).setPositiveButton("Importar") { _, _ ->
            val added = importRows(parseDelimited(input.text.toString())); afterImport(added)
        }.show()
    }

    private fun showFormat() {
        AlertDialog.Builder(this).setTitle("Formato de importación")
            .setMessage("Una fila por producto:\n\nProducto;Categoría;Marca;Precio;Stock;Descripción;Promoción\n\nEjemplo:\nCable USB-C;Cables;Samsung;8500;12;Cable de carga rápida;2x1")
            .setPositiveButton("Cerrar", null).show()
    }

    private fun afterImport(added: Int) {
        loadProducts(); refreshProducts()
        Toast.makeText(this, if (added > 0) "Se importaron $added productos." else "No encontré productos válidos.", Toast.LENGTH_LONG).show()
    }

    private fun showProductEditor(existing: CatalogProduct?) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 8, 28, 0) }
        val name = createInput("Nombre del producto", existing?.name); val category = createInput("Categoría", existing?.category)
        val brand = createInput("Marca", existing?.brand); val price = createInput("Precio", existing?.price)
        val stock = createInput("Stock", existing?.stock?.toString()); val description = createInput("Descripción", existing?.description)
        val promotion = createInput("Promoción", existing?.promotion)
        listOf(name, category, brand, price, stock, description, promotion).forEach { form.addView(it) }
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "Nuevo producto" else "Editar producto").setView(form).setNegativeButton("Cancelar", null).setPositiveButton(if (existing == null) "Agregar" else "Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim(); if (n.isBlank()) { name.error = "Ingresá el nombre"; return@setOnClickListener }
                val s = stock.text.toString().trim().toIntOrNull() ?: 0; if (s < 0) { stock.error = "Stock inválido"; return@setOnClickListener }
                if (existing == null) products.add(CatalogProduct(System.currentTimeMillis(), n, category.text.toString().trim(), brand.text.toString().trim(), price.text.toString().trim(), s, description.text.toString().trim(), promotion.text.toString().trim()))
                else { existing.name = n; existing.category = category.text.toString().trim(); existing.brand = brand.text.toString().trim(); existing.price = price.text.toString().trim(); existing.stock = s; existing.description = description.text.toString().trim(); existing.promotion = promotion.text.toString().trim() }
                saveProducts(); refreshProducts(); dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun createInput(hint: String, value: String?): EditText = EditText(this).apply {
        this.hint = hint; setText(value ?: ""); setTextColor(-1); setHintTextColor(0xff73737d.toInt()); textSize = 15f
        setPadding(14, 10, 14, 10); background = getDrawable(R.drawable.bg_catalog_input)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 10) }
    }

    private fun confirmDelete(p: CatalogProduct) {
        AlertDialog.Builder(this).setTitle("Eliminar producto").setMessage("¿Querés eliminar \"${p.name}\"?")
            .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar") { _, _ -> products.removeAll { it.id == p.id }; saveProducts(); refreshProducts() }.show()
    }
}
