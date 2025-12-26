package com.example.yol_yolakay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.databinding.ItemTripBinding
import com.example.yol_yolakay.model.Trip

class TripAdapter(
    private var tripList: MutableList<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {
            // 1. Shaharlar (Yangi XML IDlari bo'yicha)
            binding.tvFromCity.text = trip.from ?: ""
            binding.tvToCity.text = trip.to ?: ""

            // 2. Vaqt (Faqat soatni ko'rsatamiz, sana kerak emas, chunki u tepada bo'ladi yoki alohida)
            binding.tvStartTime.text = trip.time ?: "00:00"

            // 3. Narx (Mingliklarni ajratib ko'rsatish: 75 000)
            val formattedPrice = String.format("%,d", trip.price ?: 0).replace(",", " ")
            binding.tvPrice.text = formattedPrice

            // 4. Haydovchi ismi (Hozircha statik yoki trip modelida bo'lsa o'shani qo'yamiz)
            // Agar Trip modelida driverName bo'lmasa, vaqtinchalik "Haydovchi" deb turamiz
            binding.tvDriverName.text = "Haydovchi"

            // 5. O'rindiqlar soni
            binding.tvSeatsCount.text = "${trip.seats ?: 0} o'rin bo'sh"

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

    fun updateList(newList: List<Trip>) {
        tripList.clear()
        tripList.addAll(newList)
        notifyDataSetChanged()
    }
}
