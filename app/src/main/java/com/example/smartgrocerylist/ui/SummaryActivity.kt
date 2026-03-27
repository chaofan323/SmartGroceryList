package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var tvEstimatedValue: TextView
    private lateinit var tvSpentValue: TextView
    private lateinit var tvRemainingValue: TextView
    private lateinit var pbSpendingProgress: LinearProgressIndicator
    private lateinit var tvProgressValue: TextView
    private lateinit var pieChart: PieChartView
    private lateinit var legendContainer: LinearLayout

    private lateinit var rvSuggestions: RecyclerView
    private lateinit var suggestionAdapter: SimpleStringAdapter

    private lateinit var viewModel: GroceryViewModel

    private var latestEstimated: Double = 0.0
    private var latestSpent: Double = 0.0
    private var latestRemaining: Double = 0.0
    private var latestProgressPercent: Int = 0
    private var latestSpentByCategory: Map<String, Double> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        viewModel = ViewModelProvider(this)[GroceryViewModel::class.java]

        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        setupSuggestionsList()
        observeSummaryData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadBudget()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun bindViews() {
        tvEstimatedValue = findViewById(R.id.tvEstimatedValue)
        tvSpentValue = findViewById(R.id.tvSpentValue)
        tvRemainingValue = findViewById(R.id.tvRemainingValue)
        pbSpendingProgress = findViewById(R.id.pbSpendingProgress)
        tvProgressValue = findViewById(R.id.tvProgressValue)
        pieChart = findViewById(R.id.pieChart)
        legendContainer = findViewById(R.id.legendContainer)
        rvSuggestions = findViewById(R.id.rvSuggestions)
    }

    private fun setupSuggestionsList() {
        suggestionAdapter = SimpleStringAdapter(emptyList())
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = suggestionAdapter
        rvSuggestions.isNestedScrollingEnabled = false
    }

    private fun observeSummaryData() {
        viewModel.totalEstimated.observe(this) { value ->
            latestEstimated = value ?: 0.0
            renderTotals()
        }

        viewModel.totalSpent.observe(this) { value ->
            latestSpent = value ?: 0.0
            renderTotals()
        }

        viewModel.remainingBudget.observe(this) { value ->
            latestRemaining = value ?: 0.0
            renderTotals()
        }

        viewModel.spendingPercent.observe(this) { value ->
            latestProgressPercent = value ?: 0
            renderTotals()
        }

        viewModel.spentByCategory.observe(this) { value ->
            latestSpentByCategory = value ?: emptyMap()
            renderChart()
        }

        viewModel.smartSuggestions.observe(this) { suggestions ->
            suggestionAdapter.update(suggestions ?: emptyList())
        }
    }

    private fun renderTotals() {
        tvEstimatedValue.text = formatCurrency(latestEstimated)
        tvSpentValue.text = formatCurrency(latestSpent)
        tvRemainingValue.text = formatCurrency(latestRemaining)

        pbSpendingProgress.max = 100
        pbSpendingProgress.progress = latestProgressPercent.coerceIn(0, 100)
        tvProgressValue.text = String.format(Locale.US, "%d%%", latestProgressPercent)
    }

    private fun renderChart() {
        pieChart.setData(latestSpentByCategory)
        renderLegend(latestSpentByCategory)
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun renderLegend(data: Map<String, Double>) {
        legendContainer.removeAllViews()

        if (data.isEmpty()) {
            val tv = TextView(this).apply {
                text = getString(R.string.legend_empty)
                alpha = 0.75f
                textSize = 14f
                setPadding(6, 6, 6, 6)
            }
            legendContainer.addView(tv)
            return
        }

        data.entries
            .sortedByDescending { it.value }
            .forEach { (category, value) ->
                val tv = TextView(this).apply {
                    text = getString(R.string.legend_item, category, formatCurrency(value))
                    textSize = 14f
                    setPadding(6, 6, 6, 6)
                }
                legendContainer.addView(tv)
            }
    }
}