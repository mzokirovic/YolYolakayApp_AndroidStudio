package com.example.yol_yolakay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.databinding.ItemTripBinding
import com.example.yol_yolakay.model.Trip

class TripAdapter(
    private var tripList: MutableList<Trip>, // Ro'yxat
    private val onItemClick: (Trip) -> Unit  // Bosilganda nima bo'lishi
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {
            // Ma'lumotlarni XML ga bog'lash
            // Agar sizda null muammosi bo'lsa, "?:" orqali default qiymat beramiz
            binding.tvFromTo.text = "${trip.from ?: ""} - ${trip.to ?: ""}"
            binding.tvDate.text = trip.date ?: ""
            binding.tvTime.text = trip.time ?: ""
            binding.tvPrice.text = "${trip.price} so'm"
            binding.tvSeats.text = "${trip.seats} kishi"

            // Item bosilganda
            binding.root.setOnClickListener {
                onItemClick(trip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripList[position])
    }

    override fun getItemCount(): Int = tripList.size

    // --- MUHIM JOYI: Activitydan ma'lumot qabul qilish funksiyasi ---
    fun updateList(newList: List<Trip>) {
        tripList.clear()
        tripList.addAll(newList)
        notifyDataSetChanged()
    }
}
