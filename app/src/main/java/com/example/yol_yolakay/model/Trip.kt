package com.example.yol_yolakay.model

import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@IgnoreExtraProperties
@Parcelize // Manual Parcelable o'rniga zamonaviy yechim
data class Trip(
    var id: String? = null,
    var userId: String? = null,
    var from: String? = null,
    var to: String? = null,
    var date: String? = null,
    var time: String? = null,

    // Any ishlatilganda @RawValue kerak, lekin biz buni ViewModel'da hal qilamiz keyinchalik
    var price: @RawValue Any? = null,
    var seats: @RawValue Any? = null,

    var info: String? = null,
    var driverName: String? = "Haydovchi",
    var driverPhone: String? = null,
    var status: String = "active",
    var bookedUsers: HashMap<String, @RawValue Any>? = null
) : Parcelable {

    // --- YORDAMCHI FUNKSIYALAR (LOGIKA MODEL ICHIDA QOLADI) ---

    fun getPriceAsLong(): Long {
        return when (val p = price) {
            is Long -> p
            is Int -> p.toLong()
            is Double -> p.toLong()
            is String -> p.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    fun getSeatsAsInt(): Int {
        return when (val s = seats) {
            is Int -> s
            is Long -> s.toInt()
            is String -> s.toIntOrNull() ?: 1
            is Double -> s.toInt()
            else -> 1
        }
    }
}
