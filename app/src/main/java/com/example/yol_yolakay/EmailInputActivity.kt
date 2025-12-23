package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityEmailInputBinding

class EmailInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Oldingi oynadan ism va familiyani qabul qilib olish (agar jo'natilgan bo'lsa)
        val firstName = intent.getStringExtra("EXTRA_NAME")
        val lastName = intent.getStringExtra("EXTRA_SURNAME")

        // Orqaga qaytish
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Radio tugmani bosganda check/uncheck qilish
        binding.radioNewsOptOut.setOnClickListener {
            // Agar kerak bo'lsa mantiq yozish mumkin
        }

        // Davom etish
        binding.btnNext.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            // 1. Email bo'sh emasligini tekshirish
            if (email.isEmpty()) {
                binding.etEmail.error = "Email kiriting"
                return@setOnClickListener
            }

            // 2. Email formati to'g'riligini tekshirish (masalan: test@gmail.com)
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Noto'g'ri email formati"
                return@setOnClickListener
            }

            // Muvaffaqiyatli!
            // Bu yerda keyingi oynaga (masalan PasswordActivity) o'tish kodi bo'ladi.
            // Hozircha vaqtincha Toast chiqaramiz.
            Toast.makeText(this, "Email: $email qabul qilindi", Toast.LENGTH_SHORT).show()

            // YANGI KOD: Sana oynasiga o'tish
            val intent = Intent(this, DateOfBirthActivity::class.java)
            // Oldingi ma'lumotlarni ham o'zi bilan olib ketadi
            intent.putExtra("EXTRA_NAME", firstName)
            intent.putExtra("EXTRA_SURNAME", lastName)
            intent.putExtra("EXTRA_EMAIL", email)
            startActivity(intent)
        }
    }
}
