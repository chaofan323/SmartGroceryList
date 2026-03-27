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

    // Base LiveData from Room/Repository
    val allItems: LiveData<List<GroceryItem>>

    // Phase 1: search / filter state
    private val searchQuery = MutableLiveData("")
    private val selectedCategory = MutableLiveData(CATEGORY_ALL)

    // Phase 1: filtered list
    val filteredItems = MediatorLiveData<List<GroceryItem>>()

    // Phase 2: auto budget calculation
    val currentBudget = MutableLiveData<Double>()
    val totalEstimated = MediatorLiveData<Double>()
    val totalSpent = MediatorLiveData<Double>()
    val remainingBudget = MediatorLiveData<Double>()
    val spendingPercent = MediatorLiveData<Int>()
    val spentByCategory = MediatorLiveData<Map<String, Double>>()

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

        updateFilteredItems()
        updateBudgetMetrics()
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

    // Keep old methods for compatibility
    fun getTotalEstimatedCost(): Double = totalEstimated.value ?: repository.getTotalEstimatedCost()

    fun getTotalSpent(): Double = totalSpent.value ?: repository.getTotalSpent()

    fun getBudget(): Double = currentBudget.value ?: GroceryRepository.getBudget(getApplication())

    fun setBudget(value: Double) {
        GroceryRepository.setBudget(getApplication(), value)
        currentBudget.value = value
    }
}