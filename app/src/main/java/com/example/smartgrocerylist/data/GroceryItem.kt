package com.example.smartgrocerylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    var price: Double,
    var category: String,
    var purchased: Boolean = false
)
