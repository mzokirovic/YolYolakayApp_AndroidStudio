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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TripsFragment : Fragment() {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!
    private lateinit var tripAdapter: TripAdapter

    // Rasmga moslab ikkita ro'yxat:
    private val bookedTrips = ArrayList<Trip>() // Men yo'lovchi bo'lganlar
    private val publishedTrips = ArrayList<Trip>() // Men haydovchi bo'lganlar (Mening e'lonlarim)

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
        loadMyTrips()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTrips.layoutManager = LinearLayoutManager(context)
        // Adapterga bosilganda nima bo'lishini aytamiz
        tripAdapter = TripAdapter(arrayListOf()) { trip ->
            val intent = Intent(context, TripDetailsActivity::class.java)
            val gson = com.google.gson.Gson()
            intent.putExtra("TRIP_JSON", gson.toJson(trip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }
        binding.recyclerViewTrips.adapter = tripAdapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> updateList(bookedTrips)     // Band qilingan (Passenger)
                    1 -> updateList(publishedTrips)  // E'lon qilingan (Driver)
                }
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

    private fun filterList(query: String) {
        val currentTab = binding.tabLayout.selectedTabPosition
        val sourceList = if (currentTab == 0) bookedTrips else publishedTrips

        if (query.isEmpty()) {
            updateList(sourceList)
        } else {
            val filtered = sourceList.filter {
                (it.from?.contains(query, true) == true) ||
                        (it.to?.contains(query, true) == true)
            }
            tripAdapter.updateList(filtered)
        }
    }

    private fun loadMyTrips() {
        val myId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("trips")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookedTrips.clear()
                publishedTrips.clear()

                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)
                    if (trip != null) {

                        // 1. Agar men HAYDOVCHI bo'lsam -> Published ro'yxatiga
                        if (trip.userId == myId) {
                            publishedTrips.add(trip)
                        }
                        // 2. Agar men YO'LOVCHI bo'lsam (bookedUsers ichida bor bo'lsam) -> Booked ro'yxatiga
                        else if (data.child("bookedUsers").hasChild(myId)) {
                            bookedTrips.add(trip)
                        }
                    }
                }

                // Yangilari tepada tursin
                bookedTrips.reverse()
                publishedTrips.reverse()

                // Hozirgi tabni yangilash
                val currentTab = binding.tabLayout.selectedTabPosition
                if (currentTab == 0) updateList(bookedTrips) else updateList(publishedTrips)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateList(list: List<Trip>) {
        if (_binding == null) return
        tripAdapter.updateList(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
