package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
// Agar papka nomi "adapters" bo'lsa, 's' harfini qo'shing
import com.example.yol_yolakay.databinding.FragmentTripsListBinding
import com.example.yol_yolakay.model.Trip
import com.example.yol_yolakay.TripDetailsActivity
import com.example.yol_yolakay.adapter.TripAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BookedTripsFragment : Fragment() {

    private var _binding: FragmentTripsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripAdapter
    private val bookedList = mutableListOf<Trip>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTripsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewList.layoutManager = LinearLayoutManager(context)
        adapter = TripAdapter(bookedList) { trip ->
            val intent = Intent(context, TripDetailsActivity::class.java)
            val gson = com.google.gson.Gson()
            intent.putExtra("TRIP_JSON", gson.toJson(trip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }
        binding.recyclerViewList.adapter = adapter

        loadBookedTrips()
    }

    private fun loadBookedTrips() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 1. Faqat haqiqiy User ID ni ishlatamiz (AddTrip va TripDetails bilan bir xil bo'lishi uchun)
        if (currentUser == null) {
            binding.progressBarList.visibility = View.GONE
            binding.tvEmpty.text = "Iltimos, avval tizimga kiring"
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val myId = currentUser.uid

        binding.progressBarList.visibility = View.VISIBLE
        val database = FirebaseDatabase.getInstance()

        // 2. User band qilgan safarlar IDlarini olamiz
        val userBookingsRef = database.getReference("Users").child(myId).child("bookedTrips")

        userBookingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookedList.clear()
                val tripIds = mutableListOf<String>()

                for (data in snapshot.children) {
                    val tripId = data.key
                    // data.value true ekanligini tekshirish ham zarar qilmaydi
                    if (tripId != null) {
                        tripIds.add(tripId)
                    }
                }

                if (tripIds.isEmpty()) {
                    updateUI()
                } else {
                    // 3. ID lar orqali "trips" dan ma'lumot olamiz
                    fetchTripsDetails(tripIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    binding.progressBarList.visibility = View.GONE
                    Toast.makeText(context, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun fetchTripsDetails(tripIds: List<String>) {
        // --- ASOSIY TUZATISH: "Trips" -> "trips" ---
        val database = FirebaseDatabase.getInstance().getReference("trips")
        var loadedCount = 0

        for (tripId in tripIds) {
            database.child(tripId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val trip = snapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        bookedList.add(0, trip)
                    }
                    loadedCount++

                    if (loadedCount == tripIds.size) {
                        updateUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == tripIds.size) updateUI()
                }
            })
        }
    }

    private fun updateUI() {
        if (_binding != null) {
            binding.progressBarList.visibility = View.GONE
            adapter.notifyDataSetChanged()

            if (bookedList.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Hali safar band qilmadingiz"
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
