package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.smartgrocerylist.data.GroceryItem
import com.example.smartgrocerylist.data.GroceryRepository

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var groceryAdapter: GroceryAdapter

    // In-memory data (for now). Later you can save/load via SharedPreferences or Room.
//    private val items = mutableListOf(
//        GroceryItem(1, "Apples", 4.50, "Produce", false),
//        GroceryItem(2, "Bananas", 2.99, "Produce", false),
//        GroceryItem(3, "Milk", 1.99, "Dairy", true),
//        GroceryItem(4, "Chicken Breast", 9.99, "Meat", false),
//        GroceryItem(5, "Chips", 2.49, "Snacks", false)
//    )

    private val items = mutableListOf<GroceryItem>()

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Always refresh after add/edit returns OK
                refreshUI()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views
        toolbar = findViewById(R.id.toolbar)
        recycler = findViewById(R.id.recycler)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        fabAdd = findViewById(R.id.fabAdd)

        // Toolbar setup
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // matches wireframe

        // RecyclerView setup
        recycler.layoutManager = LinearLayoutManager(this)

        groceryAdapter = GroceryAdapter(
            rows = buildRows(items).toMutableList(),
            onItemClick = { item ->
//                Toast.makeText(this, "Edit: ${item.name}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AddEditItemActivity::class.java).apply {
                    putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_EDIT)
                    putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.id)
                }
                addEditLauncher.launch(intent) },

            onPurchasedToggle = { item, _ ->
                GroceryRepository.togglePurchased(item.id)
                refreshUI()
            },

            onDeleteClick = { item ->
                GroceryRepository.deleteItem(item.id)
                refreshUI()
                Toast.makeText(this, "Deleted: ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )


        recycler.adapter = groceryAdapter

        // Initial UI state
        refreshUI()

        // FAB click
        fabAdd.setOnClickListener {
            // Later: startActivity(Intent(this, AddItemActivity::class.java))
//            Toast.makeText(this, "Add item (coming next)", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AddEditItemActivity::class.java).apply {
                putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_ADD)
            }
            addEditLauncher.launch(intent)
        }
    }

    private fun refreshUI() {
        if (GroceryRepository.getAllItems().isEmpty()) {
            GroceryRepository.addItem("Apples", 4.50, "Produce")
            GroceryRepository.addItem("Bananas", 2.99, "Produce")
            GroceryRepository.addItem("Milk", 1.99, "Dairy")
            GroceryRepository.addItem("Chicken Breast", 9.99, "Meat")
            GroceryRepository.addItem("Chips", 2.49, "Snacks")
            // Optional: mark one purchased
            // GroceryRepository.togglePurchased(3)
        }
        // Pull latest data from repository
        items.clear()
        items.addAll(GroceryRepository.getAllItems())

        val hasItems = items.isNotEmpty()
        updateEmptyState(hasItems)

        if (hasItems) {
            groceryAdapter.update(buildRows(items))
        } else {
            groceryAdapter.update(emptyList())
        }
    }


    private fun updateEmptyState(hasItems: Boolean) {
        if (hasItems) {
            emptyStateContainer.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        } else {
            emptyStateContainer.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        }
    }

    // Handles toolbar back arrow
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
