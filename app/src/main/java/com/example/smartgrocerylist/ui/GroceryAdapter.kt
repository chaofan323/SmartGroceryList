package com.example.smartgrocerylist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgrocerylist.R
import com.example.smartgrocerylist.data.GroceryItem

class GroceryAdapter(
    private val rows: MutableList<ListRow>,
    private val onItemClick: (GroceryItem) -> Unit,
    private val onPurchasedToggle: (GroceryItem, Boolean) -> Unit,
    private val onDeleteClick: (GroceryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is ListRow.Header -> TYPE_HEADER
            is ListRow.ItemRow -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val v = inflater.inflate(R.layout.row_header, parent, false)
            HeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.row_item, parent, false)
            ItemVH(v)
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ListRow.Header -> (holder as HeaderVH).bind(row.title)
            is ListRow.ItemRow -> (holder as ItemVH).bind(
                row.item,
                onItemClick,
                onPurchasedToggle,
                onDeleteClick
            )
        }
    }

    fun update(newRows: List<ListRow>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.tvHeader)
        fun bind(title: String) { tvHeader.text = title }
    }

    class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val cb: CheckBox = itemView.findViewById(R.id.cbPurchased)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)

        fun bind(
            item: GroceryItem,
            onItemClick: (GroceryItem) -> Unit,
            onPurchasedToggle: (GroceryItem, Boolean) -> Unit,
            onDeleteClick: (GroceryItem) -> Unit
        ) {
            tvName.text = item.name
            tvPrice.text = "$" + String.format("%.2f", item.price)

            cb.setOnCheckedChangeListener(null)
            cb.isChecked = item.purchased
            applyPurchasedStyle(item.purchased)

            cb.setOnCheckedChangeListener { _, isChecked ->
                item.purchased = isChecked
                applyPurchasedStyle(isChecked)
                onPurchasedToggle(item, isChecked)
            }

            // Tap row to edit (later)
            itemView.setOnClickListener { onItemClick(item) }

            // Tap bin to delete
            ivDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun applyPurchasedStyle(purchased: Boolean) {
            tvName.paintFlags = if (purchased) {
                tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val alpha = if (purchased) 0.5f else 1f
            tvName.alpha = alpha
            tvPrice.alpha = alpha
        }
    }
}
