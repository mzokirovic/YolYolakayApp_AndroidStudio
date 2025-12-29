package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.TripAdapter // Agar qizil bo'lsa, 'adapters' qilib ko'ring
import com.example.yol_yolakay.databinding.FragmentMyTripsListBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson

class MyTripsListFragment : Fragment() {

    private var _binding: FragmentMyTripsListBinding? = null
    private val binding get() = _binding!!

    // "PUBLISHED" -> Men haydovchi sifatida qo'shganlarim
    // "BOOKED" -> Men yo'lovchi sifatida band qilganlarim
    private var fragmentType: String = "PUBLISHED"

    private lateinit var tripAdapter: TripAdapter
    private val tripsList = ArrayList<Trip>()

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(type: String): MyTripsListFragment {
            val fragment = MyTripsListFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fragmentType = it.getString(ARG_TYPE, "PUBLISHED")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadTrips()
    }

    private fun setupRecyclerView() {
        tripAdapter = TripAdapter(tripsList) { selectedTrip ->
            val intent = Intent(requireContext(), TripDetailsActivity::class.java)
            val gson = Gson()
            intent.putExtra("TRIP_JSON", gson.toJson(selectedTrip))
            intent.putExtra("IS_PREVIEW", false)
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = tripAdapter
    }

    private fun loadTrips() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showEmptyState("Iltimos, avval tizimga kiring")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val database = FirebaseDatabase.getInstance()

        if (fragmentType == "PUBLISHED") {
            // -------------------------------------------------------
            // 1. MENING E'LONLARIM (Mening IDim bilan qo'shilganlar)
            // -------------------------------------------------------
            val tripsRef = database.getReference("trips")
            val query = tripsRef.orderByChild("userId").equalTo(currentUser.uid)

            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tripsList.clear()
                    for (data in snapshot.children) {
                        val trip = data.getValue(Trip::class.java)
                        if (trip != null) {
                            // ID bo'lmasa, key'dan olamiz
                            if (trip.id == null) trip.id = data.key
                            tripsList.add(trip)
                        }
                    }
                    // Yangilari tepada tursin
                    tripsList.reverse()
                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError(error)
                }
            })

        } else {
            // -------------------------------------------------------
            // 2. BAND QILINGANLAR (Users -> me -> bookedTrips)
            // -------------------------------------------------------
            val myBookingsRef = database.getReference("Users")
                .child(currentUser.uid).child("bookedTrips")

            myBookingsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val bookedTripIds = ArrayList<String>()

                    for (child in snapshot.children) {
                        val tripId = child.key
                        if (tripId != null) {
                            bookedTripIds.add(tripId)
                        }
                    }

                    if (bookedTripIds.isEmpty()) {
                        tripsList.clear()
                        updateUI()
                    } else {
                        // IDlar orqali safar tafsilotlarini yuklaymiz
                        fetchBookedTripsDetails(bookedTripIds)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    handleError(error)
                }
            })
        }
    }

    // Band qilingan safarlarning to'liq ma'lumotini olish
    private fun fetchBookedTripsDetails(ids: List<String>) {
        val database = FirebaseDatabase.getInstance().getReference("trips")
        tripsList.clear()

        var loadedCount = 0
        // Agar birorta ham ID bo'lmasa, shunchaki yangilaymiz
        if (ids.isEmpty()) {
            updateUI()
            return
        }

        for (id in ids) {
            database.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val trip = snapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        if (trip.id == null) trip.id = snapshot.key
                        tripsList.add(trip)
                    }
                    loadedCount++

                    // Hammasi yuklanib bo'lgach yangilaymiz
                    if (loadedCount == ids.size) {
                        tripsList.reverse()
                        updateUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == ids.size) updateUI()
                }
            })
        }
    }

    private fun updateUI() {
        if (_binding == null) return

        binding.progressBar.visibility = View.GONE
        tripAdapter.notifyDataSetChanged()

        if (tripsList.isEmpty()) {
            val msg = if (fragmentType == "PUBLISHED") "Siz hali e'lon bermagansiz" else "Siz hali safar band qilmagansiz"
            showEmptyState(msg)
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(message: String) {
        if (_binding == null) return
        binding.progressBar.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.text = message
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun handleError(error: DatabaseError) {
        if (_binding != null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
