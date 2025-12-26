package com.example.yol_yolakay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityTripDetailsBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID // ID yo'q bo'lsa ishlatish uchun

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentTrip = intent.getSerializableExtra("TRIP_DATA") as? Trip
        isPreview = intent.getBooleanExtra("IS_PREVIEW", false)

        setupUI()

        binding.btnBackHome.setOnClickListener { finish() }

        // Qo'ng'iroq qilish
        binding.btnCallDriver.setOnClickListener {
            // Hozircha haydovchi raqami yo'q bo'lsa, o'zimiznikini qo'yamiz
            val phoneNumber = "tel:+998901234567"
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse(phoneNumber)
            startActivity(intent)
        }

        // TUGMA MOSLASHUVI (Preview vs Booking)
        if (isPreview) {
            // Agar o'zimizning e'lon bo'lsa -> "Nashr qilish" (Oldingi mantiq)
            binding.btnBookAction.text = "Nashr qilish"
            binding.contactButtonsLayout.visibility = View.GONE // O'ziga tel qilmaydi

            binding.btnBookAction.setOnClickListener {
                publishTrip() // Oldingi Firebasega yozish funksiyasi
            }
        } else {
            // Agar yo'lovchi bo'lsa -> "Bron qilish"
            binding.btnBookAction.text = "Bron qilish"
            binding.contactButtonsLayout.visibility = View.VISIBLE

            binding.btnBookAction.setOnClickListener {
                bookTrip() // Band qilish funksiyasi
            }
        }
    }

    private fun setupUI() {
        val trip = currentTrip ?: return

        binding.tvFrom.text = trip.from
        binding.tvTo.text = trip.to
        binding.tvDateTime.text = "${trip.date} • ${trip.time}"

        val formattedPrice = String.format("%,d", trip.price ?: 0).replace(",", " ")
        binding.tvTotalPrice.text = "$formattedPrice so'm"

        binding.tvSeatsDetail.text = "${trip.seats} ta"
        binding.tvDriverNameDetails.text = "Haydovchi" // Keyin User nomini ulaymiz
    }

    // 1. HAYDOVCHI UCHUN: NASHR QILISH
    private fun publishTrip() {
        val trip = currentTrip ?: return
        val database = FirebaseDatabase.getInstance().getReference("Trips")
        val newId = database.push().key ?: return
        trip.id = newId

        binding.btnBookAction.isEnabled = false
        binding.btnBookAction.text = "Yuklanmoqda..."

        database.child(newId).setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "E'lon muvaffaqiyatli joylandi!", Toast.LENGTH_LONG).show()
                // Bosh sahifaga qaytish
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik bo'ldi", Toast.LENGTH_SHORT).show()
                binding.btnBookAction.isEnabled = true
            }
    }

    // 2. YO'LOVCHI UCHUN: BRON QILISH
    private fun bookTrip() {
        val trip = currentTrip ?: return
        // MVP uchun oddiy yechim:
        // "BookedTrips" degan alohida joyga yozamiz. Har bir qurilma (user) uchun.

        // Bu qurilma uchun unikal ID (Vaqtinchalik yechim, Auth bo'lmasa)
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)

        val database = FirebaseDatabase.getInstance().getReference("BookedTrips")

        // BookedTrips -> UserID -> TripID -> TripObject
        val tripId = trip.id ?: UUID.randomUUID().toString() // ID bo'lishi shart

        database.child(deviceId).child(tripId).setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "Safar muvaffaqiyatli band qilindi! ✅", Toast.LENGTH_LONG).show()
                finish() // Yopamiz
            }
            .addOnFailureListener {
                Toast.makeText(this, "Internetda xatolik", Toast.LENGTH_SHORT).show()
            }
    }
}
