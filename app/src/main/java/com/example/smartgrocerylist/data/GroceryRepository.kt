package com.example.smartgrocerylist.data

import android.content.Context
import androidx.lifecycle.LiveData

class GroceryRepository(private val dao: GroceryDao) {

    // LiveData - UI observes this and auto-updates
    val allItems: LiveData<List<GroceryItem>> = dao.getAllItems()
    val purchaseHistory: LiveData<List<PurchaseHistory>> = dao.getAllPurchaseHistory()

    fun getAllItemsSync(): List<GroceryItem> = dao.getAllItemsSync()

    fun getItemById(id: Int): GroceryItem? = dao.getItemById(id)

    fun addItem(name: String, price: Double, category: String) {
        val item = GroceryItem(name = name, price = price, category = category)
        dao.insert(item)
    }

    fun updateItem(id: Int, name: String, price: Double, category: String) {
        val existing = dao.getItemById(id) ?: return
        val updated = existing.copy(name = name, price = price, category = category)
        dao.update(updated)
    }

    fun setPurchased(id: Int, purchased: Boolean) {
        val existing = dao.getItemById(id) ?: return
        val wasPurchased = existing.purchased
        val updated = existing.copy(purchased = purchased)
        dao.update(updated)

        if (!wasPurchased && purchased) {
            dao.insertPurchaseHistory(
                PurchaseHistory(
                    itemName = existing.name,
                    category = existing.category
                )
            )
        }
    }

    fun deleteItem(id: Int) = dao.deleteById(id)

    fun getTotalEstimatedCost(): Double = dao.getTotalEstimatedCost()

    fun getTotalSpent(): Double = dao.getTotalSpent()

    // Budget stays in SharedPreferences
    companion object {
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
    }
}