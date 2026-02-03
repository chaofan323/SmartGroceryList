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
import android.view.Menu
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var groceryAdapter: GroceryAdapter

    // Budget UI
    private lateinit var tvBudgetTitle: TextView
    private lateinit var tvSpentLine: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvBudgetEmoji: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var btnViewDetails: MaterialButton
    private lateinit var btnSetBudget: MaterialButton

    private lateinit var budgetCard: View

    private val items = mutableListOf<GroceryItem>()

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) refreshUI()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        recycler = findViewById(R.id.recycler)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        fabAdd = findViewById(R.id.fabAdd)

        tvBudgetTitle = findViewById(R.id.tvBudgetTitle)
        tvSpentLine = findViewById(R.id.tvSpentLine)
        tvPercent = findViewById(R.id.tvPercent)
        tvBudgetEmoji = findViewById(R.id.tvBudgetEmoji)
        progressBudget = findViewById(R.id.progressBudget)
        btnViewDetails = findViewById(R.id.btnViewDetails)
        btnSetBudget = findViewById(R.id.btnSetBudget)

        budgetCard = findViewById(R.id.budgetCard)


        btnViewDetails.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler.layoutManager = LinearLayoutManager(this)

        groceryAdapter = GroceryAdapter(
            rows = buildRows(items).toMutableList(),
            onItemClick = { item ->
                val intent = Intent(this, AddEditItemActivity::class.java).apply {
                    putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_EDIT)
                    putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.id)
                }
                addEditLauncher.launch(intent)
            },
            onPurchasedToggle = { item, isPurchased ->
                GroceryRepository.setPurchased(item.id, isPurchased)
                refreshUI()
            },
            onDeleteClick = { item ->
                GroceryRepository.deleteItem(item.id)
                refreshUI()
                Toast.makeText(this, "Deleted: ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )

        recycler.adapter = groceryAdapter

        refreshUI()

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditItemActivity::class.java).apply {
                putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_ADD)
            }
            addEditLauncher.launch(intent)
        }
    }

    private fun refreshUI() {

        items.clear()
        items.addAll(GroceryRepository.getAllItems())

        val hasItems = items.isNotEmpty()
        updateEmptyState(hasItems)

        groceryAdapter.update(if (hasItems) buildRows(items) else emptyList())

        updateBudgetUI()
    }

    private fun updateEmptyState(hasItems: Boolean) {
        if (hasItems) {
            emptyStateContainer.visibility = View.GONE
            recycler.visibility = View.VISIBLE
            budgetCard.visibility = View.VISIBLE
        } else {
            emptyStateContainer.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            budgetCard.visibility = View.GONE
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun showSetBudgetDialog() {
        val input = EditText(this).apply {
            hint = "e.g., 50.00"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(40, 30, 40, 10)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Budget")
            .setMessage("Enter your total budget for this shopping trip.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim().toDoubleOrNull()
                if (value == null || value < 0) {
                    Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                GroceryRepository.setBudget(this, value)
                updateBudgetUI()
            }
            .show()
    }

    private fun updateBudgetUI() {
        val budget = GroceryRepository.getBudget(this)
        val spent = GroceryRepository.getTotalSpent()

        val percent = if (budget <= 0.0) 0 else ((spent / budget) * 100).toInt().coerceAtLeast(0)

        tvBudgetTitle.text = String.format(Locale.US, "Budget: $%.2f", budget)
        tvSpentLine.text = String.format(Locale.US, "$%.2f spent out of $%.2f", spent, budget)

        progressBudget.max = 100
        progressBudget.progress = percent.coerceIn(0, 100)

        tvPercent.text = "${percent}% Spent"

        tvBudgetEmoji.text = when {
            budget <= 0.0 -> "📝"
            percent < 50 -> "😊"
            percent < 80 -> "🙂"
            percent < 100 -> "😬"
            else -> "😡"
        }

        btnSetBudget.text = if (budget <= 0.0) "Set Budget" else "Edit Budget"
    }
}
