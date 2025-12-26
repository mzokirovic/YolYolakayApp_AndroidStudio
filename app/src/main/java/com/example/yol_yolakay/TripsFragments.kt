package com.example.yol_yolakay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.yol_yolakay.databinding.FragmentTripsBinding
import com.google.android.material.tabs.TabLayoutMediator

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager va Adapterni ulash
        val adapter = TripsPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // TabLayout va ViewPager ni birlashtirish (Sarlavhalar shu yerda beriladi)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Band qilingan"
                1 -> tab.text = "Mening e'lonlarim"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // PagerAdapter klassi (FragmentStateAdapter dan meros oladi)
    class TripsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BookedTripsFragment()     // Agar bu qizil bo'lsa, pastdagi eslatmani o'qing
                1 -> PublishedTripsFragment()  // Agar bu qizil bo'lsa, pastdagi eslatmani o'qing
                else -> BookedTripsFragment()
            }
        }
    }
}
