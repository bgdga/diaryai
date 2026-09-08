package com.arvind.diaryai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.*
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.FragmentOverviewBinding
import com.arvind.diaryai.util.DateRange

class OverviewFragment : Fragment() {
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: StorageManager
    private var range: DateRange = DateRange.forType(com.arvind.diaryai.util.RangeType.MONTHLY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        storage = StorageManager(requireContext())
        return binding.root
    }

    fun updateRange(newRange: DateRange) {
        range = newRange
        if (view != null) render()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        render()
    }

    private fun render() {
        val expenses = storage.readRows(Schema.FILE_EXPENSES, Schema.EXPENSE_HEADER)
            .filter { range.contains(it["date"] ?: "") }
        val metrics = storage.readRows(Schema.FILE_DIARY_METRICS, Schema.DIARY_METRICS_HEADER)
            .filter { range.contains(it["diary_date"] ?: "") }

        val totalOut = expenses.mapNotNull { it["amount"]?.toDoubleOrNull() }.filter { it < 0 }.sum()
        val totalIn = expenses.mapNotNull { it["amount"]?.toDoubleOrNull() }.filter { it > 0 }.sum()
        binding.tvSummary.text = "Range: ${range.start} to ${range.end}\n" +
                "Spent: ${"%.0f".format(-totalOut)}   Earned: ${"%.0f".format(totalIn)}\n" +
                "Diary entries: ${metrics.size}"

        // Pie: category breakdown of spend
        val byCategory = expenses.filter { (it["amount"]?.toDoubleOrNull() ?: 0.0) < 0 }
            .groupBy { it["category"] ?: "Other" }
            .mapValues { (_, rows) -> -rows.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 } }
        val pieEntries = byCategory.map { PieEntry(it.value.toFloat(), it.key) }
        binding.pieCategory.data = PieData(PieDataSet(pieEntries, "Spend by category"))
        binding.pieCategory.invalidate()

        // Line: mood over time
        val moodEntries = metrics.mapIndexedNotNull { i, row ->
            row["mood"]?.toFloatOrNull()?.let { Entry(i.toFloat(), it) }
        }
        binding.lineMood.data = LineData(LineDataSet(moodEntries, "Mood"))
        binding.lineMood.invalidate()

        // Bar: daily net expense trend
        val byDate = expenses.groupBy { it["date"] ?: "" }
            .toSortedMap()
            .mapValues { (_, rows) -> rows.sumOf { it["amount"]?.toDoubleOrNull() ?: 0.0 } }
        val barEntries = byDate.values.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
        binding.barExpenseTrend.data = BarData(BarDataSet(barEntries, "Net daily flow"))
        binding.barExpenseTrend.invalidate()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
