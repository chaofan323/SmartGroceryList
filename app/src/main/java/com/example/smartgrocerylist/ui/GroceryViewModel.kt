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

    // Search / filter state
    private val searchQuery = MutableLiveData("")
    private val selectedCategory = MutableLiveData(CATEGORY_ALL)

    // Filtered list observed by the UI
    val filteredItems = MediatorLiveData<List<GroceryItem>>()

    init {
        val dao = GroceryDatabase.getInstance(application).groceryDao()
        repository = GroceryRepository(dao)
        allItems = repository.allItems

        filteredItems.addSource(allItems) { updateFilteredItems() }
        filteredItems.addSource(searchQuery) { updateFilteredItems() }
        filteredItems.addSource(selectedCategory) { updateFilteredItems() }

        updateFilteredItems()
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

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        selectedCategory.value = category
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

    fun getTotalEstimatedCost(): Double = repository.getTotalEstimatedCost()

    fun getTotalSpent(): Double = repository.getTotalSpent()

    // Budget (uses SharedPreferences via context)
    fun getBudget(): Double = GroceryRepository.getBudget(getApplication())

    fun setBudget(value: Double) = GroceryRepository.setBudget(getApplication(), value)
}