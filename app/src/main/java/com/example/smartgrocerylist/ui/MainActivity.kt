package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.view.Menu
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var groceryAdapter: GroceryAdapter
    private lateinit var viewModel: GroceryViewModel

    // Budget UI
    private lateinit var tvBudgetTitle: TextView
    private lateinit var tvSpentLine: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvBudgetEmoji: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var btnViewDetails: MaterialButton
    private lateinit var btnSetBudget: MaterialButton
    private lateinit var budgetCard: View

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) updateBudgetUI()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[GroceryViewModel::class.java]

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
        btnSetBudget.setOnClickListener { showSetBudgetDialog() }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler.layoutManager = LinearLayoutManager(this)

        groceryAdapter = GroceryAdapter(
            rows = mutableListOf(),
            onItemClick = { item ->
                val intent = Intent(this, AddEditItemActivity::class.java).apply {
                    putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_EDIT)
                    putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.id)
                }
                addEditLauncher.launch(intent)
            },
            onPurchasedToggle = { item, isPurchased ->
                viewModel.setPurchased(item.id, isPurchased)
                updateBudgetUI()
            },
            onDeleteClick = { item ->
                viewModel.deleteItem(item.id)
                updateBudgetUI()
                Toast.makeText(this, "Deleted: ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )
        recycler.adapter = groceryAdapter

        // Observe LiveData — UI updates automatically when data changes
        viewModel.allItems.observe(this) { items ->
            val hasItems = items.isNotEmpty()
            updateEmptyState(hasItems)
            groceryAdapter.update(if (hasItems) buildRows(items) else emptyList())
            updateBudgetUI()
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditItemActivity::class.java).apply {
                putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_ADD)
            }
            addEditLauncher.launch(intent)
        }
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
            android.R.id.home -> { finish(); true }
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
                viewModel.setBudget(value)
                updateBudgetUI()
            }
            .show()
    }

    private fun updateBudgetUI() {
        val budget = viewModel.getBudget()
        val spent = viewModel.getTotalSpent()

        val percent = if (budget <= 0.0) 0 else ((spent / budget) * 100).toInt().coerceAtLeast(0)

        tvBudgetTitle.text = getString(R.string.budget_title, budget)
        tvSpentLine.text = getString(R.string.budget_spent_line, spent, budget)

        progressBudget.max = 100
        progressBudget.progress = percent.coerceIn(0, 100)
        tvPercent.text = getString(R.string.budget_percent, percent)

        tvBudgetEmoji.text = when {
            budget <= 0.0 -> "📝"
            percent < 50 -> "😊"
            percent < 80 -> "🙂"
            percent < 100 -> "😬"
            else -> "😡"
        }

        btnSetBudget.text = getString(if (budget <= 0.0) R.string.set_budget else R.string.edit_budget)
    }
}
