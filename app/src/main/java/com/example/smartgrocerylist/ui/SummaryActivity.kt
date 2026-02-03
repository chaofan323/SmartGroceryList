package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgrocerylist.R
import com.example.smartgrocerylist.data.GroceryRepository
import java.util.Locale
import android.widget.LinearLayout


class SummaryActivity : AppCompatActivity() {

    private lateinit var tvEstimatedValue: TextView
    private lateinit var tvSpentValue: TextView
    private lateinit var tvRemainingValue: TextView
    private lateinit var pbSpendingProgress: ProgressBar
    private lateinit var tvProgressValue: TextView

    private lateinit var pieChart: PieChartView
    private lateinit var legendContainer: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        updateSummaryUI() // initial load
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

    override fun onResume() {
        super.onResume()
        updateSummaryUI()
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

    private fun updateSummaryUI() {
        // totals
        val estimated = GroceryRepository.getTotalEstimatedCost()
        val spent = GroceryRepository.getTotalSpent()

        // NEW: budget-based remaining/progress
        val budget = GroceryRepository.getBudget(this)
        val remaining = if (budget > 0.0) budget - spent else 0.0

        tvEstimatedValue.text = formatCurrency(estimated)
        tvSpentValue.text = formatCurrency(spent)
        tvRemainingValue.text = formatCurrency(remaining)

        val progressPercent = if (budget > 0.0) {
            ((spent / budget) * 100.0).toInt().coerceAtLeast(0)
        } else {
            0
        }

        pbSpendingProgress.max = 100
        pbSpendingProgress.progress = progressPercent.coerceIn(0, 100)
        tvProgressValue.text = String.format(Locale.US, "%d%%", progressPercent)

        // Pie: Spent by category (purchased items only)
        val spentByCategory = GroceryRepository.getAllItems()
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

