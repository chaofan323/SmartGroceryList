package com.example.smartgrocerylist.data

object GroceryRepository {

    private val items = mutableListOf<GroceryItem>()
    private var nextId = 1

//    fun getAllItems(): List<GroceryItem> {
//        return items
//    }
    fun getAllItems(): List<com.example.smartgrocerylist.data.GroceryItem> {
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
            it.isPurchased = !it.isPurchased
        }
    }

    fun getTotalEstimatedCost(): Double {
        return items.sumOf { it.price }
    }

    fun getTotalSpent(): Double {
        return items
            .filter { it.isPurchased }
            .sumOf { it.price }
    }

    fun deleteItem(id: Int) {
        items.removeAll { it.id == id }
    }
}
