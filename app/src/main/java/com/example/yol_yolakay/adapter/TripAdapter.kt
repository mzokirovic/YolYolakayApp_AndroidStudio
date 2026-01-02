package com.example.yol_yolakay.adapter

import android.graphics.Color
import android.view.LayoutInflater
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

            // 1. YO'NALISH (Yangi dizaynda alohida-alohida)
            // Eski 'tvRoute' o'rniga 'tvFromCity' va 'tvToCity' ishlatamiz
            binding.tvFromCity.text = trip.from ?: "Qayerdan"
            binding.tvToCity.text = trip.to ?: "Qayerga"

            // 2. VAQT (Boshlanish va Tugash)
            binding.tvStartTime.text = trip.time ?: "--:--"
            // Tugash vaqti hisoblanmagan bo'lsa, shunchaki manzil so'zini yoki taxminiy vaqtni qo'yamiz
            binding.tvEndTime.text = "Manzil"

            // 3. SANA (Header qismida)
            binding.tvDate.text = trip.date ?: "Sana yo'q"

            // 4. NARX
            val formattedPrice = String.format("%,d", trip.price ?: 0).replace(",", " ")
            binding.tvPrice.text = "$formattedPrice so'm"

            // 5. HAYDOVCHI ISMI
            binding.tvPersonName.text = trip.driverName ?: "Haydovchi"

            // 6. STATUS (Yashil yoki Sariq belgi)
            if (trip.status == "completed") {
                binding.tvStatusBadge.text = "TUGAGAN"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#9E9E9E")) // Kulrang
                // Agar orqa fon drawable fayli yo'q bo'lsa, shunchaki rang berish mumkin:
                // binding.tvStatusBadge.setBackgroundColor(Color.parseColor("#EEEEEE"))

                // Lekin sizda bg_status_... fayllari bor deb hisoblaymiz:
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_yellow)
            } else {
                binding.tvStatusBadge.text = "FAOL"
                binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981")) // Yashil
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_green)
            }

            // 7. AVATAR
            binding.imgAvatar.setImageResource(R.drawable.ic_person)

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
