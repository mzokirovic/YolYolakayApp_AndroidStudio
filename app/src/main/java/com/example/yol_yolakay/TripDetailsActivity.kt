package com.example.yol_yolakay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.RequestAdapter
import com.example.yol_yolakay.api.RetrofitInstance
import com.example.yol_yolakay.databinding.ActivityTripDetailsBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// ... boshqa importlar ...
import com.example.yol_yolakay.model.UserRequest    // <-- Biz yaratgan model
import com.example.yol_yolakay.model.PushNotification
import com.example.yol_yolakay.model.Message
import com.example.yol_yolakay.model.NotificationBody
import com.example.yol_yolakay.api.NotificationAPI
import com.example.yol_yolakay.model.Notification

// ...


class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    // Haydovchi uchun adapter va ro'yxat
    private lateinit var requestAdapter: RequestAdapter
    private var requestList = ArrayList<UserRequest>()

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
            // 1. Ma'lumotlarni olish (Json yoki Parcelable)
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

            // --- MANA SHU YERGA QO'SHILADI (TUZATILDI) ---
            // currentTrip null emasligini tekshiramiz va funksiyaga uzatamiz
            currentTrip?.let { trip ->
                setupUIForUserRole(trip)
            }
            // ---------------------------------------------

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun setupUI() {
        val trip = currentTrip ?: return

        try {
            binding.tvFromCity.text = trip.from ?: "Noma'lum"
            binding.tvToCity.text = trip.to ?: "Noma'lum"
            binding.tvDateHeader.text = trip.date ?: ""
            binding.tvStartTime.text = trip.time ?: "--:--"

            // Model ichidagi funksiyadan foydalanamiz - eng toza yo'l
            val price = trip.getPriceAsLong()
            val formattedPrice = String.format("%,d", price).replace(",", " ")
            binding.tvPrice.text = "$formattedPrice so'm"

            binding.tvDriverName.text = trip.driverName ?: "Haydovchi"
            binding.tvInfo.text = if (trip.info.isNullOrEmpty()) "Qo'shimcha ma'lumot yo'q" else trip.info

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
        val trip = currentTrip
        if (trip == null) {
            Toast.makeText(this, "Ma'lumotlar yo'qolgan!", Toast.LENGTH_SHORT).show()
            return
        }

        // Tugmani bosilmaydigan qilamiz
        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuklanmoqda..."

        val databaseRef = FirebaseDatabase.getInstance().getReference("trips")
        // Agar ID bo'lmasa yangisini yaratamiz
        val tripId = trip.id ?: databaseRef.push().key ?: return

        // --- YANGI: Trip obyektini oddiy Map ga aylantiramiz ---
        // Bu eng xavfsiz yo'l (qidiruvda muammo bo'lmasligi uchun)
        val tripMap = hashMapOf<String, Any?>(
            "id" to tripId,
            "userId" to trip.userId,
            "from" to trip.from,
            "to" to trip.to,            "date" to trip.date,
            "time" to trip.time,

            // Endi modeldagi funksiyalarni ishlatamiz, chunki 1-bosqichda ularni qo'shdik!
            "price" to trip.getPriceAsLong(),
            "seats" to trip.getSeatsAsInt(),

            "info" to trip.info,
            "driverName" to trip.driverName,
            "driverPhone" to trip.driverPhone,
            "status" to "active"
        )


        databaseRef.child(tripId).setValue(tripMap)
            .addOnSuccessListener {
                // --- Muvaffaqiyatli! Dialogni chaqiramiz ---
                showSuccessDialog()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                // Xatolik bo'lsa tugmani qayta yoqamiz
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "E'lonni nashr qilish"
            }
    }




    // --- Muvaffaqiyat dialogini ko'rsatish ---
    // --- Muvaffaqiyat dialogini ko'rsatish (YANGILANGAN) ---
    private fun showSuccessDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Muvaffaqiyatli!")
            .setMessage("E'loningiz muvaffaqiyatli joylashtirildi.")
            .setPositiveButton("Bosh sahifaga qaytish") { _, _ ->
                // 1. Barcha eski oynalarni yopamiz
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // 2. MainActivity ni ochamiz
                startActivity(intent)

                // 3. Hozirgi oynani tugatamiz
                finish()
            }
            .setCancelable(false) // Bekor qilib bo'lmasin
            .create()

        dialog.show()
    }


    // ----------------------------------------------------------------
    // 1. YO'LOVCHI UCHUN: JOY BAND QILISH (So'rov yuborish)
    // ----------------------------------------------------------------
    private fun sendBookingRequest() {
        val trip = currentTrip ?: return
        if (myUserId.isEmpty()) {
            Toast.makeText(this, "Iltimos, avval tizimga kiring!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuborilmoqda..."

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(myUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Yo'lovchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val fullName = "$firstName $lastName".trim()
                val myPhone = snapshot.child("phone").getValue(String::class.java) ?: ""

                // YANGI MODEL (NotificationModels.kt dagi)
                val request = UserRequest(
                    userId = myUserId,
                    name = fullName,
                    phone = myPhone,
                    status = "pending"
                )

                FirebaseDatabase.getInstance().getReference("trips")
                    .child(trip.id!!)
                    .child("requests")
                    .child(myUserId)
                    .setValue(request)
                    .addOnSuccessListener {
                        Toast.makeText(this@TripDetailsActivity, "So'rov yuborildi! ✅", Toast.LENGTH_LONG).show()
                        binding.btnBook.text = "So'rov yuborilgan ⏳"
                        binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)

                        // Notification yuborish
                        sendPushNotificationToDriver(trip.userId ?: "", trip.from ?: "", trip.to ?: "")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@TripDetailsActivity, "Xatolik bo'ldi", Toast.LENGTH_SHORT).show()
                        binding.btnBook.isEnabled = true
                        binding.btnBook.text = "Joy band qilish"
                        binding.btnBook.setBackgroundResource(R.drawable.bg_gradient_premium)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.btnBook.isEnabled = true
            }
        })
    }

    // ----------------------------------------------------------------
    // 2. STATUSNI TEKSHIRISH (Yo'lovchi uchun)
    // ----------------------------------------------------------------
    private fun checkRequestStatus(tripId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId)

        // A) Qabul qilinganmi?
        ref.child("bookedUsers").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.btnBook.text = "Siz qabul qilingansiz ✅"
                    binding.btnBook.isEnabled = false
                    binding.btnBook.setBackgroundResource(R.drawable.bg_button_success)
                } else {
                    // B) Kutilmoqdami?
                    ref.child("requests").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reqSnapshot: DataSnapshot) {
                            if (reqSnapshot.exists()) {
                                binding.btnBook.text = "So'rov yuborilgan ⏳"
                                binding.btnBook.isEnabled = false
                                binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)
                            } else {
                                // Hali so'rov yubormagan
                                binding.btnBook.text = "Joy band qilish"
                                binding.btnBook.isEnabled = true
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

    // ----------------------------------------------------------------
    // 3. HAYDOVCHI UCHUN: SO'ROVLARNI YUKLASH
    // ----------------------------------------------------------------
    private fun loadRequests(tripId: String) {
        // Layout ko'rinadigan bo'lsin
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
                    val req = child.getValue(UserRequest::class.java)
                    if (req != null) {
                        requestList.add(req)
                    }
                }
                requestAdapter.notifyDataSetChanged()

                if (requestList.isEmpty()) {
                    binding.tvRequestsTitle.text = "Yangi so'rovlar yo'q"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ----------------------------------------------------------------
    // 4. HAYDOVCHI UCHUN: SO'ROVNI QABUL QILISH / RAD ETISH
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    // 4. HAYDOVCHI UCHUN: SO'ROVNI QABUL QILISH / RAD ETISH
    // ----------------------------------------------------------------
    private fun handleRequestAction(tripId: String, request: UserRequest, isAccepted: Boolean) {
        val requestsRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests").child(request.userId)
        val bookedRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("bookedUsers").child(request.userId)
        val userBookedRef = FirebaseDatabase.getInstance().getReference("users").child(request.userId).child("bookedTrips").child(tripId)

        if (isAccepted) {
            // 1. Qabul qilish logikasi
            val userData = mapOf(
                "name" to request.name,
                "phone" to request.phone
            )
            bookedRef.setValue(userData)
            userBookedRef.setValue(true)
            requestsRef.removeValue()

            Toast.makeText(this, "${request.name} qabul qilindi ✅", Toast.LENGTH_SHORT).show()

            // 2. YANGI: Yo'lovchiga "Tasdiqlandi" xabarini yuborish
            sendNotification(
                userId = request.userId,
                title = "Safar tasdiqlandi! ✅",
                message = "Haydovchi sizning so'rovingizni qabul qildi. Safarga tayyorlaning!",
                type = "success"
            )

        } else {
            // 1. Rad etish logikasi
            requestsRef.removeValue()
            Toast.makeText(this, "So'rov rad etildi ❌", Toast.LENGTH_SHORT).show()

            // 2. YANGI: Yo'lovchiga "Rad etildi" xabarini yuborish
            sendNotification(
                userId = request.userId,
                title = "So'rov rad etildi ❌",
                message = "Afsuski, haydovchi so'rovingizni qabul qila olmadi. Boshqa safar qidirib ko'ring.",
                type = "error"
            )
        }
    }


    // ----------------------------------------------------------------
    // 5. TEXNIK QISM: NOTIFICATION YUBORISH (Yangi API bilan)
    // ----------------------------------------------------------------
    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        val database = FirebaseDatabase.getInstance()
        val tokenRef = database.getReference("users").child(driverId).child("fcmToken")

        tokenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)

                if (!token.isNullOrEmpty()) {
                    val title = "Yangi buyurtma!"
                    val body = "$from dan $to ga yangi yo'lovchi so'rov yubordi."

                    // Yangi modellarni ishlatamiz
                    val notificationBody = NotificationBody(title, body)
                    val message = Message(token, notificationBody)
                    val pushNotification = PushNotification(message)

                    sendNotification(pushNotification)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendNotification(pushNotification: PushNotification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Access Tokenni olish
                val accessToken = com.example.yol_yolakay.api.AccessToken.getAccessToken(this@TripDetailsActivity)

                if (accessToken != null) {
                    val call = com.example.yol_yolakay.api.RetrofitInstance.api.postNotification(
                        "Bearer $accessToken",
                        pushNotification
                    )

                    val response = call.execute()

                    if (response.isSuccessful) {
                        Log.d("FCM", "Yuborildi ✅")
                    } else {
                        Log.e("FCM", "Xato: ${response.code()} ${response.errorBody()?.string()}")
                    }
                } else {
                    Log.e("FCM", "Token topilmadi")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Xato: ${e.message}")
            }
        }
    }


    // Bildirishnoma yuborish funksiyasi
    private fun sendNotification(userId: String, title: String, message: String, type: String) {
        val notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId)
        val notifId = notifRef.push().key

        if (notifId != null) {
            val notification = Notification(
                id = notifId,
                title = title,
                message = message,
                date = System.currentTimeMillis(),
                isRead = false,
                type = type
            )
            notifRef.child(notifId).setValue(notification)
        }
    }


    //----------------------------------------------------------------
    // 3. UI NI ROLGA QARAB SOZLASH (HAYDOVCHI / YO'LOVCHI)
    // ----------------------------------------------------------------
    private fun setupUIForUserRole(trip: Trip) {
        // Agar bu "Ko'rib chiqish" (Preview) rejimi bo'lsa, hech narsani o'zgartirmaymiz
        if (isPreview) return

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // 1. Agar men HAYDOVCHI bo'lsam
        if (currentUserId != null && trip.userId == currentUserId) {
            binding.btnBook.visibility = View.GONE         // O'zimdan bron qilmayman

            // "Safarni yakunlash" mantiqini setupButtons da qilgansiz, bu yerda shart emas

            // So'rovlar ro'yxatini yuklaymiz
            loadRequests(trip.id!!)
        }
        // 2. Agar men YO'LOVCHI bo'lsam
        else {
            binding.rvRequests.visibility = View.GONE      // So'rovlarni ko'rmayman
            binding.tvRequestsTitle.visibility = View.GONE
            // Bron holatini tekshirish
            if (currentUserId != null) {
                checkRequestStatus(trip.id!!)
            }
        }
    }


} // <-- BU SINFNI YOPUVCHI OXIRGI QAVS
