package com.example.smartgrocerylist.ui

fun buildRows(items: List<GroceryItem>): List<ListRow> {
    if (items.isEmpty()) return emptyList()

    val grouped = items.groupBy { it.category }
    val rows = mutableListOf<ListRow>()

    val categoryOrder = listOf("Produce", "Dairy", "Meat", "Snacks", "Other")

    val sortedCategories = grouped.keys.sortedWith(compareBy(
        { categoryOrder.indexOf(it).takeIf { idx -> idx >= 0 } ?: 999 },
        { it }
    ))

    for (cat in sortedCategories) {
        rows.add(ListRow.Header(cat))
        grouped[cat]!!.sortedBy { it.name.lowercase() }
            .forEach { rows.add(ListRow.ItemRow(it)) }
    }
    return rows
}
