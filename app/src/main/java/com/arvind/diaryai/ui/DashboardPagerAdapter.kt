package com.arvind.diaryai.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DashboardPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    val fragments = listOf(OverviewFragment(), ExpensesFragment(), DiaryRawFragment())
    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}
