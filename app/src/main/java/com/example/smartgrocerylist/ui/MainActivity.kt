package com.example.smartgrocerylist.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
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

    private lateinit var tvBudgetTitle: TextView
    private lateinit var tvSpentLine: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvBudgetEmoji: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var btnViewDetails: MaterialButton
    private lateinit var btnSetBudget: MaterialButton
    private lateinit var budgetCard: View

    private lateinit var filterCard: View
    private lateinit var etSearch: EditText
    private lateinit var spCategoryFilter: Spinner

    private var latestAllItems: List<GroceryItem> = emptyList()
    private var latestFilteredItems: List<GroceryItem> = emptyList()

    private var latestBudgetValue: Double = 0.0
    private var latestSpentValue: Double = 0.0
    private var latestSpendingPercent: Int = 0

    private var pendingReminderDelayMinutes: Int? = null

    private val addEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val delay = pendingReminderDelayMinutes
            pendingReminderDelayMinutes = null

            if (granted && delay != null) {
                ReminderScheduler.scheduleReminder(this, delay)
                Toast.makeText(this, "Reminder set.", Toast.LENGTH_SHORT).show()
            } else if (!granted) {
                Toast.makeText(this, "Notification permission is required for reminders.", Toast.LENGTH_SHORT).show()
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
        observeData()

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditItemActivity::class.java).apply {
                putExtra(AddEditItemActivity.EXTRA_MODE, AddEditItemActivity.MODE_ADD)
            }
            addEditLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadBudget()
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

        spCategoryFilter.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
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

    private fun observeData() {
        viewModel.allItems.observe(this) { items ->
            latestAllItems = items
            updateMainContent()
        }

        viewModel.filteredItems.observe(this) { items ->
            latestFilteredItems = items
            updateMainContent()
        }

        viewModel.currentBudget.observe(this) { budget ->
            latestBudgetValue = budget ?: 0.0
            renderBudgetCard()
        }

        viewModel.totalSpent.observe(this) { spent ->
            latestSpentValue = spent ?: 0.0
            renderBudgetCard()
        }

        viewModel.spendingPercent.observe(this) { percent ->
            latestSpendingPercent = percent ?: 0
            renderBudgetCard()
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

    private fun renderBudgetCard() {
        val budget = latestBudgetValue
        val spent = latestSpentValue
        val percent = latestSpendingPercent

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_set_reminder -> {
                showReminderDialog()
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
                    Toast.makeText(
                        this,
                        getString(R.string.invalid_budget_amount),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                viewModel.setBudget(value)
            }
            .show()
    }

    private fun showReminderDialog() {
        val options = arrayOf("In 1 minute", "In 10 minutes", "In 30 minutes", "In 60 minutes")
        val delayValues = intArrayOf(1, 10, 30, 60)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Reminder")
            .setItems(options) { _, which ->
                scheduleReminderWithPermission(delayValues[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleReminderWithPermission(delayMinutes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                pendingReminderDelayMinutes = delayMinutes
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        ReminderScheduler.scheduleReminder(this, delayMinutes)
        Toast.makeText(this, "Reminder set.", Toast.LENGTH_SHORT).show()
    }
}