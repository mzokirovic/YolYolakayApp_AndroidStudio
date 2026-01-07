package com.example.yol_yolakay.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.R
import com.example.yol_yolakay.databinding.ItemNotificationBinding
import com.example.yol_yolakay.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifications[position]
        val binding = holder.binding

        // 1. Matnlarni o'rnatish
        binding.tvTitle.text = notif.title
        binding.tvMessage.text = notif.message

        // 2. Vaqtni formatlash (masalan: 14:30)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvTime.text = sdf.format(Date(notif.date))

        // 3. Statusga qarab rang va ikonkani o'zgartirish
        if (notif.type == "success") {
            // Yashil (Qabul qilindi)
            binding.iconContainer.background.setTint(Color.parseColor("#DCFCE7")) // Och yashil
            binding.ivNotifIcon.setImageResource(R.drawable.ic_check)
            binding.ivNotifIcon.setColorFilter(Color.parseColor("#16A34A")) // To'q yashil
        } else if (notif.type == "error") {
            // Qizil (Rad etildi)
            binding.iconContainer.background.setTint(Color.parseColor("#FEE2E2")) // Och qizil
            binding.ivNotifIcon.setImageResource(R.drawable.ic_logout) // Yoki x ikonka
            binding.ivNotifIcon.setColorFilter(Color.parseColor("#DC2626")) // To'q qizil
        } else {
            // Oddiy (Info)
            binding.iconContainer.background.setTint(Color.parseColor("#F1F5F9"))
            binding.ivNotifIcon.setImageResource(R.drawable.ic_notifications)
            binding.ivNotifIcon.setColorFilter(Color.parseColor("#64748B"))
        }

        // 4. O'qilmagan nuqtasini ko'rsatish
        if (!notif.isRead) {
            binding.viewUnreadDot.visibility = View.VISIBLE
            binding.tvTitle.setTextColor(Color.BLACK) // Qalin qora
        } else {
            binding.viewUnreadDot.visibility = View.GONE
            binding.tvTitle.setTextColor(Color.parseColor("#475569")) // Biroz ochroq
        }

        // 5. Bosilganda
        holder.itemView.setOnClickListener {
            onNotificationClick(notif)
        }
    }

    override fun getItemCount(): Int = notifications.size
}
