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

        // 1. MA'LUMOTNI QABUL QILISH (HomeFragment dagi kalit so'zlarga mosladik)
        val searchFrom = intent.getStringExtra("FROM_CITY")?.trim() ?: ""
        val searchTo = intent.getStringExtra("TO_CITY")?.trim() ?: ""
        val searchDate = intent.getStringExtra("DATE")?.trim() ?: ""

        // Sarlavhani yangilash
        if (searchFrom.isNotEmpty() && searchTo.isNotEmpty()) {
            binding.tvTitle.text = "$searchFrom -> $searchTo"
        } else {
            binding.tvTitle.text = "Qidiruv natijalari"
        }

        // 2. Adapterni sozlash
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TripAdapter(tripList) { selectedTrip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("TRIP_DATA", selectedTrip)
            // Qidiruvdan o'tilganda "Preview" emas, oddiy ko'rish rejimi bo'ladi
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        // 3. Qidirishni boshlash
        searchTrips(searchFrom, searchTo, searchDate)
    }

    private fun searchTrips(from: String, to: String, date: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        val database = FirebaseDatabase.getInstance().getReference("Trips")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()

                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)

                    if (trip != null) {
                        // --- FILTRLASH MANTIQI (YUMSHATILGAN) ---

                        // 1. Shaharlarni tekshirish (Contains ishlatamiz)
                        val tripFrom = trip.from?.trim() ?: ""
                        val tripTo = trip.to?.trim() ?: ""

                        // Agar qidiruv so'zi bo'sh bo'lsa, hamma narsani ko'rsataversin (true)
                        // Agar yozilgan bo'lsa, ichida bor-yo'qligini tekshirsin
                        val isFromMatch = if (from.isNotEmpty()) {
                            tripFrom.contains(from, ignoreCase = true)
                        } else true

                        val isToMatch = if (to.isNotEmpty()) {
                            tripTo.contains(to, ignoreCase = true)
                        } else true

                        // 2. Sanani tekshirish
                        // Agar user sana tanlamagan bo'lsa, hammasi chiqsin.
                        // Agar tanlagan bo'lsa, aniq tenglik kerak.
                        val isDateMatch = if (date.isNotEmpty()) {
                            trip.date == date
                        } else true

                        // Hozircha faqat shahar va sana bo'yicha qo'shamiz
                        if (isFromMatch && isToMatch && isDateMatch) {
                            tripList.add(trip)
                        }
                    }
                }

                // Adapterga o'zgarishni xabar qilish
                adapter.notifyDataSetChanged() // Yoki adapter.updateList(tripList) agar funksiya bo'lsa

                binding.progressBar.visibility = View.GONE

                // Agar ro'yxat bo'sh bo'lsa
                if (tripList.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SearchResultActivity, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
