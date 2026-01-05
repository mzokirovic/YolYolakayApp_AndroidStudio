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
import android.content.Context
import android.view.inputmethod.InputMethodManager

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

        // Intentdan ma'lumotlarni xavfsiz olish
        val fromCity = intent.getStringExtra("FROM_CITY")?.trim() ?: ""
        val toCity = intent.getStringExtra("TO_CITY")?.trim() ?: ""
        val date = intent.getStringExtra("DATE")?.trim() ?: ""
        // Crash bo'lmasligi uchun getIntExtra ishlatamiz
        val seats = intent.getIntExtra("SEATS", 1)

        Log.d("QIDIRUV", "Qidirilmoqda: From='$fromCity', To='$toCity', Date='$date', Seats=$seats")

        setupToolbar(fromCity, toCity, date, seats)
        setupRecyclerView()
        loadTripsFromFirebase(fromCity, toCity, date, seats)
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
            val gson = Gson()
            intent.putExtra("TRIP_JSON", gson.toJson(selectedTrip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = tripAdapter
    }

    private fun loadTripsFromFirebase(reqFrom: String, reqTo: String, reqDate: String, reqSeats: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE

        // Qidiruv ma'lumotlarini logga yozamiz
        Log.d("QIDIRUV", "START: Qayerdan='$reqFrom', Qayerga='$reqTo', Sana='$reqDate', Joy=$reqSeats")

        database = FirebaseDatabase.getInstance().getReference("trips")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()

                if (!snapshot.exists()) {
                    Log.e("QIDIRUV", "Bazada 'trips' papkasi bo'sh!")
                }

                for (data in snapshot.children) {
                    try {
                        val trip = data.getValue(Trip::class.java)

                        if (trip != null) {
                            // 1. Bazadagi ma'lumotlarni olamiz
                            val dbFrom = trip.from?.trim() ?: ""
                            val dbTo = trip.to?.trim() ?: ""
                            val dbDate = trip.date?.trim() ?: ""
                            val dbSeats = trip.getSeatsAsInt()

                            // Status tekshiruvi (faqat bekor qilinganlarini chiqarmaymiz)
                            val isStatusOk = trip.status != "cancelled" && trip.status != "completed"

                            // 2. SOLISHTIRISH (Logika)

                            // Shahar (ichida qatnashsa bo'ldi, masalan "Toshkent" deb yozsa "Toshkent shahar" chiqadi)
                            val isFromMatch = if (reqFrom.isNotEmpty()) dbFrom.contains(reqFrom, ignoreCase = true) else true
                            val isToMatch = if (reqTo.isNotEmpty()) dbTo.contains(reqTo, ignoreCase = true) else true

                            // Joy
                            val isSeatsMatch = dbSeats >= reqSeats

                            // Sana (Eng nozik joyi shu!)
                            // Agar sana tanlanmagan bo'lsa (bo'sh), hammasini chiqaradi.
                            // Agar sana bo'lsa, ichida borligini tekshiradi.
                            val isDateMatch = if (reqDate.isNotEmpty()) {
                                dbDate.contains(reqDate, ignoreCase = true)
                            } else true

                            // 3. LOG VA NATIJA
                            if (isFromMatch && isToMatch && isDateMatch && isSeatsMatch && isStatusOk) {
                                tripsList.add(trip)
                                Log.d("QIDIRUV_MATCH", "✅ MOS KELDI: ID=${trip.id} ($dbFrom -> $dbTo)")
                            } else {
                                // Nega mos kelmaganini ko'rsatamiz
                                Log.w("QIDIRUV_FAIL", "❌ O'TMADI: ID=${trip.id}. Sabablar: From=$isFromMatch, To=$isToMatch, Date=$isDateMatch, Seats=$isSeatsMatch, Status=$isStatusOk")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("QIDIRUV_XATO", "Tripni o'qishda xato: ${e.message}")
                    }
                }

                binding.progressBar.visibility = View.GONE

                if (tripsList.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    Log.d("QIDIRUV", "Jami topildi: 0 ta")
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.emptyStateLayout.visibility = View.GONE
                    tripAdapter.notifyDataSetChanged()
                    Log.d("QIDIRUV", "Jami topildi: ${tripsList.size} ta")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@SearchResultActivity, "Baza xatosi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hideKeyboard() {
        val view = this.currentFocus ?: View(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


}
