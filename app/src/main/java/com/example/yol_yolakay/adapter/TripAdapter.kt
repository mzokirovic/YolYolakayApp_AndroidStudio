package com.example.yol_yolakay.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.example.yol_yolakay.R
import com.example.yol_yolakay.databinding.ItemTripBinding
import com.example.yol_yolakay.model.Trip

class TripAdapter(
    private var tripList: MutableList<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {
            // 1. YO'NALISH
            binding.tvFromCity.text = trip.from ?: "Qayerdan"
            binding.tvToCity.text = trip.to ?: "Qayerga"

            // 2. VAQT
            binding.tvStartTime.text = trip.time ?: "--:--"
            binding.tvEndTime.text = "Manzil"

            // 3. SANA
            binding.tvDate.text = trip.date ?: "Sana yo'q"

            // 4. NARX (Xavfsiz hisoblash funksiyasidan foydalanamiz)
            val price = trip.getPriceAsLong()
            binding.tvPrice.text = String.format("%,d", price).replace(",", " ") + " so'm"

            // 5. HAYDOVCHI ISMI
            binding.tvPersonName.text = trip.driverName ?: "Haydovchi"

            // 6. STATUS (Mantiq soddalashtirildi)
            if (trip.status == "completed") {
                binding.tvStatusBadge.apply {
                    text = "TUGAGAN"
                    setTextColor(Color.GRAY)
                    setBackgroundResource(R.drawable.bg_status_yellow)
                }
            } else {
                binding.tvStatusBadge.apply {
                    text = "FAOL"
                    setTextColor(Color.parseColor("#10B981"))
                    setBackgroundResource(R.drawable.bg_status_green)
                }
            }

            // 7. AVATAR
            binding.imgAvatar.setImageResource(R.drawable.ic_person)

            binding.root.setOnClickListener { onItemClick(trip) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        return TripViewHolder(
            ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripList[position])
    }

    override fun getItemCount(): Int = tripList.size

    // PROFESSIONALLIK: DiffUtil orqali aqlli yangilanish
    fun updateList(newList: List<Trip>) {
        val diffCallback = TripDiffCallback(this.tripList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.tripList.clear()
        this.tripList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    // Solishtirish algoritmi
    class TripDiffCallback(
        private val oldList: List<Trip>,
        private val newList: List<Trip>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
