package com.example.smartgrocerylist.data

data class GroceryItem(
    val id: Int,
    var name: String,
    var price: Double,
    var category: String,
    var isPurchased: Boolean = false
)
