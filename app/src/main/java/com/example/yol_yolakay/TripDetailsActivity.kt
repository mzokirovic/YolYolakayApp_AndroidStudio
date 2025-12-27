package com.example.yol_yolakay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityTripDetailsBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ... super.onCreate(savedInstanceState) dan keyin:

        // Status bar va navigatsiyani shaffof qilish
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        // ... keyin binding inflate qilinadi


        // 1. Ma'lumotni JSON orqali olish
        try {
            val tripJson = intent.getStringExtra("TRIP_JSON")
            if (tripJson != null) {
                val gson = com.google.gson.Gson()
                currentTrip = gson.fromJson(tripJson, Trip::class.java)
            } else {
                // Ehtimol qidiruvdan Parcelable bo'lib kelgandir
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    currentTrip = intent.getParcelableExtra("TRIP_DATA", Trip::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    currentTrip = intent.getParcelableExtra("TRIP_DATA")
                }
            }
            isPreview = intent.getBooleanExtra("IS_PREVIEW", false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Agar ma'lumot baribir kelmasa
        if (currentTrip == null) {
            Toast.makeText(this, "E'lon ma'lumotlari yuklanmadi!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. UI ni chizish
        setupUI()
        setupButtons()
    }

    private fun setupUI() {
        val trip = currentTrip ?: return

        try {
            // 1. Yo'nalish (XATO TUZATILDI: Ikkita qator alohida yozildi)
            binding.tvFromCity.text = trip.from ?: "Noma'lum"
            binding.tvToCity.text = trip.to ?: "Noma'lum"

            // 2. Sana va Vaqt (Yangi dizaynga moslash)
            binding.tvDateHeader.text = trip.date ?: "Bugun"
            binding.tvStartTime.text = trip.time ?: "--:--"

            // Tugash vaqti (hozircha shartli ravishda)
            binding.tvEndTime.text = "Manzil"

            // 3. Narx
            val price = trip.price ?: 0
            val formattedPrice = String.format("%,d", price).replace(",", " ")
            binding.tvPrice.text = "$formattedPrice so'm"

            // 4. Haydovchi
            binding.tvDriverName.text = trip.driverName ?: "Haydovchi"

            // 5. Info
            if (trip.info.isNullOrEmpty()) {
                binding.tvInfo.text = "Qo'shimcha ma'lumot yo'q"
            } else {
                binding.tvInfo.text = trip.info
            }

            // Preview bo'lsa telefon tugmasini yashirish
            if (isPreview) {
                binding.btnCall.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("TripDetails", "UI xatolik: ${e.message}")
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCall.setOnClickListener {
            val phone = currentTrip?.driverPhone
            if (!phone.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:$phone")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Qo'ng'iroq qilib bo'lmadi", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Haydovchi raqami yo'q", Toast.LENGTH_SHORT).show()
            }
        }

        if (isPreview) {
            binding.btnBook.text = "E'lonni nashr qilish"
            binding.btnBook.setOnClickListener { publishTrip() }
        } else {
            binding.btnBook.text = "Joy band qilish"
            binding.btnBook.setOnClickListener {
                Toast.makeText(this, "Safar band qilindi! (Demo)", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun publishTrip() {
        val trip = currentTrip ?: return
        val database = FirebaseDatabase.getInstance().getReference("Trips")
        val newId = database.push().key ?: UUID.randomUUID().toString()
        trip.id = newId

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuklanmoqda..."

        database.child(newId).setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "E'lon joylandi! ✅", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "E'lonni nashr qilish"
            }
    }
}
