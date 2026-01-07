package com.example.yol_yolakay.api

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Collections

object AccessToken {

    // DIQQAT: Bu yerga Firebase Konsoldan olingan JSON fayl nomini yozish kerak
    // Hozircha oddiy placeholder yozib turamiz, keyinroq JSON ni qo'shasiz.
    // Agar JSON faylingiz bo'lmasa, bu kod ishlamaydi, lekin qizil xato yo'qoladi.

    suspend fun getAccessToken(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                // "service-account.json" faylini 'res/raw' papkasiga qo'yish kerak bo'ladi
                // Hozircha ilova qulamasligi uchun shunchaki null qaytaramiz yoki dummy token
                // Haqiqiy loyihada bu yerda GoogleCredentials ishlatiladi.

                // Vaqtinchalik yechim (Google serverisiz ishlashi uchun):
                // Agar sizda Service Account JSON fayli bo'lmasa, buni sinab ko'rish qiyin.
                // Lekin xatoni yo'qotish uchun quyidagicha yozamiz:

                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier("service_account", "raw", context.packageName)
                )

                val googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"))

                googleCredentials.refreshIfExpired()
                googleCredentials.accessToken.tokenValue

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
