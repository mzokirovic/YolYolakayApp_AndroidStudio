package com.example.yol_yolakay.repository

import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class TripRepository {
    private val database = FirebaseDatabase.getInstance().getReference("trips")

    // Barcha safarlarni Coroutine orqali olish (Callback hell dan qutulish)
    suspend fun getAllTrips(): List<Trip> {
        return try {
            val snapshot = database.get().await()
            snapshot.children.mapNotNull { it.getValue(Trip::class.java)?.apply { id = it.key } }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Safar holatini yangilash (markazlashtirilgan)
    suspend fun updateTripStatus(tripId: String, status: String): Boolean {
        return try {
            database.child(tripId).child("status").setValue(status).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
