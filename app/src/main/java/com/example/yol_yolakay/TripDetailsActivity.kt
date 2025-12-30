package com.example.yol_yolakay

import android.content.Intent
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
import java.util.UUID

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    // Yangi: Haydovchi uchun adapter va ro'yxat
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

    // --- ASOSIY O'ZGARISH: Tugmalar va Mantiq ---
    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        val trip = currentTrip ?: return

        // 1. TELEFON QILISH LOGIKASI
        val callListener = View.OnClickListener {
            val phone = trip.driverPhone
            if (!phone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Raqam yo'q", Toast.LENGTH_SHORT).show()
            }
        }
        // Pastdagi yashil tugma
        binding.btnCallBottom.setOnClickListener(callListener)

        // 2. SMS YOZISH LOGIKASI
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
            binding.layoutPassengerActions.visibility = View.GONE // SMS/Call/Book yashiramiz
            binding.btnCompleteTrip.visibility = View.VISIBLE // Yakunlash tugmasi chiqadi

            // So'rovlar ro'yxatini yuklaymiz
            loadRequests(trip.id!!)

            if (trip.status == "completed") {
                binding.btnCompleteTrip.isEnabled = false
                binding.btnCompleteTrip.text = "Safar yakunlangan"
                binding.btnCompleteTrip.setBackgroundColor(android.graphics.Color.GRAY)
            }

            binding.btnCompleteTrip.setOnClickListener {
                completeTrip()
            }

        } else {
            // B) AGAR MEN YO'LOVCHI BO'LSAM (yoki Preview)
            binding.btnCompleteTrip.visibility = View.GONE
            binding.layoutPassengerActions.visibility = View.VISIBLE

            if (isPreview) {
                // Preview rejimi (E'lon berishdan oldin ko'rish)
                binding.btnBook.text = "E'lonni nashr qilish"
                binding.btnBook.setOnClickListener { publishTrip() }
                binding.btnSms.visibility = View.GONE
                binding.btnCallBottom.visibility = View.GONE
            } else {
                // Oddiy Yo'lovchi rejimi: Statusni tekshiramiz
                checkRequestStatus(trip.id!!)
            }
        }
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
                    .child("requests") // requests papkasiga yozamiz
                    .child(myUserId)
                    .setValue(requestMap)
                    .addOnSuccessListener {
                        Toast.makeText(this@TripDetailsActivity, "So'rov yuborildi! ✅", Toast.LENGTH_LONG).show()
                        binding.btnBook.text = "So'rov yuborilgan ⏳"
                        binding.btnBook.setBackgroundColor(android.graphics.Color.parseColor("#FFC107")) // Sariq

                        // Haydovchiga push xabar
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

        // A) Band qilinganlar ro'yxatini tekshirish (Qabul qilinganmi?)
        ref.child("bookedUsers").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Qabul qilingan!
                    binding.btnBook.text = "Siz qabul qilingansiz ✅"
                    binding.btnBook.isEnabled = false
                    binding.btnBook.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    // B) Agar qabul qilinmagan bo'lsa, "requests" ni tekshiramiz (Kutilmoqdami?)
                    ref.child("requests").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reqSnapshot: DataSnapshot) {
                            if (reqSnapshot.exists()) {
                                binding.btnBook.text = "So'rov yuborilgan ⏳"
                                binding.btnBook.isEnabled = false
                                binding.btnBook.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"))
                            } else {
                                // Hali so'rov yubormagan
                                binding.btnBook.text = "So'rov yuborish"
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
    // --- 3. HAYDOVCHI: So'rovlarni yuklash (TUZATILGAN VERSIYA) ---
    private fun loadRequests(tripId: String) {
        // 1. Ekranda joy ajratamiz (Avvalboshdan ko'rinib tursin)
        binding.layoutRequests.visibility = View.VISIBLE
        binding.tvRequestsTitle.text = "So'rovlar yuklanmoqda..."

        // RecyclerViewni sozlash
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = RequestAdapter(requestList) { request, isAccepted ->
            handleRequestAction(tripId, request, isAccepted)
        }
        binding.rvRequests.adapter = requestAdapter
        binding.rvRequests.isNestedScrollingEnabled = false

        // 2. Bazadan o'qish
        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestList.clear()

                // LOG: Tekshirish uchun console ga yozamiz
                Log.d("TripDetails", "Topilgan so'rovlar soni: ${snapshot.childrenCount}")

                for (child in snapshot.children) {
                    val userId = child.key ?: ""
                    val name = child.child("name").getValue(String::class.java) ?: "Noma'lum"
                    val phone = child.child("phone").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java)

                    // Faqat "pending" (kutilayotgan) so'rovlarni qo'shamiz
                    if (status == "pending") {
                        requestList.add(UserRequest(userId, name, phone))
                    }
                }
                requestAdapter.notifyDataSetChanged()

                // 3. Natijani ko'rsatish
                if (requestList.isNotEmpty()) {
                    binding.tvRequestsTitle.text = "Kutilayotgan so'rovlar (${requestList.size}):"
                } else {
                    // Agar bo'sh bo'lsa ham YASHIRMAYMIZ, shunchaki xabar yozamiz
                    binding.tvRequestsTitle.text = "Hozircha yangi so'rovlar yo'q"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TripDetails", "Xatolik: ${error.message}")
                Toast.makeText(this@TripDetailsActivity, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // --- 4. HAYDOVCHI: Qabul qilish yoki Rad etish ---
    // --- 4. HAYDOVCHI: Qabul qilish yoki Rad etish (YANGILANGAN) ---
    private fun handleRequestAction(tripId: String, request: UserRequest, isAccepted: Boolean) {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)

        if (isAccepted) {
            // 1. Joriy joylar sonini tekshiramiz
            tripRef.child("seats").get().addOnSuccessListener { snapshot ->
                val currentSeats = snapshot.getValue(Int::class.java) ?: 0

                if (currentSeats > 0) {
                    // Joy bor, qabul qilamiz

                    // A) BookedUsers ga qo'shish
                    tripRef.child("bookedUsers").child(request.userId).setValue(true)

                    // B) Yo'lovchining profiliga yozish
                    val userTripsRef = FirebaseDatabase.getInstance().getReference("Users").child(request.userId).child("bookedTrips")
                    userTripsRef.child(tripId).setValue(true)

                    // C) Requests dan o'chirish
                    tripRef.child("requests").child(request.userId).removeValue()

                    // D) JOY SONINI 1 TAGA KAMAYTIRISH
                    tripRef.child("seats").setValue(currentSeats - 1)

                    Toast.makeText(this, "${request.name} qabul qilindi! ✅", Toast.LENGTH_SHORT).show()

                    // Ekranni yangilash (seats o'zgargani uchun)
                    binding.tvInfo.append("\n(1 ta joy band qilindi)")
                } else {
                    // Joy qolmagan bo'lsa
                    Toast.makeText(this, "Afsuski, bo'sh joy qolmadi! ❌", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Rad etish: Faqat Requests dan o'chirish
            tripRef.child("requests").child(request.userId).removeValue()
            Toast.makeText(this, "Rad etildi ❌", Toast.LENGTH_SHORT).show()
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
                val ref = FirebaseDatabase.getInstance().getReference("trips").child(trip.id!!)
                ref.child("status").setValue("completed")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Safar yakunlandi! 🏁", Toast.LENGTH_SHORT).show()
                        finish()
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

    // --- PUSH XABAR YUBORISH ---
    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        if (driverId.isEmpty()) return

        val database = FirebaseDatabase.getInstance().getReference("Users").child(driverId).child("fcmToken")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)

                if (!token.isNullOrEmpty()) {
                    val title = "Yangi so'rov! 🙋‍♂️"
                    val message = "$from - $to safaringizga qo'shilish so'rovi keldi."

                    val notificationData = NotificationData(title, message)
                    val pushNotification = PushNotification(notificationData, token)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            RetrofitInstance.api.postNotification(pushNotification)
                        } catch (e: Exception) {
                            Log.e("FCM", "Xatolik: ${e.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
