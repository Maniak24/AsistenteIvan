package com.ivan.asistente

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

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
        if (uri == null) return@registerForActivityResult
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        if (queryFileName(uri).lowercase(Locale.getDefault()).endsWith(".xlsx")) importXlsx(uri) else importTextFile(uri)
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

    private fun priceNumber(value: String): Double = value.replace(".", "").replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

    private fun createProductCard(product: CatalogProduct): View {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getDrawable(R.drawable.bg_product_card); setPadding(16, 14, 16, 14) }
        if (product.imageUri.isNotBlank()) {
            try { card.addView(ImageView(this).apply { setImageURI(Uri.parse(product.imageUri)); scaleType = ImageView.ScaleType.CENTER_CROP }, LinearLayout.LayoutParams(-1, 150).apply { bottomMargin = 10 }) } catch (_: Exception) {}
        }
        card.addView(TextView(this).apply { text = product.name; setTextColor(Color.WHITE); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
        val secondary = listOf(product.brand, product.model, product.category).filter { it.isNotBlank() }.joinToString(" • ")
        if (secondary.isNotBlank()) card.addView(TextView(this).apply { text = secondary; setTextColor(Color.rgb(150, 150, 163)); textSize = 13f; setPadding(0, 4, 0, 0) })
        card.addView(TextView(this).apply { text = if (product.price.isBlank()) "Precio no cargado" else "$ ${product.price}"; setTextColor(Color.rgb(154, 123, 255)); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 12, 0, 0) })
        card.addView(TextView(this).apply { text = if (product.stock <= 0) "Sin stock" else if (product.stock == 1) "Última unidad" else "Stock: ${product.stock}"; setTextColor(if (product.stock > 0) Color.rgb(105, 210, 150) else Color.rgb(235, 105, 105)); textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 4, 0, 0) })
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
            .setMessage("Nombre | Categoría | Marca | Modelo | Precio | Stock | Descripción | Promoción\n\nSamsung A25 5G | Celulares | Samsung | A25 5G | 450000 | 3 | 8GB RAM, 256GB | 10% OFF")
            .setPositiveButton("Entendido", null).show()
    }

    private fun showPasteDialog() {
        val input = EditText(this).apply { hint = "Pegá varias filas, una por producto..."; setTextColor(Color.WHITE); setHintTextColor(Color.rgb(115, 115, 125)); textSize = 14f; minLines = 8; gravity = Gravity.TOP; setPadding(14, 14, 14, 14); background = getDrawable(R.drawable.bg_catalog_input) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 0, 24, 0); addView(input, LinearLayout.LayoutParams(-1, 260)) }
        val dialog = AlertDialog.Builder(this).setTitle("Pegar lista de productos").setMessage("Columnas: Nombre | Categoría | Marca | Modelo | Precio | Stock | Descripción | Promoción").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Importar", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { val added = importText(input.text.toString()); if (added > 0) { dialog.dismiss(); afterImport(added) } else Toast.makeText(this, "No encontré filas válidas.", Toast.LENGTH_LONG).show() } }
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
        val lines = raw.replace("\r", "").lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0
        val delimiter = detectDelimiter(lines.first())
        val first = parseDelimitedLine(lines.first(), delimiter)
        val start = if (first.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) in setOf("nombre", "producto", "name")) 1 else 0
        var added = 0
        for (line in lines.drop(start)) {
            val c = parseDelimitedLine(line, delimiter)
            if (c.firstOrNull().orEmpty().trim().isBlank()) continue
            addRow(c, added++)
        }
        if (added > 0) { saveProducts(); rebuildCategories(); refreshProducts() }
        return added
    }

    private fun detectDelimiter(line: String): Char = when {
        line.contains('\t') -> '\t'
        line.contains('|') -> '|'
        line.contains(';') -> ';'
        else -> ','
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>(); val sb = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (quoted && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ } else quoted = !quoted
            } else if (ch == delimiter && !quoted) { out.add(sb.toString()); sb.setLength(0) } else sb.append(ch)
            i++
        }
        out.add(sb.toString()); return out
    }

    private fun addRow(c: List<String>, index: Int) {
        products.add(CatalogProduct(
            System.currentTimeMillis() + index,
            c.getOrNull(0).orEmpty().trim(), c.getOrNull(1).orEmpty().trim(), c.getOrNull(2).orEmpty().trim(),
            c.getOrNull(3).orEmpty().trim(), c.getOrNull(4).orEmpty().trim(), c.getOrNull(5).orEmpty().trim().toIntOrNull() ?: 0,
            c.getOrNull(6).orEmpty().trim(), c.getOrNull(7).orEmpty().trim()
        ))
    }

    private fun importXlsx(uri: Uri) {
        try {
            val rows = contentResolver.openInputStream(uri)?.use { parseXlsx(it) } ?: emptyList()
            val start = if (rows.firstOrNull()?.firstOrNull()?.trim()?.lowercase(Locale.getDefault()) in setOf("nombre", "producto", "name")) 1 else 0
            var added = 0
            for (row in rows.drop(start)) {
                if (row.firstOrNull().orEmpty().trim().isBlank()) continue
                addRow(row, added++)
            }
            if (added > 0) { saveProducts(); rebuildCategories(); refreshProducts(); afterImport(added) } else Toast.makeText(this, "No encontré filas válidas en el Excel.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "No se pudo leer el Excel: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun parseXlsx(input: java.io.InputStream): List<List<String>> {
        val shared = mutableListOf<String>(); var sheet: ByteArray? = null
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val bytes = zip.readBytes()
                when (entry.name) {
                    "xl/sharedStrings.xml" -> shared.addAll(parseSharedStrings(bytes))
                    "xl/worksheets/sheet1.xml" -> sheet = bytes
                }
            }
        }
        return if (sheet == null) emptyList() else parseSheet(sheet!!, shared)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }.newDocumentBuilder().parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("si")
        return (0 until nodes.length).map { i ->
            val ts = (nodes.item(i) as org.w3c.dom.Element).getElementsByTagName("t")
            buildString { for (j in 0 until ts.length) append(ts.item(j).textContent) }
        }
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = false }.newDocumentBuilder().parse(bytes.inputStream())
        val rowNodes = doc.getElementsByTagName("row")
        val result = mutableListOf<List<String>>()
        for (i in 0 until rowNodes.length) {
            val row = rowNodes.item(i) as org.w3c.dom.Element
            val cells = row.getElementsByTagName("c")
            val values = mutableListOf<String>()
            for (j in 0 until cells.length) {
                val cell = cells.item(j) as org.w3c.dom.Element
                val ref = cell.getAttribute("r")
                val col = ref.takeWhile { it.isLetter() }.fold(0) { acc, ch -> acc * 26 + (ch.uppercaseChar() - 'A' + 1) } - 1
                while (values.size <= col) values.add("")
                val type = cell.getAttribute("t")
                val v = cell.getElementsByTagName("v")
                val raw = if (v.length > 0) v.item(0).textContent else ""
                values[col] = if (type == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw
            }
            if (values.any { it.isNotBlank() }) result.add(values)
        }
        return result
    }

    private fun queryFileName(uri: Uri): String {
        var name = ""
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) name = it.getString(0) }
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
                val productName = name.text.toString().trim()
                if (productName.isBlank()) { name.error = "Ingresá el nombre"; return@setOnClickListener }
                val stockValue = stock.text.toString().trim().toIntOrNull() ?: 0
                if (stockValue < 0) { stock.error = "Stock inválido"; return@setOnClickListener }
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
