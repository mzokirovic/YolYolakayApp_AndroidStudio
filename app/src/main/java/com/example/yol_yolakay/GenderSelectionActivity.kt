package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityGenderSelectionBinding

class GenderSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenderSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenderSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Oldingi oynalardan kelgan ma'lumotlarni qabul qilish
        val firstName = intent.getStringExtra("EXTRA_NAME")
        val lastName = intent.getStringExtra("EXTRA_SURNAME")
        val email = intent.getStringExtra("EXTRA_EMAIL")
        val dob = intent.getStringExtra("EXTRA_DOB")

        // Orqaga qaytish
        binding.btnBack.setOnClickListener { finish() }

        // 1. Ayol (Mrs. / Ms.)
        binding.optionFemale.setOnClickListener {
            goToNextScreen(firstName, lastName, email, dob, "Female")
        }

        // 2. Erkak (Mr.)
        binding.optionMale.setOnClickListener {
            goToNextScreen(firstName, lastName, email, dob, "Male")
        }

        // 3. Aytishni xohlamayman
        binding.optionNone.setOnClickListener {
            goToNextScreen(firstName, lastName, email, dob, "NotSpecified")
        }
    }

    private fun goToNextScreen(name: String?, surname: String?, email: String?, dob: String?, gender: String) {
        // Tanlangan jinsni ko'rsatish (Test uchun)
        Toast.makeText(this, "Tanlandi: $gender", Toast.LENGTH_SHORT).show()

        // Keyingi oyna (Password yaratish)
        val intent = Intent(this, PasswordCreationActivity::class.java) // Hali yaratilmagan bo'lsa qizil bo'ladi

        intent.putExtra("EXTRA_NAME", name)
        intent.putExtra("EXTRA_SURNAME", surname)
        intent.putExtra("EXTRA_EMAIL", email)
        intent.putExtra("EXTRA_DOB", dob)
        intent.putExtra("EXTRA_GENDER", gender)

        startActivity(intent)
    }
}
