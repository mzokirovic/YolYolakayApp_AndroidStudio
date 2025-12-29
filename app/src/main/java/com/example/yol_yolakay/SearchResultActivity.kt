package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.ActivitySearchResultBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.*
import com.google.gson.Gson

class SearchResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var tripAdapter: TripAdapter
    private val tripsList = ArrayList<Trip>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fromCity = intent.getStringExtra("FROM_CITY") ?: ""
        val toCity = intent.getStringExtra("TO_CITY") ?: ""
        val date = intent.getStringExtra("DATE") ?: ""
        val seats = intent.getIntExtra("SEATS", 1)

        setupToolbar(fromCity, toCity, date, seats)
        setupRecyclerView()
        loadTripsFromFirebase(fromCity, toCity, date, seats)
    }

    private fun setupToolbar(from: String, to: String, date: String, seats: Int) {
        binding.tvRouteInfo.text = "$from → $to\n$date • $seats kishi"
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(tripsList) { selectedTrip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            val gson = Gson()
            intent.putExtra("TRIP_JSON", gson.toJson(selectedTrip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = tripAdapter
    }

    private fun loadTripsFromFirebase(from: String, to: String, date: String, seats: Int) {
        binding.progressBar.visibility = View.VISIBLE

        // E'lonlar saqlanadigan joy ("Trips" yoki "trips" ekanligini tekshiring, odatda 'trips')
        database = FirebaseDatabase.getInstance().getReference("trips")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()

                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)

                    if (trip != null) {
                        // ----------------------------------------------------
                        // FILTRLASH (Sizning Trip.kt ga moslab)
                        // ----------------------------------------------------

                        // 1. Shahar tekshiruvi (Katta-kichik harf farqisiz)
                        // Agar qidiruv bo'sh bo'lsa, hammasini ko'rsataveradi
                        val dbFrom = trip.from ?: ""
                        val dbTo = trip.to ?: ""

                        val isFromMatch = dbFrom.contains(from, ignoreCase = true) || from.isEmpty()
                        val isToMatch = dbTo.contains(to, ignoreCase = true) || to.isEmpty()

                        // 2. Sana tekshiruvi
                        // Agar qidiruvda sana bo'sh bo'lsa yoki bazadagi sana mos kelsa
                        val dbDate = trip.date ?: ""
                        val isDateMatch = if (date.isNotEmpty()) {
                            dbDate.contains(date, ignoreCase = true)
                        } else true

                        // 3. Joylar soni (Trip.kt da 'seats' deb nomlangan)
                        // Bazadagi joylar >= qidirilayotgan joylar
                        val dbSeats = trip.seats ?: 0
                        val hasSeats = dbSeats >= seats

                        if (isFromMatch && isToMatch && isDateMatch && hasSeats) {
                            tripsList.add(trip)
                        }
                    }
                }

                tripAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE

                // Bo'sh holat
                if (tripsList.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyStateLayout.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
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
