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

        // Status bar dizayni (O'zgarishsiz)
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
            binding.tvEndTime.text = "Manzil" // Yoki hisoblangan vaqt

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

    // --- ASOSIY O'ZGARISH: Tugmalar va Mantiq (Yangi dizaynga moslab) ---
    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        val trip = currentTrip ?: return

        // 1. TELEFON VA SMS (Faqat yo'lovchi uchun kerak, lekin click listenerlarni tayyorlab qo'yamiz)
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

            // 1. Yo'lovchi tugmalarini (SMS/Call) yashiramiz
            binding.btnSms.visibility = View.GONE
            binding.btnCallBottom.visibility = View.GONE

            // 2. Asosiy tugmani "Safarni yakunlash" ga aylantiramiz
            // (Yangi dizaynda alohida btnCompleteTrip yo'q, btnBook ishlatiladi)
            binding.btnBook.visibility = View.VISIBLE
            binding.btnBook.text = "Safarni yakunlash"
            binding.btnBook.setBackgroundColor(Color.parseColor("#EF4444")) // Qizil rang

            // 3. So'rovlar ro'yxatini yuklaymiz
            loadRequests(trip.id!!)

            // 4. Status tekshiruvi
            if (trip.status == "completed") {
                binding.btnBook.isEnabled = false
                binding.btnBook.text = "Safar yakunlangan"
                binding.btnBook.setBackgroundColor(Color.GRAY)
            }

            // 5. Bosilganda yakunlash funksiyasi
            binding.btnBook.setOnClickListener {
                completeTrip()
            }

        } else {
            // B) AGAR MEN YO'LOVCHI BO'LSAM (yoki Preview)

            // Barcha tugmalar ko'rinadi
            binding.btnSms.visibility = View.VISIBLE
            binding.btnCallBottom.visibility = View.VISIBLE
            binding.btnBook.visibility = View.VISIBLE

            if (isPreview) {
                // Preview rejimi (E'lon berishdan oldin ko'rish)
                binding.btnBook.text = "E'lonni nashr qilish"
                binding.btnBook.setOnClickListener { publishTrip() }

                // Previewda haydovchi o'ziga yozmaydi
                binding.btnSms.visibility = View.GONE
                binding.btnCallBottom.visibility = View.GONE
            } else {
                // Oddiy Yo'lovchi rejimi: Statusni tekshiramiz
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
                binding.btnBook.setBackgroundColor(Color.GRAY)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "Safarni yakunlash"
            }
    }

    // --- E'lon qilish (Preview rejimi uchun) ---
    private fun publishTrip() {
        // Bu funksiya AddTripActivity dan kelgan ma'lumotni bazaga yozish uchun kerak bo'ladi
        // Hozircha oddiy Toast:
        Toast.makeText(this, "E'lon chop etilmoqda...", Toast.LENGTH_SHORT).show()
        // Bu yerda real publish logikasi bo'lishi kerak (yoki AddTripActivity o'zi qiladi)
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

        // Foydalanuvchi ma'lumotlarini olamiz
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(myUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Yo'lovchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()
                val myPhone = snapshot.child("phone").getValue(String::class.java) ?: ""

                // Ma'lumot tayyor, endi yuboramiz
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
                        binding.btnBook.text = "So'rov yuborilgan ⏳"
                        binding.btnBook.setBackgroundColor(Color.parseColor("#FFC107")) // Sariq

                        // Push notification yuborish (agar mavjud bo'lsa)
                        sendPushNotificationToDriver(trip.userId ?: "", trip.from ?: "", trip.to ?: "")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@TripDetailsActivity, "Xatolik bo'ldi", Toast.LENGTH_SHORT).show()
                        binding.btnBook.isEnabled = true
                        binding.btnBook.text = "So'rov yuborish"
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
                    binding.btnBook.setBackgroundColor(Color.parseColor("#4CAF50"))
                } else {
                    // B) Kutilmoqdami?
                    ref.child("requests").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reqSnapshot: DataSnapshot) {
                            if (reqSnapshot.exists()) {
                                binding.btnBook.text = "So'rov yuborilgan ⏳"
                                binding.btnBook.isEnabled = false
                                binding.btnBook.setBackgroundColor(Color.parseColor("#FFC107"))
                            } else {
                                // Hali so'rov yubormagan
                                binding.btnBook.text = "Joy band qilish"
                                binding.btnBook.isEnabled = true
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
        // XML da yangi layoutRequests ID siga o'zgartirganmiz, shuni ishlatamiz
        binding.layoutRequests.visibility = View.VISIBLE
        binding.tvRequestsTitle.text = "Kutilayotgan so'rovlar:"

        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = RequestAdapter(requestList) { request, isAccepted ->
            handleRequestAction(tripId, request, isAccepted)
        }
        binding.rvRequests.adapter = requestAdapter
        binding.rvRequests.isNestedScrollingEnabled = false

        // Bazadan o'qish
        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestList.clear()
                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: "Noma'lum"
                    val phone = child.child("phone").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "pending"

                    // Faqat kutilayotgan (pending) larni ko'rsatamiz
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
            // Qabul qilish
            bookedRef.setValue(true) // Safar ichiga qo'shish
            userBookedRef.setValue(true) // Foydalanuvchi profiliga qo'shish
            requestsRef.removeValue() // So'rovlardan o'chirish
            Toast.makeText(this, "${request.name} qabul qilindi!", Toast.LENGTH_SHORT).show()
        } else {
            // Rad etish
            requestsRef.removeValue()
            Toast.makeText(this, "So'rov rad etildi", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Push Notification (Mavjud funksionallik) ---
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
