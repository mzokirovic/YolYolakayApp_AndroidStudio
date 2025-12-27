package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.ActivitySearchResultBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var adapter: TripAdapter
    private val tripList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Toolbar sozlamalari
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 2. MA'LUMOTNI QABUL QILISH
        val searchFrom = intent.getStringExtra("FROM_CITY")?.trim() ?: ""
        val searchTo = intent.getStringExtra("TO_CITY")?.trim() ?: ""
        val searchDate = intent.getStringExtra("DATE")?.trim() ?: ""

        // Sarlavhani yangilash
        if (searchFrom.isNotEmpty() && searchTo.isNotEmpty()) {
            supportActionBar?.title = "$searchFrom -> $searchTo"
        } else {
            supportActionBar?.title = "Qidiruv natijalari"
        }

        // 3. Adapterni sozlash
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TripAdapter(tripList) { selectedTrip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("TRIP_DATA", selectedTrip)
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter

        // 4. Qidirishni boshlash
        searchTrips(searchFrom, searchTo, searchDate)
    }

    private fun searchTrips(from: String, to: String, date: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        val database = FirebaseDatabase.getInstance().getReference("Trips")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()

                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)

                    if (trip != null) {
                        // --- FILTRLASH MANTIQI ---
                        val tripFrom = trip.from?.trim() ?: ""
                        val tripTo = trip.to?.trim() ?: ""

                        // 1. Shaharni tekshirish (Ichida borligini tekshiramiz)
                        val isFromMatch = if (from.isNotEmpty()) {
                            tripFrom.contains(from, ignoreCase = true)
                        } else true

                        val isToMatch = if (to.isNotEmpty()) {
                            tripTo.contains(to, ignoreCase = true)
                        } else true

                        // 2. Sanani tekshirish (Agar user tanlagan bo'lsa)
                        val isDateMatch = if (date.isNotEmpty()) {
                            trip.date == date
                        } else true

                        // Hozircha faqat shahar va sana bo'yicha
                        if (isFromMatch && isToMatch && isDateMatch) {
                            tripList.add(trip)
                        }
                    }
                }

                // Ekranni yangilash
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE

                if (tripList.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SearchResultActivity, "Internetda xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
