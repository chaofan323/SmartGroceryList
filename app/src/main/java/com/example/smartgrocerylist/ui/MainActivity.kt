package com.example.smartgrocerylist.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R
import com.example.smartgrocerylist.data.GroceryItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recycler: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptySubtitle: TextView
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

    // Search / filter UI
    private lateinit var filterCard: View
    private lateinit var etSearch: EditText
    private lateinit var spCategoryFilter: Spinner

    // Latest observed data
    private var latestAllItems: List<GroceryItem> = emptyList()
    private var latestFilteredItems: List<GroceryItem> = emptyList()

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updateBudgetUI()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GroceryViewModel::class.java]

        toolbar = findViewById(R.id.toolbar)
        recycler = findViewById(R.id.recycler)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptySubtitle = findViewById(R.id.emptySubtitle)
        fabAdd = findViewById(R.id.fabAdd)

        tvBudgetTitle = findViewById(R.id.tvBudgetTitle)
        tvSpentLine = findViewById(R.id.tvSpentLine)
        tvPercent = findViewById(R.id.tvPercent)
        tvBudgetEmoji = findViewById(R.id.tvBudgetEmoji)
        progressBudget = findViewById(R.id.progressBudget)
        btnViewDetails = findViewById(R.id.btnViewDetails)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        budgetCard = findViewById(R.id.budgetCard)

        filterCard = findViewById(R.id.filterCard)
        etSearch = findViewById(R.id.etSearch)
        spCategoryFilter = findViewById(R.id.spCategoryFilter)

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
            },
            onDeleteClick = { item ->
                viewModel.deleteItem(item.id)
                Toast.makeText(this, "Deleted: ${item.name}", Toast.LENGTH_SHORT).show()
            }
        )
        recycler.adapter = groceryAdapter

        setupFilterControls()

        // Observe full list
        viewModel.allItems.observe(this) { items ->
            latestAllItems = items
            updateBudgetUI()
            updateMainContent()
        }

        // Observe filtered list
        viewModel.filteredItems.observe(this) { items ->
            latestFilteredItems = items
            updateMainContent()
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditItemActivity::class.java).apply {
                putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_ADD)
            }
            addEditLauncher.launch(intent)
        }
    }

    private fun setupFilterControls() {
        val spinnerAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.filter_category_options,
            android.R.layout.simple_spinner_item
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategoryFilter.adapter = spinnerAdapter

        etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        spCategoryFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent?.getItemAtPosition(position)?.toString()
                    ?: GroceryViewModel.CATEGORY_ALL
                viewModel.setSelectedCategory(selected)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                viewModel.setSelectedCategory(GroceryViewModel.CATEGORY_ALL)
            }
        }
    }

    private fun updateMainContent() {
        val hasAnyItems = latestAllItems.isNotEmpty()
        val hasFilteredItems = latestFilteredItems.isNotEmpty()

        when {
            !hasAnyItems -> {
                emptyStateContainer.visibility = View.VISIBLE
                recycler.visibility = View.GONE
                budgetCard.visibility = View.GONE
                filterCard.visibility = View.GONE

                emptyTitle.text = getString(R.string.empty_title)
                emptySubtitle.text = getString(R.string.empty_subtitle)

                groceryAdapter.update(emptyList())
            }

            hasFilteredItems -> {
                emptyStateContainer.visibility = View.GONE
                recycler.visibility = View.VISIBLE
                budgetCard.visibility = View.VISIBLE
                filterCard.visibility = View.VISIBLE

                groceryAdapter.update(buildRows(latestFilteredItems))
            }

            else -> {
                emptyStateContainer.visibility = View.VISIBLE
                recycler.visibility = View.GONE
                budgetCard.visibility = View.VISIBLE
                filterCard.visibility = View.VISIBLE

                emptyTitle.text = getString(R.string.no_results_title)
                emptySubtitle.text = getString(R.string.no_results_subtitle)

                groceryAdapter.update(emptyList())
            }
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
            hint = getString(R.string.budget_input_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(40, 30, 40, 10)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.set_budget)
            .setMessage(R.string.set_budget_message)
            .setView(input)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val value = input.text.toString().trim().toDoubleOrNull()
                if (value == null || value < 0) {
                    Toast.makeText(this, getString(R.string.invalid_budget_amount), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.setBudget(value)
                updateBudgetUI()
            }
            .show()
    }

    private fun updateBudgetUI() {
        val budget = viewModel.getBudget()
        val spent = latestAllItems
            .filter { it.purchased }
            .sumOf { it.price }

        val percent = if (budget <= 0.0) {
            0
        } else {
            ((spent / budget) * 100).toInt().coerceAtLeast(0)
        }

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

        btnSetBudget.text = getString(
            if (budget <= 0.0) R.string.set_budget else R.string.edit_budget
        )
    }
}