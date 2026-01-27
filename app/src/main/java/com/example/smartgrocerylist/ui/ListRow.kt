package com.example.smartgrocerylist.ui

sealed class ListRow {
    data class Header(val title: String) : ListRow()
    data class ItemRow(val item: GroceryItem) : ListRow()
}
