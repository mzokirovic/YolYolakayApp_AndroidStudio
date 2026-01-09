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
import com.example.yol_yolakay.model.UserRequest
import com.example.yol_yolakay.model.PushNotification
import com.example.yol_yolakay.model.Message
import com.example.yol_yolakay.model.NotificationBody
import com.example.yol_yolakay.model.Notification

class TripDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripDetailsBinding
    private var currentTrip: Trip? = null
    private var isPreview: Boolean = false

    private lateinit var requestAdapter: RequestAdapter
    private var requestList = ArrayList<UserRequest>()

    private val myUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar dizayni (Original saqlandi)
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
            // TUZATILDI: SearchResultActivity'dan kelayotgan TRIP_OBJ (Parcelable) ni birinchi tekshiramiz
            // Bu orqali GSON xatoligini oldini olamiz
            currentTrip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("TRIP_OBJ", Trip::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("TRIP_OBJ")
            }

            // Agar TRIP_OBJ bo'sh bo'lsa, eski kalitlarni tekshiramiz (backwards compatibility)
            if (currentTrip == null) {
                val tripJson = intent.getStringExtra("TRIP_JSON")
                if (tripJson != null) {
                    currentTrip = com.google.gson.Gson().fromJson(tripJson, Trip::class.java)
                } else {
                    currentTrip = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("trip_data", Trip::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra("trip_data")
                    }
                }
            }

            isPreview = intent.getBooleanExtra("IS_PREVIEW", false)

            currentTrip?.let { trip ->
                setupUIForUserRole(trip)
            }
        } catch (e: Exception) {
            Log.e("TripDetails", "Data load error: ${e.message}")
        }
    }

    private fun setupUI() {
        val trip = currentTrip ?: return
        try {
            binding.tvFromCity.text = trip.from ?: "Noma'lum"
            binding.tvToCity.text = trip.to ?: "Noma'lum"
            binding.tvDateHeader.text = trip.date ?: ""
            binding.tvStartTime.text = trip.time ?: "--:--"

            // TUZATILDI: Trip modelidagi xavfsiz getPriceAsLong() ishlatildi
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

        binding.btnCallBottom.setOnClickListener {
            val phone = trip.driverPhone
            if (!phone.isNullOrEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
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

        if (trip.userId == myUserId && !isPreview) {
            binding.btnSms.visibility = View.GONE
            binding.btnCallBottom.visibility = View.GONE
            binding.btnBook.visibility = View.VISIBLE
            binding.btnBook.text = "Safarni yakunlash"
            binding.btnBook.setBackgroundResource(R.drawable.bg_button_danger)

            loadRequests(trip.id!!)

            if (trip.status == "completed") {
                binding.btnBook.isEnabled = false
                binding.btnBook.text = "Safar yakunlangan"
                binding.btnBook.setBackgroundResource(R.drawable.bg_button_completed)
            }
            binding.btnBook.setOnClickListener { completeTrip() }
        } else {
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

    private fun completeTrip() {
        val tripId = currentTrip?.id ?: return
        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yakunlanmoqda..."

        FirebaseDatabase.getInstance().getReference("trips")
            .child(tripId).child("status").setValue("completed")
            .addOnSuccessListener {
                Toast.makeText(this, "Safar yakunlandi!", Toast.LENGTH_SHORT).show()
                binding.btnBook.text = "Safar yakunlangan"
                binding.btnBook.setBackgroundResource(R.drawable.bg_button_completed)
            }
            .addOnFailureListener {
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "Safarni yakunlash"
            }
    }

    private fun publishTrip() {
        val trip = currentTrip ?: return
        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuklanmoqda..."

        val databaseRef = FirebaseDatabase.getInstance().getReference("trips")
        val tripId = trip.id ?: databaseRef.push().key ?: return

        // TUZATILDI: getPriceAsLong() va getSeatsAsInt() orqali ma'lumotlar bazaga toza Long/Int bo'lib tushadi
        val tripMap = hashMapOf(
            "id" to tripId,
            "userId" to trip.userId,
            "from" to trip.from,
            "to" to trip.to,
            "date" to trip.date,
            "time" to trip.time,
            "price" to trip.getPriceAsLong(),
            "seats" to trip.getSeatsAsInt(),
            "info" to trip.info,
            "driverName" to trip.driverName,
            "driverPhone" to trip.driverPhone,
            "status" to "active"
        )

        databaseRef.child(tripId).setValue(tripMap)
            .addOnSuccessListener { showSuccessDialog() }
            .addOnFailureListener {
                binding.btnBook.isEnabled = true
                binding.btnBook.text = "E'lonni nashr qilish"
            }
    }

    private fun showSuccessDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Muvaffaqiyatli!")
            .setMessage("E'loningiz muvaffaqiyatli joylashtirildi.")
            .setPositiveButton("Bosh sahifaga qaytish") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setCancelable(false).create().show()
    }

    private fun sendBookingRequest() {
        val trip = currentTrip ?: return
        if (myUserId.isEmpty()) return

        binding.btnBook.isEnabled = false
        binding.btnBook.text = "Yuborilmoqda..."

        FirebaseDatabase.getInstance().getReference("users").child(myUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Yo'lovchi"
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val fullName = "$firstName $lastName".trim()
                    val myPhone = snapshot.child("phone").getValue(String::class.java) ?: ""

                    val request = UserRequest(userId = myUserId, name = fullName, phone = myPhone, status = "pending")

                    FirebaseDatabase.getInstance().getReference("trips")
                        .child(trip.id!!).child("requests").child(myUserId).setValue(request)
                        .addOnSuccessListener {
                            binding.btnBook.text = "So'rov yuborilgan ⏳"
                            binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)
                            sendPushNotificationToDriver(trip.userId ?: "", trip.from ?: "", trip.to ?: "")
                        }
                }
                override fun onCancelled(error: DatabaseError) { binding.btnBook.isEnabled = true }
            })
    }

    private fun checkRequestStatus(tripId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        ref.child("bookedUsers").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.btnBook.text = "Siz qabul qilingansiz ✅"
                    binding.btnBook.isEnabled = false
                    binding.btnBook.setBackgroundResource(R.drawable.bg_button_success)
                } else {
                    ref.child("requests").child(myUserId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(reqSnapshot: DataSnapshot) {
                            if (reqSnapshot.exists()) {
                                binding.btnBook.text = "So'rov yuborilgan ⏳"
                                binding.btnBook.isEnabled = false
                                binding.btnBook.setBackgroundResource(R.drawable.bg_button_pending)
                            } else {
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

    private fun loadRequests(tripId: String) {
        binding.layoutRequests.visibility = View.VISIBLE
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        requestAdapter = RequestAdapter(requestList) { request, isAccepted ->
            handleRequestAction(tripId, request, isAccepted)
        }
        binding.rvRequests.adapter = requestAdapter

        FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestList.clear()
                    for (child in snapshot.children) {
                        child.getValue(UserRequest::class.java)?.let { requestList.add(it) }
                    }
                    requestAdapter.notifyDataSetChanged()
                    binding.tvRequestsTitle.text = if (requestList.isEmpty()) "Yangi so'rovlar yo'q" else "Kutilayotgan so'rovlar:"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleRequestAction(tripId: String, request: UserRequest, isAccepted: Boolean) {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        if (isAccepted) {
            val userData = mapOf("name" to request.name, "phone" to request.phone)
            tripRef.child("bookedUsers").child(request.userId).setValue(userData)
            FirebaseDatabase.getInstance().getReference("users").child(request.userId).child("bookedTrips").child(tripId).setValue(true)
            tripRef.child("requests").child(request.userId).removeValue()
            sendNotification(request.userId, "Safar tasdiqlandi! ✅", "Haydovchi so'rovingizni qabul qildi.", "success")
        } else {
            tripRef.child("requests").child(request.userId).removeValue()
            sendNotification(request.userId, "So'rov rad etildi ❌", "Haydovchi so'rovingizni qabul qila olmadi.", "error")
        }
    }

    private fun sendPushNotificationToDriver(driverId: String, from: String, to: String) {
        FirebaseDatabase.getInstance().getReference("users").child(driverId).child("fcmToken")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val token = snapshot.getValue(String::class.java)
                    if (!token.isNullOrEmpty()) {
                        val push = PushNotification(Message(token, NotificationBody("Yangi buyurtma!", "$from dan $to ga so'rov.")))
                        sendNotification(push)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendNotification(pushNotification: PushNotification) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accessToken = com.example.yol_yolakay.api.AccessToken.getAccessToken(this@TripDetailsActivity)
                if (accessToken != null) {
                    RetrofitInstance.api.postNotification("Bearer $accessToken", pushNotification).execute()
                }
            } catch (e: Exception) { Log.e("FCM", "Error: ${e.message}") }
        }
    }

    private fun sendNotification(userId: String, title: String, message: String, type: String) {
        val notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId)
        val notifId = notifRef.push().key ?: return
        val notification = Notification(notifId, title, message, System.currentTimeMillis(), false, type)
        notifRef.child(notifId).setValue(notification)
    }

    private fun setupUIForUserRole(trip: Trip) {
        if (isPreview) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && trip.userId == currentUserId) {
            binding.btnBook.visibility = View.GONE
            loadRequests(trip.id!!)
        } else {
            binding.layoutRequests.visibility = View.GONE
            if (currentUserId != null) checkRequestStatus(trip.id!!)
        }
    }
}
