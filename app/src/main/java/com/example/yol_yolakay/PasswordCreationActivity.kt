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

        // Oldingi oynalardan kelgan ma'lumotlarni olish
        val firstName = intent.getStringExtra("EXTRA_NAME") ?: ""
        val lastName = intent.getStringExtra("EXTRA_SURNAME") ?: ""
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val dob = intent.getStringExtra("EXTRA_DOB") ?: ""
        val gender = intent.getStringExtra("EXTRA_GENDER") ?: ""

        // 1. Orqaga qaytish tugmasi
        binding.btnBack.setOnClickListener { finish() }

        // 2. Ro'yxatdan o'tish tugmasi (FloatingActionButton)
        binding.btnRegister.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()

            // Parol uzunligini tekshirish (sizning XML da hintda 8 ta deyilgan)
            if (password.length < 8) {
                binding.etPassword.error = "Parol kamida 8 ta belgidan iborat bo'lishi kerak"
                return@setOnClickListener
            }

            // Tugmani vaqtincha bosib bo'lmaydigan qilish (bloklash)
            binding.btnRegister.isEnabled = false

            // Foydalanuvchiga jarayon ketayotganini bildirish (Toast orqali, chunki tugma kichkina)
            Toast.makeText(this, "Ro'yxatdan o'tilmoqda...", Toast.LENGTH_SHORT).show()

            // Ro'yxatdan o'tkazishni boshlash
            registerUser(email, password, firstName, lastName, dob, gender)
        }
    }

    private fun registerUser(email: String, pass: String, name: String, surname: String, dob: String, gender: String) {
        // Firebase Auth orqali akkaunt yaratish
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        val userRef = database.getReference("users").child(userId)

                        // Bazaga to'liq ma'lumot yozish (Crash bo'lmasligi uchun bo'sh joylar bilan)
                        val userData = hashMapOf(
                            "id" to userId,
                            "firstName" to name,
                            "lastName" to surname,
                            "email" to email,
                            "dob" to dob,
                            "gender" to gender,
                            "phone" to "",
                            "profileImage" to ""
                        )

                        userRef.setValue(userData).addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Ro'yxatdan o'tish muvaffaqiyatli! 🎉", Toast.LENGTH_SHORT).show()

                                // Asosiy oynaga o'tish
                                // ✅ YANGI KOD (shunga almashtiring):
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                // Xatolik bo'lsa tugmani qayta yoqamiz
                                binding.btnRegister.isEnabled = true
                                Toast.makeText(this, "Ma'lumotlarni saqlashda xatolik", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Auth xatolik
                    binding.btnRegister.isEnabled = true
                    val errorMsg = task.exception?.message ?: "Noma'lum xatolik"
                    Toast.makeText(this, "Xatolik: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }
}
