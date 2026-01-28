package com.example.smartgrocerylist.ui

import com.example.smartgrocerylist.data.GroceryItem

sealed class ListRow {
    data class Header(val title: String) : ListRow()
    data class ItemRow(val item: GroceryItem) : ListRow()
}
