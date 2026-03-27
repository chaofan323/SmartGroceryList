package com.example.smartgrocerylist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemName: String,
    val category: String,
    val purchasedAt: Long = System.currentTimeMillis()
)