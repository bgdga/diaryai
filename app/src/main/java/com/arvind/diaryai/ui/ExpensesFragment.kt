package com.arvind.diaryai.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.FragmentExpensesBinding
import com.arvind.diaryai.util.DateRange

class ExpensesFragment : Fragment() {
    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: StorageManager
    private var range: DateRange = DateRange.forType(com.arvind.diaryai.util.RangeType.MONTHLY)
    private var dateKeysForChart: List<String> = emptyList()
    private var rowsForChart: List<Map<String, String>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        storage = StorageManager(requireContext())
        binding.rvExpenseCards.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    fun updateRange(newRange: DateRange) {
        range = newRange
        if (view != null) render()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { render() }

    private fun render() {
        val expenses = storage.readRows(Schema.FILE_EXPENSES, Schema.EXPENSE_HEADER)
            .filter { range.contains(it["date"] ?: "") }
        rowsForChart = expenses

        // Bar chart: total spend per date (tap a bar -> popup full category breakdown for that date)
        val byDate = expenses.groupBy { it["date"] ?: "" }.toSortedMap()
        dateKeysForChart = byDate.keys.toList()
        val entries = byDate.values.mapIndexed { i, rows ->
            BarEntry(i.toFloat(), (-rows.filter { (it["amount"]?.toDoubleOrNull() ?: 0.0) < 0 }
                .sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 }).toFloat())
        }
        binding.barCategoryByDate.data = BarData(BarDataSet(entries, "Spend by date (tap a bar)"))
        binding.barCategoryByDate.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val idx = e?.x?.toInt() ?: return
                val date = dateKeysForChart.getOrNull(idx) ?: return
                showBreakdownPopup(date)
            }
            override fun onNothingSelected() {}
        })
        binding.barCategoryByDate.invalidate()

        // Raw cards grouped by year_month, newest first
        binding.rvExpenseCards.adapter = GroupedCardAdapter(expenses, "date") { row ->
            val amt = row["amount"] ?: "0"
            "${row["date"]}  •  ${row["category"]}  •  $amt" to
                "${row["cash_online"]} · ${row["whose_exp"]} · ${row["comments"]}"
        }
    }

    private fun showBreakdownPopup(date: String) {
        val dayRows = rowsForChart.filter { it["date"] == date }
        val byCategory = dayRows.groupBy { it["category"] ?: "Other" }
            .mapValues { (_, rows) -> rows.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 } }
        val body = byCategory.entries.joinToString("\n") { "${it.key}: ${"%.0f".format(it.value)}" }
        AlertDialog.Builder(requireContext())
            .setTitle("Breakdown for $date")
            .setMessage(body.ifBlank { "No entries" })
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
