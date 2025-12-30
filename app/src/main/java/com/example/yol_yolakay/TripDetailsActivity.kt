package com.example.yol_yolakay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.api.RetrofitInstance
import com.example.yol_yolakay.databinding.ActivityTripDetailsBinding
import com.example.yol_yolakay.model.NotificationData
import com.example.yol_yolakay.model.PushNotification
import com.example.yol_yolakay.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    private val myUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar dizayni
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }

        loadTripData()

        if (currentTrip == null) {
            Toast.makeText(this, "E'lon ma'lumotlari yuklanmadi!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
        setupButtons()
    }

    private fun loadTripData() {
        try {
            val tripJson = intent.getStringExtra("TRIP_JSON")
            if (tripJson != null) {
                val gson = com.google.gson.Gson()
                currentTrip = gson.fromJson(tripJson, Trip::class.java)
            } else {
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
    }

    private fun setupUI() {
        val trip = currentTrip ?: return

        try {
            binding.tvFromCity.text = trip.from ?: "Noma'lum"
            binding.tvToCity.text = trip.to ?: "Noma'lum"
            binding.tvDateHeader.text = trip.date ?: "Bugun"
            binding.tvStartTime.text = trip.time ?: "--:--"
            binding.tvEndTime.text = "Manzil"

            val price = trip.price ?: 0
            val formattedPrice = String.format("%,d", price).replace(",", " ")
            binding.tvPrice.text = "$formattedPrice so'm"
            binding.tvDriverName.text = trip.driverName ?: "Haydovchi"

            if (trip.info.isNullOrEmpty()) {
                binding.tvInfo.text = "Qo'shimcha ma'lumot yo'q"
            } else {
                binding.tvInfo.text = trip.info
            }

            if (isPreview) {
                binding.btnCall.visibility = View.GONE
            }

        } catch (e: Exception) {
            Log.e("TripDetails", "UI xatolik: ${e.message}")
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        // Qo'ng'iroq qilish tugmasi
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

        // --- TUGMALARNI BOSHQARISH (MANTIQ) ---
        val trip = currentTrip ?: return

        // 1. Agar men HAYDOVCHI bo'lsam
        if (trip.userId == myUserId && !isPreview) {
            // Band qilish va Telefon qilish tugmalarini yashiramiz
            binding.btnBook.visibility = View.GONE
            binding.btnCall.visibility = View.GONE

            // Yakunlash tugmasini chiqaramiz
            binding.btnCompleteTrip.visibility = View.VISIBLE

            // Agar safar allaqachon tugagan bo'lsa
            if (trip.status == "completed") {
                binding.btnCompleteTrip.isEnabled = false
                binding.btnCompleteTrip.text = "Safar yakunlangan"
                binding.btnCompleteTrip.setBackgroundColor(android.graphics.Color.GRAY)
            }

            binding.btnCompleteTrip.setOnClickListener {
                completeTrip()
            }

        } else {
            // 2. Agar men YO'LOVCHI bo'lsam (yoki Preview rejimi)
            binding.btnCompleteTrip.visibility = View.GONE
            binding.btnBook.visibility = View.VISIBLE

            // Preview rejimida
            if (isPreview) {
                binding.btnBook.text = "E'lonni nashr qilish"
                binding.btnBook.setOnClickListener { publishTrip() }
            } else {
                // Oddiy rejimda
                binding.btnBook.text = "Joy band qilish"
                binding.btnBook.setOnClickListener { bookTripReal() }
            }
        }
    }

    // --- SAFARNI YAKUNLASH ---
    private fun completeTrip() {
        val trip = currentTrip ?: return
        if (trip.id == null) return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Safarni yakunlash")
            .setMessage("Ushbu safarni haqiqatan ham tugatmoqchimisiz? U 'Tarix' bo'limiga o'tkaziladi.")
            .setPositiveButton("Ha, yakunlash") { _, _ ->

                // Bazada statusni o'zgartirish
                val ref = FirebaseDatabase.getInstance().getReference("trips").child(trip.id!!)
                ref.child("status").setValue("completed")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Safar muvaffaqiyatli yakunlandi! 🏁", Toast.LENGTH_SHORT).show()
                        finish() // Oynani yopish
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Xatolik: ${it.message}", Toast.LENGTH_SHORT).show()
                    }

            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    // --- E'LONNI NASHR QILISH ---
    private fun publishTrip() {
        val trip = currentTrip ?: return

        val database = FirebaseDatabase.getInstance().getReference("trips")
        val newId = database.push().key ?: UUID.randomUUID().toString()

        trip.id = newId
        trip.userId = myUserId
        // Yangi e'lon har doim active bo'ladi
        trip.status = "active"

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

    // --- HAQIQIY BAND QILISH (BOOKING) ---
    private fun bookTripReal() {
        if (myUserId.isEmpty()) {
            Toast.makeText(this, "Iltimos, avval tizimga kiring!", Toast.LENGTH_SHORT).show()
            return
        }

        val trip = currentTrip ?: return
        val driverId = trip.userId

        if (driverId == myUserId) {
            Toast.makeText(this, "Siz o'zingizning e'loningizni band qila olmaysiz!", Toast.LENGTH_LONG).show()
            return
        }

        if (trip.id.isNullOrEmpty()) {
            Toast.makeText(this, "E'lon IDsi topilmadi", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Band qilinmoqda..."

        val database = FirebaseDatabase.getInstance()
        val tripBookingRef = database.getReference("trips").child(trip.id!!).child("bookedUsers")
        val myProfileRef = database.getReference("Users").child(myUserId).child("bookedTrips")

        // Notification
        val notificationRef = database.getReference("Notifications").child(driverId ?: "unknown").push()
        val notificationDataMap = mapOf(
            "title" to "Yangi yo'lovchi!",
            "message" to "${trip.from} - ${trip.to} yo'nalishingizga joy band qilindi.",
            "tripId" to (trip.id ?: ""),
            "passengerId" to myUserId,
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )

        tripBookingRef.child(myUserId).setValue(true)
            .addOnSuccessListener {
                myProfileRef.child(trip.id!!).setValue(true)
                notificationRef.setValue(notificationDataMap)
                sendPushNotificationToDriver(driverId ?: "", trip.from ?: "", trip.to ?: "")

                Toast.makeText(this, "Joy muvaffaqiyatli band qilindi! ✅", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "Joy band qilish"
            }
    }

    // --- PUSH XABAR YUBORISH ---
    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        if (driverId.isEmpty()) return

        val database = FirebaseDatabase.getInstance().getReference("Users").child(driverId).child("fcmToken")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)

                if (!token.isNullOrEmpty()) {
                    val title = "Yangi yo'lovchi! 🚖"
                    val message = "$from - $to safaringizga joy band qilindi."

                    val notificationData = NotificationData(title, message)
                    val pushNotification = PushNotification(notificationData, token)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitInstance.api.postNotification(pushNotification)
                            if (response.isSuccessful) {
                                Log.d("FCM", "Xabar yuborildi")
                            } else {
                                Log.e("FCM", "Xatolik: ${response.errorBody()}")
                            }
                        } catch (e: Exception) {
                            Log.e("FCM", "Internet xatosi: ${e.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
