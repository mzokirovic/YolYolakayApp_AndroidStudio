package com.example.yol_yolakay.model

import android.os.Parcel
import android.os.Parcelable

data class Trip(
    var id: String? = null,
    var userId: String? = null, // <-- YANGI: Haydovchi IDsi (Eng muhim joyi)
    var from: String? = null,
    var to: String? = null,
    var date: String? = null,
    var time: String? = null,
    var price: Long? = 0,
    var seats: Int? = 1,
    var info: String? = null,
    var driverName: String? = "Haydovchi",
    var driverPhone: String? = null,
    var status: String = "active"
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(), // userId ni o'qish
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString() ?: "active"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(userId) // userId ni yozish
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
