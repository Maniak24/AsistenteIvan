package com.ivan.asistente

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream

private data class CatalogProduct(
    val id: Long,
    var name: String,
    var category: String,
    var brand: String,
    var model: String,
    var price: String,
    var stock: Int,
    var description: String,
    var promotion: String,
    var imageUri: String = ""
)

class CatalogActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var search: EditText
    private lateinit var countText: TextView
    private lateinit var stockText: TextView
    private lateinit var categoryContainer: LinearLayout
    private val products = mutableListOf<CatalogProduct>()
    private var selectedCategory = "Todas"
    private var sortMode = 0
    private var pendingImageInput: EditText? = null
    private val prefs by lazy { getSharedPreferences("mi_pc_catalog", Context.MODE_PRIVATE) }

    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        val name = queryFileName(uri).lowercase(Locale.getDefault())
        if (name.endsWith(".xlsx")) importXlsx(uri) else importTextFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)
        container = findViewById(R.id.products_container)
        search = findViewById(R.id.catalog_search)
        countText = findViewById(R.id.catalog_count)
        stockText = findViewById(R.id.catalog_stock)
        categoryContainer = findViewById(R.id.category_container)
        loadProducts()
        rebuildCategories()
        refreshProducts()
        findViewById<View>(R.id.btn_add_product).setOnClickListener { showProductEditor(null) }
        findViewById<View>(R.id.btn_import_catalog).setOnClickListener { showImportOptions() }
        findViewById<View>(R.id.btn_sort_catalog).setOnClickListener { showSortOptions() }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshProducts() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
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
                    o.optString("model"), o.optString("price"), o.optInt("stock"),
                    o.optString("description"), o.optString("promotion"), o.optString("imageUri")
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
                put("id", p.id); put("name", p.name); put("category", p.category); put("brand", p.brand)
                put("model", p.model); put("price", p.price); put("stock", p.stock)
                put("description", p.description); put("promotion", p.promotion); put("imageUri", p.imageUri)
            })
        }
        prefs.edit().putString("products", array.toString()).apply()
    }

    private fun rebuildCategories() {
        categoryContainer.removeAllViews()
        val categories = listOf("Todas") + products.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        categories.forEach { category ->
            val chip = TextView(this).apply {
                text = category; textSize = 13f; gravity = Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD); setPadding(18, 0, 18, 0)
                setTextColor(if (category == selectedCategory) Color.WHITE else Color.rgb(170, 170, 182))
                background = getDrawable(if (category == selectedCategory) R.drawable.bg_catalog_add else R.drawable.bg_product_card)
                setOnClickListener { selectedCategory = category; rebuildCategories(); refreshProducts() }
            }
            categoryContainer.addView(chip, LinearLayout.LayoutParams(-2, 36).apply { setMargins(0, 0, 8, 0) })
        }
    }

    private fun refreshProducts() {
        container.removeAllViews()
        val query = search.text.toString().trim().lowercase(Locale.getDefault())
        var filtered = products.filter {
            (selectedCategory == "Todas" || it.category.equals(selectedCategory, true)) &&
                (query.isEmpty() || listOf(it.name, it.category, it.brand, it.model, it.description, it.promotion)
                    .any { value -> value.lowercase(Locale.getDefault()).contains(query) })
        }
        filtered = when (sortMode) {
            1 -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
            2 -> filtered.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            3 -> filtered.sortedBy { priceNumber(it.price) }
            4 -> filtered.sortedByDescending { priceNumber(it.price) }
            5 -> filtered.sortedByDescending { it.stock }
            else -> filtered
        }
        countText.text = if (filtered.size == 1) "1 producto" else "${filtered.size} productos"
        stockText.text = "Stock: ${products.sumOf { it.stock }}"
        if (filtered.isEmpty()) {
            container.addView(TextView(this).apply {
                text = if (products.isEmpty()) "Todavía no hay productos.\n\nUsá Importar para cargar un Excel/CSV o pegá una lista." else "No encontramos productos con esa búsqueda."
                setTextColor(Color.rgb(150, 150, 163)); textSize = 15f; gravity = Gravity.CENTER; setPadding(24, 80, 24, 80)
            })
            return
        }
        filtered.forEach { container.addView(createProductCard(it)) }
    }

    private fun priceNumber(value: String): Double = value.replace(".", "").replace(",", ".")
        .filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

    private fun createProductCard(product: CatalogProduct): View {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getDrawable(R.drawable.bg_product_card); setPadding(16, 14, 16, 14) }
        if (product.imageUri.isNotBlank()) {
            try {
                card.addView(ImageView(this).apply { setImageURI(Uri.parse(product.imageUri)); scaleType = ImageView.ScaleType.CENTER_CROP }, LinearLayout.LayoutParams(-1, 150).apply { bottomMargin = 10 })
            } catch (_: Exception) {}
        }
        card.addView(TextView(this).apply { text = product.name; setTextColor(Color.WHITE); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
        val secondary = listOf(product.brand, product.model, product.category).filter { it.isNotBlank() }.joinToString(" • ")
        if (secondary.isNotBlank()) card.addView(TextView(this).apply { text = secondary; setTextColor(Color.rgb(150, 150, 163)); textSize = 13f; setPadding(0, 4, 0, 0) })
        card.addView(TextView(this).apply { text = if (product.price.isBlank()) "Precio no cargado" else "$ ${product.price}"; setTextColor(Color.rgb(154, 123, 255)); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 12, 0, 0) })
        card.addView(TextView(this).apply { text = when { product.stock <= 0 -> "Sin stock"; product.stock == 1 -> "Última unidad"; else -> "Stock: ${product.stock}" }; setTextColor(if (product.stock > 0) Color.rgb(105, 210, 150) else Color.rgb(235, 105, 105)); textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 4, 0, 0) })
        if (product.description.isNotBlank()) card.addView(TextView(this).apply { text = product.description; setTextColor(Color.rgb(190, 190, 200)); textSize = 14f; setPadding(0, 10, 0, 0) })
        if (product.promotion.isNotBlank()) card.addView(TextView(this).apply { text = "PROMO  •  ${product.promotion}"; setTextColor(Color.rgb(255, 190, 80)); textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 8, 0, 0) })
        card.setOnClickListener { showProductDetail(product) }
        card.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 12) }
        return card
    }

    private fun showProductDetail(product: CatalogProduct) {
        val text = buildString {
            append(product.name).append("\n\n")
            if (product.brand.isNotBlank()) append("Marca: ").append(product.brand).append('\n')
            if (product.model.isNotBlank()) append("Modelo: ").append(product.model).append('\n')
            if (product.category.isNotBlank()) append("Categoría: ").append(product.category).append('\n')
            append("Precio: ").append(if (product.price.isBlank()) "No cargado" else "$ ${product.price}").append('\n')
            append("Stock: ").append(product.stock).append(" unidades\n")
            if (product.description.isNotBlank()) append("\nDescripción\n").append(product.description).append('\n')
            if (product.promotion.isNotBlank()) append("\nPromoción\n").append(product.promotion)
        }
        AlertDialog.Builder(this).setTitle("Detalle del producto").setMessage(text)
            .setNeutralButton("Editar") { _, _ -> showProductEditor(product) }
            .setNegativeButton("Eliminar") { _, _ -> confirmDelete(product) }
            .setPositiveButton("Cerrar", null).show()
    }

    private fun showImportOptions() {
        AlertDialog.Builder(this).setTitle("Carga rápida del catálogo")
            .setItems(arrayOf("Importar Excel / CSV", "Pegar una lista", "Ver formato de ejemplo")) { _, which ->
                when (which) {
                    0 -> importFile.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    1 -> showPasteDialog()
                    2 -> showFormatExample()
                }
            }.show()
    }

    private fun showFormatExample() {
        AlertDialog.Builder(this).setTitle("Formato recomendado")
            .setMessage("Nombre | Categoría | Marca | Modelo | Precio | Stock | Descripción | Promoción\n\nEjemplo:\nSamsung A25 5G | Celulares | Samsung | A25 5G | 450000 | 3 | 8GB RAM, 256GB | 10% OFF\nCargador USB-C 25W | Accesorios | Samsung | EP-T2510 | 25000 | 12 | Carga rápida |")
            .setPositiveButton("Entendido", null).show()
    }

    private fun showPasteDialog() {
        val input = EditText(this).apply {
            hint = "Pegá varias filas, una por producto..."; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(115, 115, 125)); textSize = 14f
            minLines = 8; gravity = Gravity.TOP; setPadding(14, 14, 14, 14); background = getDrawable(R.drawable.bg_catalog_input)
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 0, 24, 0); addView(input, LinearLayout.LayoutParams(-1, 260)) }
        val dialog = AlertDialog.Builder(this).setTitle("Pegar lista de productos")
            .setMessage("Columnas: Nombre | Categoría | Marca | Modelo | Precio | Stock | Descripción | Promoción")
            .setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Importar", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val added = importText(input.text.toString())
            if (added > 0) { dialog.dismiss(); afterImport(added) } else Toast.makeText(this, "No encontré filas válidas.", Toast.LENGTH_LONG).show()
        }}
        dialog.show()
    }

    private fun importTextFile(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: ""
            val added = importText(text)
            if (added > 0) afterImport(added) else Toast.makeText(this, "No encontré filas válidas en el archivo.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "No se pudo importar: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun importText(raw: String): Int {
        val clean = raw.replace("\r", "").trim()
        if (clean.isBlank()) return 0
        val lines = clean.lines().filter { it.isNotBlank() }
        val delimiter = when { lines.first().contains('\t') -> '\t'; lines.first().contains(';') -> ';'; else -> ',' }
        var start = 0
        val first = parseCsvLine(lines.first(), delimiter)
        if (first.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) in setOf("nombre", "producto", "name")) start = 1
        var added = 0
        for (line in lines.drop(start)) {
            val c = parseCsvLine(line, delimiter)
            if (c.isEmpty() || c[0].trim().isBlank()) continue
            products.add(CatalogProduct(System.currentTimeMillis() + added, c.getOrNull(0).orEmpty().trim(), c.getOrNull(1).orEmpty().trim(), c.getOrNull(2).orEmpty().trim(), c.getOrNull(3).orEmpty().trim(), c.getOrNull(4).orEmpty().trim(), c.getOrNull(5)?.trim()?.toIntOrNull() ?: 0, c.getOrNull(6).orEmpty().trim(), c.getOrNull(7).orEmpty().trim()))
            added++
        }
        if (added > 0) { saveProducts(); rebuildCategories(); refreshProducts() }
        return added
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>(); val sb = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') { if (quoted && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ } else quoted = !quoted }
            else if (ch == delimiter && !quoted) { out.add(sb.toString()); sb.setLength(0) } else sb.append(ch)
            i++
        }
        out.add(sb.toString()); return out
    }

    private fun importXlsx(uri: Uri) {
        try {
            val rows = contentResolver.openInputStream(uri)?.use { parseXlsx(it) } ?: emptyList()
            val added = importRows(rows)
            if (added > 0) afterImport(added) else Toast.makeText(this, "No encontré filas válidas en el Excel.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "No se pudo leer el Excel: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun parseXlsx(input: java.io.InputStream): List<List<String>> {
        val zip = ZipInputStream(input)
        val shared = mutableListOf<String>(); var sheetBytes: ByteArray? = null
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            val bytes = zip.readBytes()
            if (name == "xl/sharedStrings.xml") shared.addAll(parseSharedStrings(bytes))
            if (name == "xl/worksheets/sheet1.xml") sheetBytes = bytes
        }
        return if (sheetBytes != null) parseWorksheet(sheetBytes!!, shared) else emptyList()
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser(); parser.setInput(bytes.inputStream(), "UTF-8"); val result = mutableListOf<String>(); var text = StringBuilder(); var inside = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "t") { inside = true; text = StringBuilder() }
            else if (parser.eventType == XmlPullParser.TEXT && inside) text.append(parser.text)
            else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "t" && inside) { result.add(text.toString()); inside = false }
        }
        return result
    }

    private fun parseWorksheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser(); parser.setInput(bytes.inputStream(), "UTF-8")
        val rows = mutableListOf<List<String>>(); var row = mutableListOf<String>(); var cellIndex = 0; var cellType = ""; var cellRef = ""; var value = ""; var inValue = false; var inline = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> { row = mutableListOf(); cellIndex = 0 }
                    "c" -> { cellRef = parser.getAttributeValue(null, "r") ?: ""; cellType = parser.getAttributeValue(null, "t") ?: ""; value = ""; inline = cellType == "inlineStr"; val letters = cellRef.takeWhile { it.isLetter() }; cellIndex = letters.fold(0) { acc, ch -> acc * 26 + (ch.uppercaseChar() - 'A' + 1) } - 1 }
                    "v", "t" -> if (cellType == "s" || inline || parser.name == "v") inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) value += parser.text
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inValue = false
                    "c" -> { while (row.size <= cellIndex) row.add(""); row[cellIndex] = if (cellType == "s") shared.getOrNull(value.toIntOrNull() ?: -1).orEmpty() else value; cellIndex++ }
                    "row" -> if (row.any { it.isNotBlank() }) rows.add(row)
                }
            }
        }
        return rows
    }

    private fun importRows(rows: List<List<String>>): Int {
        if (rows.isEmpty()) return 0
        val start = if (rows.firstOrNull()?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) in setOf("nombre", "producto", "name")) 1 else 0
        var added = 0
        for (r in rows.drop(start)) {
            if (r.getOrNull(0).orEmpty().trim().isBlank()) continue
            products.add(CatalogProduct(System.currentTimeMillis() + added, r.getOrNull(0).orEmpty().trim(), r.getOrNull(1).orEmpty().trim(), r.getOrNull(2).orEmpty().trim(), r.getOrNull(3).orEmpty().trim(), r.getOrNull(4).orEmpty().trim(), r.getOrNull(5)?.trim()?.toIntOrNull() ?: 0, r.getOrNull(6).orEmpty().trim(), r.getOrNull(7).orEmpty().trim()))
            added++
        }
        if (added > 0) { saveProducts(); rebuildCategories(); refreshProducts() }
        return added
    }

    private fun queryFileName(uri: Uri): String {
        var name = ""
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) name = it.getString(0) }
        return name
    }

    private fun afterImport(added: Int) { Toast.makeText(this, "$added productos cargados correctamente.", Toast.LENGTH_LONG).show() }

    private fun showSortOptions() {
        AlertDialog.Builder(this).setTitle("Ordenar catálogo").setSingleChoiceItems(arrayOf("Orden de carga", "Nombre A → Z", "Nombre Z → A", "Precio menor → mayor", "Precio mayor → menor", "Mayor stock"), sortMode) { dialog, which -> sortMode = which; dialog.dismiss(); refreshProducts() }.show()
    }

    private fun showProductEditor(existing: CatalogProduct?) {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val name = createInput("Nombre del producto", existing?.name); val category = createInput("Categoría", existing?.category); val brand = createInput("Marca", existing?.brand); val model = createInput("Modelo", existing?.model); val price = createInput("Precio", existing?.price); val stock = createInput("Stock", existing?.stock?.toString()); val description = createInput("Descripción", existing?.description); val promotion = createInput("Promoción", existing?.promotion); val image = createInput("Imagen (opcional)", existing?.imageUri)
        listOf(name, category, brand, model, price, stock, description, promotion, image).forEach { form.addView(it) }
        form.addView(Button(this).apply { text = "Elegir foto del producto"; setOnClickListener { pendingImageInput = image; startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }, 7001) } })
        val dialog = AlertDialog.Builder(this).setTitle(if (existing == null) "Nuevo producto" else "Editar producto").setView(form).setNegativeButton("Cancelar", null).setPositiveButton(if (existing == null) "Agregar" else "Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val productName = name.text.toString().trim(); if (productName.isBlank()) { name.error = "Ingresá el nombre"; return@setOnClickListener }
                val stockValue = stock.text.toString().trim().toIntOrNull() ?: 0; if (stockValue < 0) { stock.error = "Stock inválido"; return@setOnClickListener }
                if (existing == null) products.add(CatalogProduct(System.currentTimeMillis(), productName, category.text.toString().trim(), brand.text.toString().trim(), model.text.toString().trim(), price.text.toString().trim(), stockValue, description.text.toString().trim(), promotion.text.toString().trim(), image.text.toString().trim()))
                else { existing.name = productName; existing.category = category.text.toString().trim(); existing.brand = brand.text.toString().trim(); existing.model = model.text.toString().trim(); existing.price = price.text.toString().trim(); existing.stock = stockValue; existing.description = description.text.toString().trim(); existing.promotion = promotion.text.toString().trim(); existing.imageUri = image.text.toString().trim() }
                saveProducts(); rebuildCategories(); refreshProducts(); dialog.dismiss(); Toast.makeText(this, if (existing == null) "Producto agregado" else "Producto actualizado", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun createInput(hint: String, value: String?): EditText = EditText(this).apply {
        this.hint = hint; setText(value ?: ""); setTextColor(Color.WHITE); setHintTextColor(Color.rgb(115, 115, 125)); textSize = 15f; setSingleLine(false); setPadding(14, 10, 14, 10); background = getDrawable(R.drawable.bg_catalog_input)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 8) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7001 && resultCode == RESULT_OK) data?.data?.let { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            pendingImageInput?.setText(uri.toString()); pendingImageInput = null
        }
    }

    private fun confirmDelete(product: CatalogProduct) {
        AlertDialog.Builder(this).setTitle("Eliminar producto").setMessage("¿Querés eliminar \"${product.name}\" del catálogo?")
            .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar") { _, _ -> products.removeAll { it.id == product.id }; saveProducts(); refreshProducts(); rebuildCategories(); Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show() }.show()
    }
}
