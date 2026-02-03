package com.example.smartgrocerylist.data

object GroceryRepository {

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
}
