package com.arvind.diaryai.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.arvind.diaryai.databinding.ActivityDashboardBinding
import com.arvind.diaryai.util.DateRange
import com.arvind.diaryai.util.PdfExportUtil
import com.arvind.diaryai.util.RangeType
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: DashboardPagerAdapter
    private val rangeLabels = listOf("Daily", "Weekly", "Monthly", "Yearly", "All time", "Custom…")
    private val rangeTypes = listOf(RangeType.DAILY, RangeType.WEEKLY, RangeType.MONTHLY, RangeType.YEARLY, RangeType.ALL_TIME, RangeType.CUSTOM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = DashboardPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = listOf("Dashboard", "Expenses", "Diary")[pos]
        }.attach()

        binding.spRange.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rangeLabels)
        binding.spRange.setSelection(2) // Monthly default, matches sample_output sheet
        applyRange(DateRange.forType(RangeType.MONTHLY))

        binding.spRange.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                if (rangeTypes[pos] == RangeType.CUSTOM) pickCustomRange()
                else applyRange(DateRange.forType(rangeTypes[pos]))
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        })
        binding.btnCustomRange.setOnClickListener { pickCustomRange() }
        binding.btnPrintMonthly.setOnClickListener { printMonthlyPdf() }
    }

    private fun applyRange(range: DateRange) {
        adapter.fragments[0].let { (it as? OverviewFragment)?.updateRange(range) }
        adapter.fragments[1].let { (it as? ExpensesFragment)?.updateRange(range) }
        adapter.fragments[2].let { (it as? DiaryRawFragment)?.updateRange(range) }
    }

    private fun pickCustomRange() {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        DatePickerDialog(this, { _, y1, m1, d1 ->
            val start = fmt.format(GregorianCalendar(y1, m1, d1).time)
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val end = fmt.format(GregorianCalendar(y2, m2, d2).time)
                applyRange(DateRange(start, end))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun printMonthlyPdf() {
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val file = PdfExportUtil(this).exportMonth(monthStr)
        Toast.makeText(this, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
