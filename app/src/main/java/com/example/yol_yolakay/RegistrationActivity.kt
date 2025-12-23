package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityRegistrationBinding

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Orqaga qaytish
        binding.btnBack.setOnClickListener {
            finish()
        }

        // ... (RegistrationActivity.kt ichida)

        // Davom etish
        binding.btnNext.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val surname = binding.etSurname.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = "Ismingizni kiriting"
                return@setOnClickListener
            }
            if (surname.isEmpty()) {
                binding.etSurname.error = "Familiyangizni kiriting"
                return@setOnClickListener
            }

            // YANGI KOD: Email oynasiga o'tish va ismlarni ham olib ketish
            val intent = Intent(this, EmailInputActivity::class.java)
            intent.putExtra("EXTRA_NAME", name)
            intent.putExtra("EXTRA_SURNAME", surname)
            startActivity(intent)
        }

    }
}
