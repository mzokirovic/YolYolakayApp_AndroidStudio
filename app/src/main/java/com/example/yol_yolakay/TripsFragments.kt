package com.example.yol_yolakay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.yol_yolakay.databinding.FragmentTripsBinding
import com.google.android.material.tabs.TabLayoutMediator

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private var adapter: TripsPagerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapterni yaratish
        adapter = TripsPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.adapter = adapter

        // ViewPager xotirasini boshqarish
        binding.viewPager.isSaveEnabled = false

        // Tablarni ulash
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "E'lonlarim"   // Tartibini o'zgartirdim (mantiqan to'g'ri bo'lishi uchun)
                1 -> tab.text = "Band qilingan"
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null
        _binding = null
    }

    // --- ADAPTER KLASSI ---
    class TripsPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            // DIQQAT: Bu yerda endi UNIVERSAL fragmentni ishlatamiz
            return when (position) {
                // 0-tab: Mening e'lonlarim
                0 -> MyTripsListFragment.newInstance("PUBLISHED")

                // 1-tab: Men band qilganlarim
                1 -> MyTripsListFragment.newInstance("BOOKED")

                else -> Fragment()
            }
        }
    }
}
