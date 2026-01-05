package com.example.yol_yolakay

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityCarInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CarInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarInfoBinding

    // Firebase o'zgaruvchilari
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebaseni ishga tushiramiz
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Orqaga qaytish
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Saqlash tugmasi
        binding.btnSaveCar.setOnClickListener {
            val model = binding.etCarModel.text.toString().trim()
            val color = binding.etCarColor.text.toString().trim()
            val number = binding.etCarNumber.text.toString().trim().uppercase()

            if (model.isEmpty() || color.isEmpty() || number.isEmpty()) {
                Toast.makeText(this, "Iltimos, barcha maydonlarni to'ldiring", Toast.LENGTH_SHORT).show()
            } else {
                saveCarDataToFirebase(model, color, number)
            }
        }

        loadCurrentData()
    }

    private fun loadCurrentData() {
        // Eski ma'lumotlarni ko'rsatish (hozircha shart emas, lekin qoldiramiz)
        val prefs = getSharedPreferences("UserCarPrefs", Context.MODE_PRIVATE)
        binding.etCarModel.setText(prefs.getString("car_model", ""))
        binding.etCarColor.setText(prefs.getString("car_color", ""))
        binding.etCarNumber.setText(prefs.getString("car_number", ""))
    }

    private fun saveCarDataToFirebase(model: String, color: String, number: String) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Xatolik: Tizimga kirilmagan!", Toast.LENGTH_SHORT).show()
            return
        }

        // Foydalanuvchi kutib turishi uchun tugmani o'chirib turamiz
        binding.btnSaveCar.isEnabled = false
        binding.btnSaveCar.text = "Saqlanmoqda..."

        // Firebasega yuboriladigan ma'lumotlar
        val carMap = mapOf(
            "carModel" to model,
            "carColor" to color,
            "carNumber" to number,
            "hasCar" to true
        )

        // 1. Firebase Realtime Database ga yozish
        database.getReference("users").child(userId).updateChildren(carMap)
            .addOnSuccessListener {

                // 2. Lokal xotiraga ham saqlab qo'yamiz (zaxira uchun)
                val prefs = getSharedPreferences("UserCarPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("car_model", model)
                editor.putString("car_color", color)
                editor.putString("car_number", number)
                editor.putBoolean("has_car", true)
                editor.apply()

                Toast.makeText(this, "Mashina saqlandi! ✅", Toast.LENGTH_SHORT).show()

                // Oynani yopish
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                // Xatolik bo'lsa
                binding.btnSaveCar.isEnabled = true
                binding.btnSaveCar.text = "Saqlash"
                Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
