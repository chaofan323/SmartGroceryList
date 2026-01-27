package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgrocerylist.R
import com.example.smartgrocerylist.data.GroceryRepository

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

    private var mode: String = MODE_ADD
    private var editingItemId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_item)

        bindViews()
        setupCategorySpinner()
        setupCancel()

        initFromIntent()
    }

    private fun bindViews() {
        etItemName = findViewById(R.id.etItemName)
        etPrice = findViewById(R.id.etPrice)
        spCategory = findViewById(R.id.spCategory)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.category_options).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = adapter
    }

    private fun setupCancel() {
        btnCancel.setOnClickListener { finish() }
    }

    private fun initFromIntent() {
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ADD

        if (mode == MODE_EDIT) {
            editingItemId = intent.getIntExtra(EXTRA_ITEM_ID, -1)
            if (editingItemId == -1) {
                Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val item = GroceryRepository.getItemById(editingItemId)
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
        etPrice.setText(price.toString())

        val categories = resources.getStringArray(R.array.category_options)
        val index = categories.indexOf(category)
        spCategory.setSelection(if (index >= 0) index else 0)
    }
}
