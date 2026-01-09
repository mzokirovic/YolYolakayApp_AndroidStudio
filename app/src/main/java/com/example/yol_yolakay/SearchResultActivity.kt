package com.example.yol_yolakay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.ActivitySearchResultBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.*

class SearchResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var tripAdapter: TripAdapter
    private val tripsList = ArrayList<Trip>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideKeyboard()

        // 1. Intentdan ma'lumotlarni olish
        val fromCity = intent.getStringExtra("FROM_CITY")?.trim() ?: ""
        val toCity = intent.getStringExtra("TO_CITY")?.trim() ?: ""
        val date = intent.getStringExtra("DATE")?.trim() ?: ""
        val seats = intent.getIntExtra("SEATS", 1)

        setupToolbar(fromCity, toCity, date, seats)
        setupRecyclerView()

        // So'rov obyektini yaratish
        val searchRequest = SearchRequest(fromCity, toCity, date, seats)
        loadTripsFromFirebase(searchRequest)
    }

    private fun setupToolbar(from: String, to: String, date: String, seats: Int) {
        val displayDate = if (date.isEmpty()) "Barcha sanalar" else date
        val displayFrom = if (from.isEmpty()) "Barcha joylar" else from
        val displayTo = if (to.isEmpty()) "Barcha joylar" else to

        binding.tvRouteInfo.text = "$displayFrom → $displayTo\n$displayDate • $seats kishi"
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(tripsList) { selectedTrip ->
            val intent = Intent(this, TripDetailsActivity::class.java)
            // GSON O'RNIGA PARCELABLE (Trip modeli @Parcelize bo'lgani uchun)
            intent.putExtra("TRIP_OBJ", selectedTrip)
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = tripAdapter
    }

    private fun loadTripsFromFirebase(request: SearchRequest) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE

        database = FirebaseDatabase.getInstance().getReference("trips")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()

                for (data in snapshot.children) {
                    try {
                        val trip = data.getValue(Trip::class.java)
                        // Har bir tripni bizning filtrga mosligini tekshiramiz
                        if (trip != null && isTripMatch(trip, request)) {
                            tripsList.add(trip)
                        }
                    } catch (e: Exception) {
                        Log.e("QIDIRUV_XATO", "Tripni o'qishda xato: ${e.message}")
                    }
                }

                binding.progressBar.visibility = View.GONE
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SearchResultActivity, "Baza xatosi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // PROFESSIONALLIK: Filtrlash mantiqi endi alohida va sovuqqonlik bilan yozilgan
    private fun isTripMatch(trip: Trip, req: SearchRequest): Boolean {
        val dbFrom = trip.from?.trim() ?: ""
        val dbTo = trip.to?.trim() ?: ""
        val dbDate = trip.date?.trim() ?: ""
        val dbSeats = trip.getSeatsAsInt()

        // Faqat aktiv safarlarni ko'rsatamiz
        val isStatusOk = trip.status != "cancelled" && trip.status != "completed"

        // Filtrlash shartlari
        val isFromMatch = req.from.isEmpty() || dbFrom.contains(req.from, ignoreCase = true)
        val isToMatch = req.to.isEmpty() || dbTo.contains(req.to, ignoreCase = true)
        val isSeatsMatch = dbSeats >= req.seats
        val isDateMatch = req.date.isEmpty() || dbDate.contains(req.date, ignoreCase = true)

        return isFromMatch && isToMatch && isDateMatch && isSeatsMatch && isStatusOk
    }

    private fun updateUI() {
        if (tripsList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
            tripAdapter.notifyDataSetChanged()
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus ?: View(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

// SO'ROV UCHUN YORDAMCHI MODEL
data class SearchRequest(val from: String, val to: String, val date: String, val seats: Int)
