package com.example.yol_yolakay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Fragmentlarni bir marta yaratib olamiz
    private val homeFragment = HomeFragment()
    private val tripsFragment = TripsFragment()
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ilova ochilganda HomeFragment tursin
        loadFragment(homeFragment)

        // --- 1. XABARNOMA UCHUN RUXSAT SO'RASH (Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // --- 2. TOKENNI BAZAGA SAQLASH ---
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

            // Yangi tokenni olamiz
            val token = task.result

            // Uni bazaga "Users -> userID -> fcmToken" ga yozamiz
            val ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.uid)
                .child("fcmToken")

            ref.setValue(token)
        }
    }

    // --- XABARLARNI TEKSHIRISH ---
    // --- XABARLARNI TEKSHIRISH (YANGILANGAN DIZAYN BILAN) ---
    private fun checkNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val myId = currentUser.uid

        val database = FirebaseDatabase.getInstance().getReference("Notifications").child(myId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val isRead = data.child("isRead").getValue(Boolean::class.java) ?: false

                    if (!isRead) {
                        val message = data.child("message").getValue(String::class.java)
                        val title = data.child("title").getValue(String::class.java) ?: "🔔 Yangi xabar"

                        // --- CHIROYLI CUSTOM DIALOG YARATISH ---
                        val dialogView = layoutInflater.inflate(R.layout.dialog_notification, null)

                        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                            .setView(dialogView)
                            .setCancelable(false) // Foydalanuvchi majbur OK bosishi kerak

                        val customDialog = dialogBuilder.create()

                        // Dialog fonini shaffof qilish (burchaklar yumaloq ko'rinishi uchun)
                        customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                        // View elementlarini topish
                        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
                        val tvMessage = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogMessage)
                        val btnOk = dialogView.findViewById<android.view.View>(R.id.btnOk)

                        // Ma'lumotlarni qo'yish
                        tvTitle.text = title
                        tvMessage.text = message

                        // OK tugmasi bosilganda
                        btnOk.setOnClickListener {
                            // Xabarni o'qildi deb belgilash
                            data.ref.child("isRead").setValue(true)
                            customDialog.dismiss()
                        }

                        customDialog.show()

                        // Bir vaqtning o'zida faqat bitta xabar chiqsin (loopni to'xtatish)
                        // Agar xohlasangiz, buni olib tashlab, hammasini ketma-ket chiqarish mumkin
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

}
