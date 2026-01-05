package com.example.yol_yolakay.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Trip(
    var id: String? = null,
    var userId: String? = null,
    var from: String? = null,
    var to: String? = null,
    var date: String? = null,
    var time: String? = null,

    // Narx va joyni har qanday turda qabul qilamiz (xavfsizlik uchun)
    var price: Any? = null,
    var seats: Any? = null,

    var info: String? = null,
    var driverName: String? = "Haydovchi",
    var driverPhone: String? = null,
    var status: String = "active",
    var bookedUsers: HashMap<String, Boolean>? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Trip::class.java.classLoader),
        parcel.readValue(Trip::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString() ?: "active"
    )

    // --- YORDAMCHI FUNKSIYALAR (Qolgan fayllar uchun zarur) ---

    fun getPriceAsLong(): Long {
        return when (price) {
            is Long -> price as Long
            is Int -> (price as Int).toLong()
            is Double -> (price as Double).toLong()
            is String -> (price as String).replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    fun getSeatsAsInt(): Int {
        return when (seats) {
            is Int -> seats as Int
            is Long -> (seats as Long).toInt()
            is String -> (seats as String).toIntOrNull() ?: 1
            is Double -> (seats as Double).toInt()
            else -> 1
        }
    }
    // ---------------------------------------------------------

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(userId)
        parcel.writeString(from)
        parcel.writeString(to)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeValue(price)
        parcel.writeValue(seats)
        parcel.writeString(info)
        parcel.writeString(driverName)
        parcel.writeString(driverPhone)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Trip> {
        override fun createFromParcel(parcel: Parcel): Trip {
            return Trip(parcel)
        }

        override fun newArray(size: Int): Array<Trip?> {
            return arrayOfNulls(size)
        }
    }
}
