package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.ActivitySearchResultBinding // <-- DIQQAT: Bu nom to'g'ri bo'lishi kerak
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.*

class SearchResultActivity : AppCompatActivity() { // <-- DIQQAT: Class nomi "s" harfisiz

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: TripAdapter
    private val resultList = ArrayList<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Qidiruv natijalari"

        val fromCity = intent.getStringExtra("FROM_CITY") ?: ""
        val toCity = intent.getStringExtra("TO_CITY") ?: ""
        val date = intent.getStringExtra("DATE") ?: ""

        setupRecyclerView()
        searchInFirebase(fromCity, toCity, date)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = TripAdapter(resultList) { trip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("trip", trip)
            startActivity(intent)
        }
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun searchInFirebase(from: String, to: String, date: String) {
        database = FirebaseDatabase.getInstance().getReference("trips")
        binding.tvNoResults.visibility = View.GONE
        binding.rvResults.visibility = View.GONE

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foundTrips = mutableListOf<Trip>()
                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)
                    if (trip != null) {
                        val tripFrom = trip.from?.trim() ?: ""
                        val tripTo = trip.to?.trim() ?: ""
                        val tripDate = trip.date?.trim() ?: ""

                        val isFromMatch = tripFrom.equals(from.trim(), ignoreCase = true)
                        val isToMatch = tripTo.equals(to.trim(), ignoreCase = true)
                        val isDateMatch = if (date.isNotEmpty()) tripDate == date.trim() else true

                        if (isFromMatch && isToMatch && isDateMatch) {
                            foundTrips.add(trip)
                        }
                    }
                }

                if (foundTrips.isEmpty()) {
                    binding.rvResults.visibility = View.GONE
                    binding.tvNoResults.visibility = View.VISIBLE
                } else {
                    binding.rvResults.visibility = View.VISIBLE
                    binding.tvNoResults.visibility = View.GONE
                    adapter.updateList(foundTrips)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Xatolik: ${error.message}")
                Toast.makeText(this@SearchResultActivity, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
