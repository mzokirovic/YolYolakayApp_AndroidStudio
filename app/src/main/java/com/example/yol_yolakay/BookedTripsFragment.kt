package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter
import com.example.yol_yolakay.databinding.FragmentTripsListBinding
import com.example.yol_yolakay.model.Trip
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
            // Band qilingan safarni ochish
            val intent = Intent(context, TripDetailsActivity::class.java)
            intent.putExtra("TRIP_DATA", trip)
            intent.putExtra("IS_PREVIEW", false) // Bu preview emas
            startActivity(intent)
        }
        binding.recyclerViewList.adapter = adapter

        loadBookedTrips()
    }

    private fun loadBookedTrips() {
        binding.progressBarList.visibility = View.VISIBLE

        // Hozirgi user ID (DeviceId)
        val deviceId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        val database = FirebaseDatabase.getInstance().getReference("BookedTrips").child(deviceId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                bookedList.clear()
                for (data in snapshot.children) {
                    val trip = data.getValue(Trip::class.java)
                    if (trip != null) {
                        bookedList.add(0, trip)
                    }
                }

                if (_binding != null) {
                    binding.progressBarList.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (bookedList.isEmpty()) View.VISIBLE else View.GONE
                    if (bookedList.isEmpty()) binding.tvEmpty.text = "Hali safar band qilmadingiz"
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
