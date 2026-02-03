package com.example.smartgrocerylist.data

import android.content.Context

object GroceryRepository {

    /* ---------------- Existing item logic ---------------- */

    private val items = mutableListOf<GroceryItem>()
    private var nextId = 1

    fun getAllItems(): List<GroceryItem> {
        return items.toList()
    }

    fun getItemById(id: Int): GroceryItem? {
        return items.find { it.id == id }
    }

    fun addItem(name: String, price: Double, category: String) {
        val item = GroceryItem(
            id = nextId++,
            name = name,
            price = price,
            category = category
        )
        items.add(item)
    }

    fun updateItem(id: Int, name: String, price: Double, category: String) {
        val item = getItemById(id)
        item?.let {
            it.name = name
            it.price = price
            it.category = category
        }
    }

    @Suppress("unused")
    fun togglePurchased(id: Int) {
        val item = getItemById(id)
        item?.let {
            it.purchased = !it.purchased
        }
    }

    fun setPurchased(id: Int, purchased: Boolean) {
        val item = getItemById(id)
        item?.purchased = purchased
    }

    fun getTotalEstimatedCost(): Double {
        return items.sumOf { it.price }
    }

    fun getTotalSpent(): Double {
        return items
            .filter { it.purchased }
            .sumOf { it.price }
    }

    fun deleteItem(id: Int) {
        items.removeAll { it.id == id }
    }

    /* ---------------- Budget logic (NEW) ---------------- */

    private const val PREFS_NAME = "budget_prefs"
    private const val KEY_BUDGET = "budget_value"

    fun getBudget(context: Context): Double {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return java.lang.Double.longBitsToDouble(
            sp.getLong(KEY_BUDGET, java.lang.Double.doubleToLongBits(0.0))
        )
    }

    fun setBudget(context: Context, value: Double) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putLong(KEY_BUDGET, java.lang.Double.doubleToLongBits(value))
            .apply()
    }

    @Suppress("unused")
    fun getRemainingBudget(context: Context): Double {
        val budget = getBudget(context)
        return budget - getTotalSpent()
    }

    @Suppress("unused")
    fun getBudgetPercentage(context: Context): Int {
        val budget = getBudget(context)
        if (budget <= 0) return 0
        return ((getTotalSpent() / budget) * 100).toInt()
    }
}
