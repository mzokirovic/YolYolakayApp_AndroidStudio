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

    // Barcha fayllarda bir xil ID bo'lishi uchun FirebaseAuth ishlatildi.
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
                bookTripReal()
            }
        }
    }

    // --- 1. E'LONNI NASHR QILISH ---
    private fun publishTrip() {
        val trip = currentTrip ?: return

        // "trips" (kichik harf) bo'lishi shart
        val database = FirebaseDatabase.getInstance().getReference("trips")
        val newId = database.push().key ?: UUID.randomUUID().toString()

        trip.id = newId
        trip.userId = myUserId

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

    // --- 2. HAQIQIY BAND QILISH (BOOKING) ---
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

        // 1. E'lon ichiga "bookedUsers" ga meni qo'shish
        val tripBookingRef = database.getReference("trips").child(trip.id!!).child("bookedUsers")

        // 2. Mening profilimga "bookedTrips" qo'shish
        val myProfileRef = database.getReference("Users").child(myUserId).child("bookedTrips")

        // 3. Ilova ichidagi xabarnoma (History uchun)
        val notificationRef = database.getReference("Notifications").child(driverId ?: "unknown").push()
        val notificationDataMap = mapOf(
            "title" to "Yangi yo'lovchi!",
            "message" to "${trip.from} - ${trip.to} yo'nalishingizga joy band qilindi.",
            "tripId" to (trip.id ?: ""),
            "passengerId" to myUserId,
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )

        // Hammasini bir vaqtda bajarishga harakat qilamiz
        tripBookingRef.child(myUserId).setValue(true)
            .addOnSuccessListener {

                // Profilga yozish
                myProfileRef.child(trip.id!!).setValue(true)

                // Ilova ichidagi xabarnoma bazaga
                notificationRef.setValue(notificationDataMap)

                // --- 4. TELEFONGA PUSH NOTIFICATION YUBORISH (YANGI) ---
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

    // --- PUSH XABAR YUBORISH FUNKSIYASI ---
    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        if (driverId.isEmpty()) return

        // 1. Haydovchining Tokenini bazadan olamiz
        val database = FirebaseDatabase.getInstance().getReference("Users").child(driverId).child("fcmToken")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)

                if (!token.isNullOrEmpty()) {
                    // 2. Xabar ma'lumotlarini tayyorlaymiz
                    val title = "Yangi yo'lovchi! 🚖"
                    val message = "$from - $to safaringizga joy band qilindi."

                    val notificationData = NotificationData(title, message)
                    val pushNotification = PushNotification(notificationData, token)

                    // 3. Internet orqali yuboramiz (Coroutine ishlatamiz)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitInstance.api.postNotification(pushNotification)
                            if (response.isSuccessful) {
                                Log.d("FCM", "Xabar yuborildi: ${response.body()}")
                            } else {
                                Log.e("FCM", "Xatolik: ${response.errorBody()}")
                            }
                        } catch (e: Exception) {
                            Log.e("FCM", "Internet xatosi: ${e.message}")
                        }
                    }
                } else {
                    Log.e("FCM", "Haydovchining tokeni topilmadi")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FCM", "Bazadan o'qish xatosi: ${error.message}")
            }
        })
    }
}
