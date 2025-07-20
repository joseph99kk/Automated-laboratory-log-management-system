package com.nuerovent.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.nuerovent.R
import com.nuerovent.adapter.ViewPagerAdapter
import com.nuerovent.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        binding.apply {
            options.setOnClickListener {
                startActivity(Intent(this@DashboardActivity, Options::class.java))
            }
            menu.selectedItemId = R.id.dashboard
            viewPager()
            menu.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.dashboard -> binding.viewPager.currentItem = 0
                    R.id.alerts -> binding.viewPager.currentItem = 1
                    R.id.stats -> binding.viewPager.currentItem = 2
                    R.id.control -> binding.viewPager.currentItem = 3
                }
                true
            }
        }

    }

    private fun ActivityDashboardBinding.viewPager() {/* Set up pages for preview */
        viewPager.offscreenPageLimit = 2
        ViewPagerAdapter(this@DashboardActivity).apply {
            list = ArrayList<String>().apply {
                add(getString(R.string.dashboard))
                add(getString(R.string.alerts))
                add(getString(R.string.stats_word))
                add(getString(R.string.control))
            }
        }.also { it.also { binding.viewPager.adapter = it } }

        /* Switch item selected in the bottom navigation bar using
        the view pager current page*/
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> menu.selectedItemId = R.id.dashboard
                    1 -> menu.selectedItemId = R.id.alerts
                    2 -> menu.selectedItemId = R.id.stats
                    3 -> menu.selectedItemId = R.id.control
                }
            }
        })
    }
}