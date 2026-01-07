package com.example.yol_yolakay.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.TripDetailsActivity
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.FragmentTripsBinding
import com.example.yol_yolakay.model.Trip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private lateinit var tripAdapter: TripAdapter

    // Ikkita alohida ro'yxat:
    // 1. Men bron qilganim (Yo'lovchi sifatida)
    private val bookedTrips = ArrayList<Trip>()
    // 2. Men e'lon qilganim (Haydovchi sifatida)
    private val publishedTrips = ArrayList<Trip>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupSearch()

        // Asosiy ish shu yerda: Ma'lumotlarni yuklash
        loadMyTrips()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTrips.layoutManager = LinearLayoutManager(context)

        // Adapter bosilganda TripDetailsActivity ga o'tamiz
        tripAdapter = TripAdapter(arrayListOf()) { trip ->
            val intent = Intent(context, TripDetailsActivity::class.java)
            val gson = com.google.gson.Gson()
            // Trip obyektini to'liq JSON qilib uzatamiz
            intent.putExtra("TRIP_JSON", gson.toJson(trip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }

        binding.recyclerViewTrips.adapter = tripAdapter
    }

    private fun setupTabs() {
        // Tab o'zgarganda ro'yxatni yangilash
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateList(bookedTrips)     // "Band qilingan"
                    1 -> updateList(publishedTrips)  // "E'lon qilingan"
                }
                // Tab o'zgarganda qidiruv maydonini tozalab qo'yish yaxshi amaliyot
                binding.etSearch.setText("")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Qidiruv mantig'i
    private fun filterList(query: String) {
        val currentTab = binding.tabLayout.selectedTabPosition
        val sourceList = if (currentTab == 0) bookedTrips else publishedTrips

        if (query.isEmpty()) {
            updateList(sourceList)
        } else {
            // Qayerdan (From) yoki Qayerga (To) bo'yicha qidirish
            val filtered = sourceList.filter {
                (it.from?.contains(query, true) == true) ||
                        (it.to?.contains(query, true) == true)
            }
            // Filtrlangan ro'yxatni adapterga beramiz (lekin EmptyState tekshiruvi adapter ichida bo'lmagani uchun updateList ni ishlatmaymiz)
            tripAdapter.updateList(filtered)

            // UI ni yangilash
            if (filtered.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerViewTrips.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.recyclerViewTrips.visibility = View.VISIBLE
            }
        }
    }

    private fun loadMyTrips() {
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("trips")

        // Real vaqtda o'zgarishlarni tinglash
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ro'yxatlarni tozalaymiz (dublikat bo'lmasligi uchun)
                bookedTrips.clear()
                publishedTrips.clear()

                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)
                    if (trip != null) {

                        // 1. MEN HAYDOVCHIMAN (E'lon egasiman)
                        if (trip.userId == myId) {
                            publishedTrips.add(trip)
                        }

                        // 2. MEN YO'LOVCHIMAN (Bron qilganman yoki so'rov yuborganman)
                        else {
                            val isBooked = data.child("bookedUsers").hasChild(myId)
                            val isRequested = data.child("requests").hasChild(myId)

                            if (isBooked || isRequested) {
                                bookedTrips.add(trip)
                            }
                        }
                    }
                }

                // Ro'yxatlarni teskari qilamiz (Yangilari tepada turishi uchun)
                bookedTrips.reverse()
                publishedTrips.reverse()

                // Hozirgi tanlangan Tabga qarab ekranni yangilaymiz
                val currentTab = binding.tabLayout.selectedTabPosition
                if (currentTab == 0) {
                    updateList(bookedTrips)
                } else {
                    updateList(publishedTrips)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateList(list: List<Trip>) {
        if (_binding == null) return

        tripAdapter.updateList(list)

        // Agar ro'yxat bo'sh bo'lsa -> Rasm chiqaramiz
        if (list.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewTrips.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerViewTrips.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
