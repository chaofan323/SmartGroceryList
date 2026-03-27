package com.example.smartgrocerylist.data

import androidx.room.*
import androidx.lifecycle.LiveData

@Dao
interface GroceryDao {

    @Query("SELECT * FROM grocery_items")
    fun getAllItems(): LiveData<List<GroceryItem>>

    // Non-LiveData version for one-off reads (e.g. editing a single item)
    @Query("SELECT * FROM grocery_items")
    fun getAllItemsSync(): List<GroceryItem>

    @Query("SELECT * FROM grocery_items WHERE id = :id")
    fun getItemById(id: Int): GroceryItem?

    @Query("SELECT * FROM purchase_history ORDER BY purchasedAt DESC")
    fun getAllPurchaseHistory(): LiveData<List<PurchaseHistory>>

    @Insert
    fun insert(item: GroceryItem): Long

    @Insert
    fun insertPurchaseHistory(entry: PurchaseHistory)

    @Update
    fun update(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT COALESCE(SUM(price), 0.0) FROM grocery_items")
    fun getTotalEstimatedCost(): Double

    @Query("SELECT COALESCE(SUM(price), 0.0) FROM grocery_items WHERE purchased = 1")
    fun getTotalSpent(): Double
}