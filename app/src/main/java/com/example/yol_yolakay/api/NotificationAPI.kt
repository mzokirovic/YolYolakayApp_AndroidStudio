package com.example.yol_yolakay.api

// Biz boya yaratgan yangi modelni chaqiramizimport com.example.yol_yolakay.model.PushNotification
import com.example.yol_yolakay.model.PushNotification
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotificationAPI {
    // Bu yangi HTTP v1 manzili
    // "myapp1f" o'rniga o'z Project ID ingizni yozishingiz kerak bo'ladi (keyinroq tushuntiraman)
    @POST("v1/projects/myapp1f/messages:send")
    fun postNotification(
        @Header("Authorization") accessToken: String,
        @Body notification: PushNotification
    ): Call<ResponseBody>
}
