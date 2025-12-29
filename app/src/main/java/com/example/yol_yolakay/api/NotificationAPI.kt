package com.example.yol_yolakay.api

import com.example.yol_yolakay.model.PushNotification

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationAPI {

    // Firebase Console -> Project Settings -> Cloud Messaging -> Cloud Messaging API (Legacy)
    // Server Key ni shu yerga qo'ying:
    @Headers("Authorization: key=BFQUlU1aHO_I4mlaAHnlbglU2TvXwrpAxsYCLQ2OXUKXZfibHSLjRowFpSpEjSR_ToaasMaGI43rcLYvR2rQf0s", "Content-Type: application/json")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}
