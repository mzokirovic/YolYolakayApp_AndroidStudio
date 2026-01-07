package com.example.yol_yolakay.model

// 1. Notificationning asosiy qutisi
data class PushNotification(
    val message: Message
)

// 2. Xabar ichidagi ma'lumot
data class Message(
    val token: String,
    val notification: NotificationBody
)

// 3. Xabar sarlavhasi va matni
data class NotificationBody(
    val title: String,
    val body: String
)

// 4. Yo'lovchi so'rovi (Ism, telefon, status)
data class UserRequest(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val status: String = "pending"
)
