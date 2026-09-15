package com.ivan.asistente

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private val prefs by lazy {
        getSharedPreferences("mi_pc_catalog", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_catalog)

        container = findViewById(R.id.products_container)
        search = findViewById(R.id.catalog_search)
        countText = findViewById(R.id.catalog_count)
        stockText = findViewById(R.id.catalog_stock)

        loadProducts()
        refreshProducts()

        findViewById<View>(R.id.btn_add_product).setOnClickListener {
            showProductEditor(null)
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                refreshProducts()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadProducts() {
        products.clear()

        val raw = prefs.getString("products", "[]") ?: "[]"

        try {
            val array = JSONArray(raw)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                products.add(
                    CatalogProduct(
                        id = obj.optLong("id"),
                        name = obj.optString("name"),
                        category = obj.optString("category"),
                        brand = obj.optString("brand"),
                        price = obj.optString("price"),
                        stock = obj.optInt("stock"),
                        description = obj.optString("description"),
                        promotion = obj.optString("promotion")
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No se pudo cargar el catálogo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveProducts() {
        val array = JSONArray()

        products.forEach { product ->
            array.put(
                JSONObject().apply {
                    put("id", product.id)
                    put("name", product.name)
                    put("category", product.category)
                    put("brand", product.brand)
                    put("price", product.price)
                    put("stock", product.stock)
                    put("description", product.description)
                    put("promotion", product.promotion)
                }
            )
        }

        prefs.edit()
            .putString("products", array.toString())
            .apply()
    }

    private fun refreshProducts() {
        container.removeAllViews()

        val query = search.text.toString().trim().lowercase()

        val filtered = products.filter {
            query.isEmpty() ||
                it.name.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.brand.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
        }

        countText.text =
            if (filtered.size == 1) "1 producto"
            else "${filtered.size} productos"

        stockText.text = "Stock: ${products.sumOf { it.stock }}"

        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (products.isEmpty()) {
                    "Todavía no hay productos.\n\nTocá + para agregar el primero."
                } else {
                    "No encontramos productos con esa búsqueda."
                }

                setTextColor(android.graphics.Color.rgb(150, 150, 163))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(24, 80, 24, 80)
            }

            container.addView(empty)
            return
        }

        filtered.forEach { product ->
            container.addView(createProductCard(product))
        }
    }

    private fun createProductCard(product: CatalogProduct): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_product_card)
            setPadding(16, 14, 16, 14)
        }

        val name = TextView(this).apply {
            text = product.name
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        card.addView(name)

        if (product.brand.isNotBlank() || product.category.isNotBlank()) {
            val subtitle = TextView(this).apply {
                text = listOf(product.brand, product.category)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")

                setTextColor(android.graphics.Color.rgb(150, 150, 163))
                textSize = 13f
                setPadding(0, 4, 0, 0)
            }

            card.addView(subtitle)
        }

        val price = TextView(this).apply {
            text = if (product.price.isBlank()) {
                "Precio no cargado"
            } else {
                "$ ${product.price}"
            }

            setTextColor(android.graphics.Color.rgb(154, 123, 255))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }

        card.addView(price)

        val stock = TextView(this).apply {
            text = when {
                product.stock <= 0 -> "Sin stock"
                product.stock == 1 -> "Última unidad"
                else -> "Stock: ${product.stock}"
            }

            setTextColor(
                if (product.stock > 0)
                    android.graphics.Color.rgb(105, 210, 150)
                else
                    android.graphics.Color.rgb(235, 105, 105)
            )

            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 0)
        }

        card.addView(stock)

        if (product.description.isNotBlank()) {
            val description = TextView(this).apply {
                text = product.description
                setTextColor(android.graphics.Color.rgb(190, 190, 200))
                textSize = 14f
                setPadding(0, 10, 0, 0)
            }

            card.addView(description)
        }

        if (product.promotion.isNotBlank()) {
            val promotion = TextView(this).apply {
                text = "🔥 ${product.promotion}"
                setTextColor(android.graphics.Color.rgb(255, 190, 80))
                textSize = 13f
                setPadding(0, 8, 0, 0)
            }

            card.addView(promotion)
        }

        card.setOnClickListener {
            showProductActions(product)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 12)

        card.layoutParams = params

        return card
    }

    private fun showProductActions(product: CatalogProduct) {
        val options = arrayOf(
            "✏️  Editar producto",
            "🗑️  Eliminar producto"
        )

        AlertDialog.Builder(this)
            .setTitle(product.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showProductEditor(product)
                    1 -> confirmDelete(product)
                }
            }
            .show()
    }

    private fun showProductEditor(existing: CatalogProduct?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }

        val name = createInput("Nombre del producto", existing?.name)
        val category = createInput("Categoría", existing?.category)
        val brand = createInput("Marca / modelo", existing?.brand)
        val price = createInput("Precio", existing?.price)
        val stock = createInput("Stock", existing?.stock?.toString())
        val description = createInput("Descripción", existing?.description)
        val promotion = createInput("Promoción", existing?.promotion)

        form.addView(name)
        form.addView(category)
        form.addView(brand)
        form.addView(price)
        form.addView(stock)
        form.addView(description)
        form.addView(promotion)

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (existing == null)
                    "Nuevo producto"
                else
                    "Editar producto"
            )
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(
                if (existing == null) "Agregar" else "Guardar",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val productName = name.text.toString().trim()

                if (productName.isBlank()) {
                    name.error = "Ingresá el nombre del producto"
                    return@setOnClickListener
                }

                val stockValue = stock.text.toString()
                    .trim()
                    .toIntOrNull() ?: 0

                if (stockValue < 0) {
                    stock.error = "El stock no puede ser negativo"
                    return@setOnClickListener
                }

                if (existing == null) {
                    products.add(
                        CatalogProduct(
                            id = System.currentTimeMillis(),
                            name = productName,
                            category = category.text.toString().trim(),
                            brand = brand.text.toString().trim(),
                            price = price.text.toString().trim(),
                            stock = stockValue,
                            description = description.text.toString().trim(),
                            promotion = promotion.text.toString().trim()
                        )
                    )
                } else {
                    existing.name = productName
                    existing.category = category.text.toString().trim()
                    existing.brand = brand.text.toString().trim()
                    existing.price = price.text.toString().trim()
                    existing.stock = stockValue
                    existing.description = description.text.toString().trim()
                    existing.promotion = promotion.text.toString().trim()
                }

                saveProducts()
                refreshProducts()
                dialog.dismiss()

                Toast.makeText(
                    this,
                    if (existing == null)
                        "Producto agregado"
                    else
                        "Producto actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun createInput(
        hint: String,
        value: String?
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value ?: "")
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.rgb(115, 115, 125))
            textSize = 15f
            setSingleLine(false)
            setPadding(14, 10, 14, 10)
            background = getDrawable(R.drawable.bg_catalog_input)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 0, 0, 10)
            layoutParams = params
        }
    }

    private fun confirmDelete(product: CatalogProduct) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Querés eliminar \"${product.name}\" del catálogo?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                products.removeAll { it.id == product.id }
                saveProducts()
                refreshProducts()

                Toast.makeText(
                    this,
                    "Producto eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }
}
