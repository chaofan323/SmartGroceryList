package com.example.smartgrocerylist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.smartgrocerylist.data.GroceryDatabase
import com.example.smartgrocerylist.data.GroceryItem
import com.example.smartgrocerylist.data.GroceryRepository

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroceryRepository

    // LiveData that the UI observes
    val allItems: LiveData<List<GroceryItem>>

    init {
        val dao = GroceryDatabase.getInstance(application).groceryDao()
        repository = GroceryRepository(dao)
        allItems = repository.allItems
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