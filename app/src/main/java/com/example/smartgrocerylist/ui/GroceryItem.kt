package com.example.smartgrocerylist.ui

data class GroceryItem(
    val id: Long,
    var name: String,
    var price: Double,
    var category: String,
    var purchased: Boolean = false
)
