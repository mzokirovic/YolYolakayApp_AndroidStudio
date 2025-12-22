package com.example.yol_yolakay.model

import java.io.Serializable

/**
 * Bu klass safar (trip) haqidagi barcha ma'lumotlarni o'zida saqlaydi.
 * Serializable interfeysi bu obyektni Activity'lar o'rtasida uzatish uchun kerak.
 */
data class Trip(
    // Asosiy, unikal ID
    var id: String? = null,

    // Yo'nalish
    var from: String? = null,
    var to: String? = null,

    // Vaqt
    var date: String? = null,
    var time: String? = null, // Vaqt matn ko'rinishida ("14:30")

    // Tafsilotlar
    var seats: Int? = null,    // Joylar soni butun son
    var price: Long? = null,   // Narx katta son bo'lishi mumkin
    var info: String? = null   // Qo'shimcha ma'lumot

) : Serializable {
    // Firebase to'g'ri ishlashi uchun bo'sh konstruktor kerak
    constructor() : this(null, null, null, null, null, null, null, null)
}
