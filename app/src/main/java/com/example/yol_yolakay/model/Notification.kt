package com.example.yol_yolakay.model

data class Notification(
    var id: String? = null,
    var title: String? = null,
    var message: String? = null,
    var date: Long = System.currentTimeMillis(), // Vaqtni saqlash uchun
    var isRead: Boolean = false, // O'qilgan/O'qilmagan statusi
    var type: String = "info" // info, success, error (turli ranglar uchun)
)
