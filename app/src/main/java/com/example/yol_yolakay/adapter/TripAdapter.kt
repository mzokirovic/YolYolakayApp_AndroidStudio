package com.example.yol_yolakay.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.R
import com.example.yol_yolakay.databinding.ItemTripBinding
import com.example.yol_yolakay.model.Trip

class TripAdapter(
    private var tripList: MutableList<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {

            // 1. YO'NALISH (Toshkent → Samarqand)
            // Eski "tvFromCity" va "tvToCity" endi bitta "tvRoute" bo'ldi
            binding.tvRoute.text = "${trip.from} → ${trip.to}"

            // 2. NARX
            val formattedPrice = String.format("%,d", trip.price ?: 0).replace(",", " ")
            binding.tvPrice.text = "$formattedPrice so'm"

            // 3. SANA VA VAQT
            binding.tvDate.text = trip.date ?: "Sana yo'q"
            binding.tvTime.text = trip.time ?: "Vaqt yo'q"

            // 4. HAYDOVCHI ISMI
            binding.tvPersonName.text = trip.driverName ?: "Haydovchi"

            // 5. STATUS (Yashil yoki Sariq belgi)
            if (trip.status == "completed") {
                binding.tvStatusBadge.text = "TUGAGAN"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#FF9800")) // To'q sariq
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_yellow)
            } else {
                binding.tvStatusBadge.text = "FAOL"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#4CAF50")) // Yashil
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_green)
            }

            // 6. AVATAR (Hozircha standart rasm)
            // Keyinchalik bu yerga Glide kutubxonasi bilan rasm yuklashni qo'shamiz
            binding.imgAvatar.setImageResource(R.drawable.ic_launcher_foreground)

            // Bosilganda
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
