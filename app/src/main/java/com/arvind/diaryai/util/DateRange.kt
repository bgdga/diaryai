package com.arvind.diaryai.util

import java.text.SimpleDateFormat
import java.util.*

enum class RangeType { DAILY, WEEKLY, MONTHLY, YEARLY, ALL_TIME, CUSTOM }

data class DateRange(val start: String, val end: String) {
    fun contains(dateStr: String): Boolean = dateStr in start..end

    companion object {
        private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun forType(type: RangeType, customStart: String? = null, customEnd: String? = null): DateRange {
            val cal = Calendar.getInstance()
            val today = fmt.format(cal.time)
            return when (type) {
                RangeType.DAILY -> DateRange(today, today)
                RangeType.WEEKLY -> {
                    cal.add(Calendar.DAY_OF_YEAR, -6)
                    DateRange(fmt.format(cal.time), today)
                }
                RangeType.MONTHLY -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    DateRange(fmt.format(cal.time), today)
                }
                RangeType.YEARLY -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    DateRange(fmt.format(cal.time), today)
                }
                RangeType.ALL_TIME -> DateRange("0000-01-01", "9999-12-31")
                RangeType.CUSTOM -> DateRange(customStart ?: today, customEnd ?: today)
            }
        }
    }
}
