package com.nuerovent.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nuerovent.ui.dashboard.ControlFragment
import com.nuerovent.ui.dashboard.HomeFragment
import com.nuerovent.ui.dashboard.StatsFragment
import com.nuerovent.ui.dashboard.AlertsFragment




class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    var list = ArrayList<String>()
    override fun getItemCount(): Int = list.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment.newInstance()
            1 -> AlertsFragment.newInstance()
            2 -> StatsFragment.newInstance()
            else -> ControlFragment.newInstance()
        }
    }
}


