package com.example.yol_yolakay

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    // Haydovchi uchun adapter va ro'yxat
    private lateinit var requestAdapter: RequestAdapter
    private val requestList = ArrayList<UserRequest>()

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
                    currentTrip = intent.getParcelableExtra("trip_data", Trip::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    currentTrip = intent.getParcelableExtra("trip_data")
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

        } catch (e: Exception) {
            Log.e("TripDetails", "UI xatolik: ${e.message}")
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        val trip = currentTrip ?: return

        // 1. TELEFON VA SMS
        binding.btnCallBottom.setOnClickListener {
            val phone = trip.driverPhone
            if (!phone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Raqam yo'q", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSms.setOnClickListener {
            val phone = trip.driverPhone
            if (!phone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
                intent.putExtra("sms_body", "Assalomu alaykum! Men ${trip.from} dan ${trip.to} ga ketmoqchi edim.")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Raqam yo'q", Toast.LENGTH_SHORT).show()
            }
        }

        // --- MANTIQ (Haydovchi yoki Yo'lovchi) ---
        if (trip.userId == myUserId && !isPreview) {
            // A) AGAR MEN HAYDOVCHI BO'LSAM
            binding.btnSms.visibility = View.GONE
            binding.btnCallBottom.visibility = View.GONE

            binding.btnBook.visibility = View.VISIBLE
            binding.btnBook.text = "Safarni yakunlash"

            // --- TUZATILDI: Qizil rang (yumaloq shakl uchun) ---
            binding.btnBook.setBackgroundResource(R.drawable.bg_button_danger)

            loadRequests(trip.id!!)

            if (trip.status == "completed") {
                binding.btnBook.isEnabled = false
                binding.btnBook.text = "Safar yakunlangan"
                // --- TUZATILDI: Kulrang (yumaloq shakl uchun) ---
                binding.btnBook.setBackgroundResource(R.drawable.bg_button_completed)
            }

            binding.btnBook.setOnClickListener {
                completeTrip()
            }

        } else {
            // B) AGAR MEN YO'LOVCHI BO'LSAM (yoki Preview)
            binding.btnSms.visibility = View.VISIBLE
            binding.btnCallBottom.visibility = View.VISIBLE
            binding.btnBook.visibility = View.VISIBLE

            if (isPreview) {
                binding.btnBook.text = "E'lonni nashr qilish"
                binding.btnBook.setBackgroundResource(R.drawable.bg_gradient_premium)
                binding.btnBook.setOnClickListener { publishTrip() }

                binding.btnSms.visibility = View.GONE
                binding.btnCallBottom.visibility = View.GONE
            } else {
                checkRequestStatus(trip.id!!)
            }
        }
    }

    // --- Safarni yakunlash (Haydovchi uchun) ---
    private fun completeTrip() {
        val tripId = currentTrip?.id ?: return

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yakunlanmoqda..."

        FirebaseDatabase.getInstance().getReference("trips")
            .child(tripId)
            .child("status")
            .setValue("completed")
            .addOnSuccessListener {
                Toast.makeText(this, "Safar muvaffaqiyatli yakunlandi!", Toast.LENGTH_SHORT).show()
                binding.btnBook.text = "Safar yakunlangan"

                // --- TUZATILDI: Kulrang (yumaloq shakl) ---
                binding.btnBook.setBackgroundResource(R.drawable.bg_button_completed)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "Safarni yakunlash"
                // Xatolik bo'lsa qaytib Qizil rangga o'tadi
                binding.btnBook.setBackgroundResource(R.drawable.bg_button_danger)
            }
    }

    private fun publishTrip() {
        Toast.makeText(this, "E'lon chop etilmoqda...", Toast.LENGTH_SHORT).show()
        finish()
    }

    // --- 1. YO'LOVCHI: So'rov yuborish ---
    private fun sendBookingRequest() {
        val trip = currentTrip ?: return
        if (myUserId.isEmpty()) {
            Toast.makeText(this, "Iltimos, avval tizimga kiring!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuborilmoqda..."

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(myUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Yo'lovchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()
                val myPhone = snapshot.child("phone").getValue(String::class.java) ?: ""

                val requestMap = mapOf(
                    "userId" to myUserId,
                    "name" to fullName,
                    "phone" to myPhone,
                    "status" to "pending"
                )

                FirebaseDatabase.getInstance().getReference("trips")
                    .child(trip.id!!)
                    .child("requests")
                    .child(myUserId)
                    .setValue(requestMap)
                    .addOnSuccessListener {
                        Toast.makeText(this@TripDetailsActivity, "So'rov yuborildi! ✅", Toast.LENGTH_LONG).show()

                        // --- TUZATILDI: Rang o'rniga Resource ---
                        binding.btnBook.text = "So'rov yuborilgan ⏳"
                        // Bu yerda sariq drawable ishlatiladi va shakl buzilmaydi
                        binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)
                        // ----------------------------------------

                        sendPushNotificationToDriver(trip.userId ?: "", trip.from ?: "", trip.to ?: "")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@TripDetailsActivity, "Xatolik bo'ldi", Toast.LENGTH_SHORT).show()
                        binding.btnBook.isEnabled = true
                        binding.btnBook.text = "Joy band qilish"
                        // Xatolik bo'lsa yana gradientga qaytamiz
                        binding.btnBook.setBackgroundResource(R.drawable.bg_gradient_premium)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.btnBook.isEnabled = true
            }
        })
    }

    // --- 2. YO'LOVCHI: Statusni tekshirish ---
    private fun checkRequestStatus(tripId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId)

        // A) Qabul qilinganmi?
        ref.child("bookedUsers").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.btnBook.text = "Siz qabul qilingansiz ✅"
                    binding.btnBook.isEnabled = false
                    // --- TUZATILDI: Yashil drawable ---
                    binding.btnBook.setBackgroundResource(R.drawable.bg_button_success)
                } else {
                    // B) Kutilmoqdami?
                    ref.child("requests").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reqSnapshot: DataSnapshot) {
                            if (reqSnapshot.exists()) {
                                binding.btnBook.text = "So'rov yuborilgan ⏳"
                                binding.btnBook.isEnabled = false
                                // --- TUZATILDI: Sariq drawable ---
                                binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)
                            } else {
                                // Hali so'rov yubormagan
                                binding.btnBook.text = "Joy band qilish"
                                binding.btnBook.isEnabled = true
                                // Default holat: Gradient
                                binding.btnBook.setBackgroundResource(R.drawable.bg_gradient_premium)
                                binding.btnBook.setOnClickListener { sendBookingRequest() }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- 3. HAYDOVCHI: So'rovlarni yuklash ---
    private fun loadRequests(tripId: String) {
        binding.layoutRequests.visibility = View.VISIBLE
        binding.tvRequestsTitle.text = "Kutilayotgan so'rovlar:"

        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = RequestAdapter(requestList) { request, isAccepted ->
            handleRequestAction(tripId, request, isAccepted)
        }
        binding.rvRequests.adapter = requestAdapter
        binding.rvRequests.isNestedScrollingEnabled = false

        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestList.clear()
                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: "Noma'lum"
                    val phone = child.child("phone").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "pending"

                    if (status == "pending") {
                        requestList.add(UserRequest(userId, name, phone, status))
                    }
                }
                requestAdapter.notifyDataSetChanged()

                if (requestList.isEmpty()) {
                    binding.tvRequestsTitle.text = "Yangi so'rovlar yo'q"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TripDetailsActivity, "So'rovlarni yuklashda xatolik", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleRequestAction(tripId: String, request: UserRequest, isAccepted: Boolean) {
        val requestsRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests").child(request.userId)
        val bookedRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("bookedUsers").child(request.userId)
        val userBookedRef = FirebaseDatabase.getInstance().getReference("Users").child(request.userId).child("bookedTrips").child(tripId)

        if (isAccepted) {
            bookedRef.setValue(true)
            userBookedRef.setValue(true)
            requestsRef.removeValue()
            Toast.makeText(this, "${request.name} qabul qilindi!", Toast.LENGTH_SHORT).show()
        } else {
            requestsRef.removeValue()
            Toast.makeText(this, "So'rov rad etildi", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Push Notification ---
    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        val database = FirebaseDatabase.getInstance()
        val tokenRef = database.getReference("Users").child(driverId).child("fcmToken")

        tokenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)
                if (token != null) {
                    val pushNotification = PushNotification(
                        NotificationData("Yangi buyurtma!", "$from dan $to ga yangi yo'lovchi so'rov yubordi."),
                        token
                    )
                    sendNotification(pushNotification)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendNotification(notification: PushNotification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d("FCM", "Xabar yuborildi: ${response.body()}")
                } else {
                    Log.e("FCM", "Xatolik: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Xatolik: ${e.message}")
            }
        }
    }
}
