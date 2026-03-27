package com.example.smartgrocerylist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.smartgrocerylist.data.GroceryDatabase
import com.example.smartgrocerylist.data.GroceryItem
import com.example.smartgrocerylist.data.GroceryRepository

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val CATEGORY_ALL = "All"
    }

    private val repository: GroceryRepository

    val allItems: LiveData<List<GroceryItem>>

    private val searchQuery = MutableLiveData("")
    private val selectedCategory = MutableLiveData(CATEGORY_ALL)

    val filteredItems = MediatorLiveData<List<GroceryItem>>()

    val currentBudget = MutableLiveData<Double>()
    val totalEstimated = MediatorLiveData<Double>()
    val totalSpent = MediatorLiveData<Double>()
    val remainingBudget = MediatorLiveData<Double>()
    val spendingPercent = MediatorLiveData<Int>()
    val spentByCategory = MediatorLiveData<Map<String, Double>>()

    val smartSuggestions = MediatorLiveData<List<String>>()

    init {
        val dao = GroceryDatabase.getInstance(application).groceryDao()
        repository = GroceryRepository(dao)
        allItems = repository.allItems

        currentBudget.value = GroceryRepository.getBudget(getApplication())

        filteredItems.addSource(allItems) { updateFilteredItems() }
        filteredItems.addSource(searchQuery) { updateFilteredItems() }
        filteredItems.addSource(selectedCategory) { updateFilteredItems() }

        totalEstimated.addSource(allItems) { updateBudgetMetrics() }
        totalEstimated.addSource(currentBudget) { updateBudgetMetrics() }

        totalSpent.addSource(allItems) { updateBudgetMetrics() }
        totalSpent.addSource(currentBudget) { updateBudgetMetrics() }

        remainingBudget.addSource(allItems) { updateBudgetMetrics() }
        remainingBudget.addSource(currentBudget) { updateBudgetMetrics() }

        spendingPercent.addSource(allItems) { updateBudgetMetrics() }
        spendingPercent.addSource(currentBudget) { updateBudgetMetrics() }

        spentByCategory.addSource(allItems) { updateBudgetMetrics() }
        spentByCategory.addSource(currentBudget) { updateBudgetMetrics() }

        smartSuggestions.addSource(allItems) { updateSmartSuggestions() }
        smartSuggestions.addSource(currentBudget) { updateSmartSuggestions() }

        updateFilteredItems()
        updateBudgetMetrics()
        updateSmartSuggestions()
    }

    private fun updateFilteredItems() {
        val items = allItems.value ?: emptyList()
        val query = searchQuery.value.orEmpty().trim().lowercase()
        val category = selectedCategory.value ?: CATEGORY_ALL

        val result = items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)

            val matchesCategory = category == CATEGORY_ALL || item.category == category

            matchesQuery && matchesCategory
        }

        filteredItems.value = result
    }

    private fun updateBudgetMetrics() {
        val items = allItems.value ?: emptyList()
        val budget = currentBudget.value ?: 0.0

        val estimated = items.sumOf { it.price }
        val spent = items.filter { it.purchased }.sumOf { it.price }
        val remaining = if (budget > 0.0) budget - spent else 0.0
        val percent = if (budget > 0.0) {
            ((spent / budget) * 100.0).toInt().coerceIn(0, 9999)
        } else {
            0
        }

        val categoryMap = items
            .filter { it.purchased }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.price } }
            .filterValues { it > 0.0 }

        totalEstimated.value = estimated
        totalSpent.value = spent
        remainingBudget.value = remaining
        spendingPercent.value = percent
        spentByCategory.value = categoryMap
    }

    private fun updateSmartSuggestions() {
        val items = allItems.value ?: emptyList()
        val budget = currentBudget.value ?: 0.0
        val spent = items.filter { it.purchased }.sumOf { it.price }
        val estimated = items.sumOf { it.price }
        val percent = if (budget > 0.0) ((spent / budget) * 100.0).toInt() else 0

        val suggestions = mutableListOf<String>()

        if (items.isEmpty()) {
            suggestions.add("Add a few grocery items first to receive smart suggestions.")
            smartSuggestions.value = suggestions
            return
        }

        if (budget <= 0.0) {
            suggestions.add("Set a budget to get better spending guidance.")
        } else {
            when {
                spent > budget -> {
                    suggestions.add("You are already over budget. Consider removing one or two non-essential items.")
                }
                percent >= 80 -> {
                    suggestions.add("You are close to your budget. Focus on essential items only.")
                }
                percent in 1..49 -> {
                    suggestions.add("Your spending is under control so far. You still have room in your budget.")
                }
            }

            if (estimated > budget) {
                suggestions.add("Your full list costs more than your budget. Review unpurchased items before checkout.")
            }
        }

        val unpurchasedCount = items.count { !it.purchased }
        if (unpurchasedCount > 0) {
            suggestions.add("You still have $unpurchasedCount unpurchased item(s) on your list.")
        } else {
            suggestions.add("All items are marked as purchased. You can start planning your next shopping trip.")
        }

        val categories = items.map { it.category }.toSet()
        if (!categories.contains("Produce")) {
            suggestions.add("Consider adding some Produce items for a more balanced grocery list.")
        }

        if (!categories.contains("Dairy")) {
            suggestions.add("You do not have any Dairy items. Check whether you still need milk, cheese, or yogurt.")
        }

        val snackCount = items.count { it.category == "Snacks" }
        val produceCount = items.count { it.category == "Produce" }
        if (snackCount >= 2 && produceCount == 0) {
            suggestions.add("You have several Snacks items. Consider adding fruits or vegetables as well.")
        }

        val expensiveUnpurchased = items
            .filter { !it.purchased }
            .maxByOrNull { it.price }

        if (expensiveUnpurchased != null && expensiveUnpurchased.price >= 15.0) {
            suggestions.add("Your most expensive unpurchased item is ${expensiveUnpurchased.name}. Double-check whether you still need it.")
        }

        smartSuggestions.value = suggestions.distinct().take(4)
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        selectedCategory.value = category
    }

    fun reloadBudget() {
        currentBudget.value = GroceryRepository.getBudget(getApplication())
    }

    fun getItemById(id: Int): GroceryItem? = repository.getItemById(id)

    fun addItem(name: String, price: Double, category: String) {
        repository.addItem(name, price, category)
    }

    fun updateItem(id: Int, name: String, price: Double, category: String) {
        repository.updateItem(id, name, price, category)
    }

    fun setPurchased(id: Int, purchased: Boolean) {
        repository.setPurchased(id, purchased)
    }

    fun deleteItem(id: Int) {
        repository.deleteItem(id)
    }

    fun getTotalEstimatedCost(): Double =
        totalEstimated.value ?: repository.getTotalEstimatedCost()

    fun getTotalSpent(): Double =
        totalSpent.value ?: repository.getTotalSpent()

    fun getBudget(): Double =
        currentBudget.value ?: GroceryRepository.getBudget(getApplication())

    fun setBudget(value: Double) {
        GroceryRepository.setBudget(getApplication(), value)
        currentBudget.value = value
    }
}