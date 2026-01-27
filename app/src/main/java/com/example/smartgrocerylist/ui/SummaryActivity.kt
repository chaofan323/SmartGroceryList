package com.example.smartgrocerylist.ui

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgrocerylist.R
import com.example.smartgrocerylist.data.GroceryRepository
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private lateinit var tvEstimatedValue: TextView
    private lateinit var tvSpentValue: TextView
    private lateinit var tvRemainingValue: TextView
    private lateinit var pbSpendingProgress: ProgressBar
    private lateinit var tvProgressValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        bindViews()
        updateSummaryUI() // initial load
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
    }

    private fun updateSummaryUI() {
        // compute totals
        val estimated = GroceryRepository.getTotalEstimatedCost()
        val spent = GroceryRepository.getTotalSpent()
        val remaining = estimated - spent

        // Display currency with 2 decimals
        tvEstimatedValue.text = formatCurrency(estimated)
        tvSpentValue.text = formatCurrency(spent)
        tvRemainingValue.text = formatCurrency(remaining)

        // progress = spent / estimated (guard against 0)
        val progressPercent = if (estimated > 0.0) {
            ((spent / estimated) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

        pbSpendingProgress.max = 100
        pbSpendingProgress.progress = progressPercent
        tvProgressValue.text = String.format(Locale.US, "%d%%", progressPercent)
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }
}

