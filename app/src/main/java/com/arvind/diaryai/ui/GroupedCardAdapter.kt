package com.arvind.diaryai.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arvind.diaryai.R
import com.arvind.diaryai.data.StorageManager

sealed class CardRow {
    data class Header(val label: String) : CardRow()
    data class Item(val title: String, val body: String, val onClick: (() -> Unit)? = null) : CardRow()
}

/**
 * Groups arbitrary rows by "yyyy-MM" (year_month) with a header, newest month first.
 * Used by both the Expenses raw list and the Diary raw list per the spec (Tab 2 / Tab 3).
 */
class GroupedCardAdapter(rows: List<Map<String, String>>, dateKey: String, private val toCard: (Map<String, String>) -> Pair<String, String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val flat: List<CardRow> = buildList {
        rows.sortedByDescending { it[dateKey] }
            .groupBy { StorageManager.yearMonth(it[dateKey] ?: "") }
            .toSortedMap(compareByDescending { it })
            .forEach { (month, monthRows) ->
                add(CardRow.Header(month))
                monthRows.forEach { row ->
                    val (title, body) = toCard(row)
                    add(CardRow.Item(title, body))
                }
            }
    }

    override fun getItemViewType(position: Int) = if (flat[position] is CardRow.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderVH(inflater.inflate(R.layout.item_month_header, parent, false))
        } else {
            ItemVH(inflater.inflate(R.layout.item_transaction_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = flat[position]) {
            is CardRow.Header -> (holder as HeaderVH).bind(row.label)
            is CardRow.Item -> (holder as ItemVH).bind(row)
        }
    }

    override fun getItemCount() = flat.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(label: String) { (itemView as TextView).text = label }
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: CardRow.Item) {
            itemView.findViewById<TextView>(R.id.tvCardTitle).text = item.title
            itemView.findViewById<TextView>(R.id.tvCardBody).text = item.body
            itemView.setOnClickListener { item.onClick?.invoke() }
        }
    }
}
