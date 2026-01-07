package com.example.yol_yolakay

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.ActivityMainBinding
import com.example.yol_yolakay.fragment.TripsFragment
import com.example.yol_yolakay.model.Notification // Modelni import qilish
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.* // Database importlari
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Fragmentlarni bir marta yaratib olamiz
    private val homeFragment = HomeFragment()
    private val tripsFragment = TripsFragment()
    private val profileFragment = ProfileFragment()

    // YANGI: Kanal ID si
    private val CHANNEL_ID = "yol_yolakay_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- 1. INTERNET TEKSHIRUVI ---
        if (!isInternetAvailable()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Aloqa yo'q")
                .setMessage("Iltimos, internetga ulanganingizni tekshiring. Ilova internetsiz ishlamasligi mumkin.")
                .setPositiveButton("Tushunarli", null)
                .show()
        }

        // Ilova ochilganda HomeFragment tursin
        loadFragment(homeFragment)

        // --- 2. XABARNOMA UCHUN RUXSAT SO'RASH (Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // --- 3. YANGI: BILDIRISHNOMA TIZIMINI ISHGA TUSHIRISH ---
        createNotificationChannel()     // Kanal yaratish
        listenForSystemNotifications()  // Ovozli xabarni tinglash

        // --- 4. TOKENNI BAZAGA SAQLASH ---
        saveFcmToken()

        // Pastki menyuni sozlash
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.nav_add -> {
                    val intent = Intent(this, AddTripActivity::class.java)
                    startActivity(intent)
                    false
                }
                R.id.nav_trips -> {
                    loadFragment(tripsFragment)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Bu sizning eski dialog funksiyangiz (o'z joyida qoldi)
        checkNotifications()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // --- TOKENNI OLISH VA BAZAGA YOZISH ---
    private fun saveFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            val token = task.result
            val ref = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.uid)
                .child("fcmToken")

            ref.setValue(token)
        }
    }

    // --- ESKI FUNKSIYA: DIALOG CHIQARISH (O'ZGARTIRILMADI) ---
    private fun checkNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val myId = currentUser.uid

        // Sizning eski kodingizda "Notifications" (Katta harf bilan) ishlatilgan
        val database = FirebaseDatabase.getInstance().getReference("Notifications").child(myId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val isRead = data.child("isRead").getValue(Boolean::class.java) ?: false

                    if (!isRead) {
                        val message = data.child("message").getValue(String::class.java)
                        val title = data.child("title").getValue(String::class.java) ?: "🔔 Yangi xabar"

                        // Custom Dialog (Sizning kodingiz)
                        val dialogView = layoutInflater.inflate(R.layout.dialog_notification, null)
                        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                            .setView(dialogView)
                            .setCancelable(false)

                        val customDialog = dialogBuilder.create()
                        customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
                        val tvMessage = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogMessage)
                        val btnOk = dialogView.findViewById<android.view.View>(R.id.btnOk)

                        tvTitle.text = title
                        tvMessage.text = message

                        btnOk.setOnClickListener {
                            data.ref.child("isRead").setValue(true)
                            customDialog.dismiss()
                        }

                        try {
                            if (!isFinishing) customDialog.show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        break
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // =========================================================================
    // --- YANGI QO'SHILGAN: TIZIM BILDIRISHNOMASI (OVOZ VA POPUP) ---
    // =========================================================================

    private fun listenForSystemNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Biz TripDetailsActivity da kichik "notifications" dan foydalandik
        val notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId)

        // Faqat eng oxirgi kelgan xabarni tinglaymiz
        notifRef.limitToLast(1).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notif = snapshot.getValue(Notification::class.java) ?: return

                // Faqat oxirgi 5 soniya ichida kelgan xabarlarni chiqaramiz
                // Bu ilovani qayta ochganda eski xabarlar "sayrab" yubormasligi uchun
                val currentTime = System.currentTimeMillis()
                if (currentTime - notif.date < 5000) {
                    showSystemNotification(notif.title ?: "Yangi xabar", notif.message ?: "")
                }
            }
            // Bo'sh metodlar
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showSystemNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bildirishnomani bosganda NotificationsActivity ochiladi
        val intent = Intent(this, NotificationsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Agar ic_notifications bo'lmasa, ic_launcher yoki ic_car ishlating
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Tepadan tushishi uchun
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // Ovoz va vibratsiya
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Yo'l-Yo'lakay Xabarlari"
            val descriptionText = "Safar statusi haqida xabarlar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // =========================================================================

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}
