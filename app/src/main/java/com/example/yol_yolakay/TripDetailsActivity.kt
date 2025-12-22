package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityTripDetailsBinding
import com.example.yol_yolakay.model.Trip

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent orqali kelgan ma'lumotni olamiz
        val trip = intent.getSerializableExtra("trip") as? Trip

        if (trip != null) {
            binding.tvFrom.text = trip.from
            binding.tvTo.text = trip.to
            binding.tvDate.text = trip.date
            binding.tvTime.text = trip.time
            binding.tvPrice.text = "${trip.price} so'm"
            binding.tvSeats.text = "${trip.seats} ta"
            binding.tvInfo.text = trip.info ?: "Qo'shimcha ma'lumot yo'q"
        }

        binding.btnBackHome.setOnClickListener {
            // Bosh sahifaga qaytish (barcha activitylarni yopib)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
