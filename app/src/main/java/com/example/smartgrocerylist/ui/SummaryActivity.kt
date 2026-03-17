package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.smartgrocerylist.R
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var tvEstimatedValue: TextView
    private lateinit var tvSpentValue: TextView
    private lateinit var tvRemainingValue: TextView
    private lateinit var pbSpendingProgress: ProgressBar
    private lateinit var tvProgressValue: TextView
    private lateinit var pieChart: PieChartView
    private lateinit var legendContainer: LinearLayout

    private lateinit var viewModel: GroceryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        viewModel = ViewModelProvider(this)[GroceryViewModel::class.java]

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()

        // Observe LiveData — summary updates automatically
        viewModel.allItems.observe(this) { items ->
            updateSummaryUI(items)
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
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
    }

    private fun updateSummaryUI(items: List<com.example.smartgrocerylist.data.GroceryItem>) {
        val estimated = items.sumOf { it.price }
        val spent = items.filter { it.purchased }.sumOf { it.price }
        val budget = viewModel.getBudget()
        val remaining = if (budget > 0.0) budget - spent else 0.0

        tvEstimatedValue.text = formatCurrency(estimated)
        tvSpentValue.text = formatCurrency(spent)
        tvRemainingValue.text = formatCurrency(remaining)

        val progressPercent = if (budget > 0.0) {
            ((spent / budget) * 100.0).toInt().coerceAtLeast(0)
        } else 0

        pbSpendingProgress.max = 100
        pbSpendingProgress.progress = progressPercent.coerceIn(0, 100)
        tvProgressValue.text = String.format(Locale.US, "%d%%", progressPercent)

        // Pie chart: spent by category
        val spentByCategory = items
            .filter { it.purchased }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.price } }
            .filterValues { it > 0.0 }

        pieChart.setData(spentByCategory)
        renderLegend(spentByCategory)
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
        data.entries.sortedByDescending { it.value }.forEach { (cat, value) ->
            val tv = TextView(this).apply {
                text = getString(R.string.legend_item, cat, formatCurrency(value))
                textSize = 14f
                setPadding(6, 6, 6, 6)
            }
            legendContainer.addView(tv)
        }
    }
}