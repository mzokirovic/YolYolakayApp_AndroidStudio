package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.NotificationsAdapter
import com.example.yol_yolakay.databinding.ActivityNotificationsBinding
import com.example.yol_yolakay.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val notificationList = ArrayList<Notification>()
    private lateinit var adapter: NotificationsAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadNotifications()

        // Orqaga qaytish tugmasi
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Adapter yaratish va bosilganda o'qilgan (markAsRead) deb belgilash
        adapter = NotificationsAdapter(notificationList) { notification ->
            markAsRead(notification)
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        val notifRef = database.getReference("notifications").child(userId)

        // Firebase'dan o'zgarishlarni tinglash
        notifRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                for (child in snapshot.children) {
                    val notif = child.getValue(Notification::class.java)
                    if (notif != null) {
                        notificationList.add(notif)
                    }
                }

                // Ro'yxatni teskari aylantirish (eng yangisi tepada turishi uchun)
                notificationList.reverse()
                adapter.notifyDataSetChanged()

                // Agar ro'yxat bo'sh bo'lsa, "Bo'sh" rasmini ko'rsatish
                if (notificationList.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xatolik yuz bersa (shart emas, lekin log uchun yaxshi)
            }
        })
    }

    private fun markAsRead(notification: Notification) {
        if (notification.isRead) return // Agar allaqachon o'qilgan bo'lsa, qaytamiz

        val userId = auth.currentUser?.uid ?: return
        val notifId = notification.id ?: return

        // Firebase'da 'isRead' ni true qilib yangilash
        database.getReference("notifications")
            .child(userId)
            .child(notifId)
            .child("isRead")
            .setValue(true)
    }
}
