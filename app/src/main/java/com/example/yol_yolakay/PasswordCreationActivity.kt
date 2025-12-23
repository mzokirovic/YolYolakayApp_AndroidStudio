package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityPasswordCreationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PasswordCreationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordCreationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase initsializatsiya
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Oldingi oynalardan ma'lumotlarni olish
        val firstName = intent.getStringExtra("EXTRA_NAME") ?: ""
        val lastName = intent.getStringExtra("EXTRA_SURNAME") ?: ""
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val dob = intent.getStringExtra("EXTRA_DOB") ?: ""
        val gender = intent.getStringExtra("EXTRA_GENDER") ?: ""

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()

            // "Confirm Password" qismini olib tashladik, chunki rasmda faqat bitta maydon bor

            if (password.length < 8) {
                binding.etPassword.error = "Parol kamida 8 ta belgidan iborat bo'lishi kerak"
                return@setOnClickListener
            }

            // Ro'yxatdan o'tkazishni boshlash
            registerUser(email, password, firstName, lastName, dob, gender)
        }
    }

    private fun registerUser(email: String, pass: String, name: String, surname: String, dob: String, gender: String) {
        // 1. Firebase Auth orqali akkaunt yaratish
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Akkaunt yaratildi, endi qo'shimcha ma'lumotlarni Databasega yozamiz
                    val userId = auth.currentUser?.uid
                    val userRef = database.getReference("Users").child(userId!!)

                    val userData = hashMapOf(
                        "firstName" to name,
                        "lastName" to surname,
                        "email" to email,
                        "dob" to dob,
                        "gender" to gender
                    )

                    userRef.setValue(userData).addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) {
                            Toast.makeText(this, "Ro'yxatdan o'tish muvaffaqiyatli!", Toast.LENGTH_SHORT).show()

                            // Asosiy oynaga o'tish va barcha activitylarni yopish
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Ma'lumotlarni saqlashda xatolik", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    // Xatolik bo'lsa
                    Toast.makeText(this, "Xatolik: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
