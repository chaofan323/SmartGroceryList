package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R

class AddEditItemActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val MODE_ADD = "mode_add"
        const val MODE_EDIT = "mode_edit"
    }

    private lateinit var etItemName: EditText
    private lateinit var etPrice: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private lateinit var tvAddSuggestionsTitle: TextView
    private lateinit var rvAddSuggestions: RecyclerView
    private lateinit var suggestionAdapter: SimpleStringAdapter

    private lateinit var viewModel: GroceryViewModel

    private var mode: String = MODE_ADD
    private var editingItemId: Int = -1

    private var validatedName: String = ""
    private var validatedPrice: Double = 0.0
    private var validatedCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_item)

        viewModel = ViewModelProvider(this)[GroceryViewModel::class.java]

        bindViews()
        setupCategorySpinner()
        setupSuggestions()
        setupCancel()
        setupSave()
        initFromIntent()

        etPrice.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) etPrice.selectAll()
        }
    }

    private fun bindViews() {
        etItemName = findViewById(R.id.etItemName)
        etPrice = findViewById(R.id.etPrice)
        spCategory = findViewById(R.id.spCategory)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)

        tvAddSuggestionsTitle = findViewById(R.id.tvAddSuggestionsTitle)
        rvAddSuggestions = findViewById(R.id.rvAddSuggestions)
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.category_options).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
        spCategory.setSelection(0)
    }

    private fun setupSuggestions() {
        suggestionAdapter = SimpleStringAdapter(emptyList())
        rvAddSuggestions.layoutManager = LinearLayoutManager(this)
        rvAddSuggestions.adapter = suggestionAdapter
        rvAddSuggestions.isNestedScrollingEnabled = false

        viewModel.smartSuggestions.observe(this) { suggestions ->
            val safeSuggestions = suggestions ?: emptyList()

            if (safeSuggestions.isEmpty()) {
                tvAddSuggestionsTitle.visibility = View.GONE
                rvAddSuggestions.visibility = View.GONE
            } else {
                tvAddSuggestionsTitle.visibility = View.VISIBLE
                rvAddSuggestions.visibility = View.VISIBLE
                suggestionAdapter.update(safeSuggestions)
            }
        }
    }

    private fun setupCancel() {
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupSave() {
        btnSave.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            if (mode == MODE_EDIT) {
                viewModel.updateItem(editingItemId, validatedName, validatedPrice, validatedCategory)
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addItem(validatedName, validatedPrice, validatedCategory)
                Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show()
            }

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun initFromIntent() {
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ADD

        if (mode == MODE_EDIT) {
            editingItemId = intent.getIntExtra(EXTRA_ITEM_ID, -1)
            val item = if (editingItemId != -1) viewModel.getItemById(editingItemId) else null

            if (item == null) {
                Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            prefillFields(item.name, item.price, item.category)
        }
    }

    private fun prefillFields(name: String, price: Double, category: String) {
        etItemName.setText(name)
        etPrice.setText(String.format("%.2f", price))
        val categories = resources.getStringArray(R.array.category_options)
        val index = categories.indexOf(category)
        spCategory.setSelection(if (index >= 0) index else 0)
    }

    private fun validateInputs(): Boolean {
        etItemName.error = null
        etPrice.error = null

        val name = etItemName.text.toString().trim()
        if (name.isEmpty()) {
            etItemName.error = "Name cannot be empty"
            etItemName.requestFocus()
            return false
        }

        val priceText = etPrice.text.toString().trim()
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            etPrice.error = "Price must be a positive number"
            etPrice.requestFocus()
            return false
        }

        val pos = spCategory.selectedItemPosition
        if (pos == 0) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            spCategory.requestFocus()
            return false
        }

        validatedName = name
        validatedPrice = price
        validatedCategory = spCategory.selectedItem.toString()
        return true
    }
}