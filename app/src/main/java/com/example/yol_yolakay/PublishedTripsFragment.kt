package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.FragmentTripsListBinding // XML nomi fragment_trips_list bo'lishi kerak
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PublishedTripsFragment : Fragment() {

    private var _binding: FragmentTripsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TripAdapter
    private val tripsList = mutableListOf<Trip>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTripsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewList.layoutManager = LinearLayoutManager(context)
        adapter = TripAdapter(tripsList) { trip ->
            // E'lonni tahrirlash uchun ochish
            val intent = Intent(context, TripDetailsActivity::class.java)
            intent.putExtra("TRIP_DATA", trip)
            intent.putExtra("IS_PREVIEW", true) // O'zimizniki bo'lgani uchun tahrirlash mumkin
            startActivity(intent)
        }
        binding.recyclerViewList.adapter = adapter

        loadMyPublishedTrips()
    }

    private fun loadMyPublishedTrips() {
        binding.progressBarList.visibility = View.VISIBLE
        val database = FirebaseDatabase.getInstance().getReference("Trips")

        // Hozircha hamma e'lonlarni olamiz (Login qilganda o'zgartiramiz: .orderByChild("driverId").equalTo(uid))
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripsList.clear()
                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)
                    if (trip != null) {
                        tripsList.add(0, trip)
                    }
                }

                if (_binding != null) {
                    binding.progressBarList.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (tripsList.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) binding.progressBarList.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
