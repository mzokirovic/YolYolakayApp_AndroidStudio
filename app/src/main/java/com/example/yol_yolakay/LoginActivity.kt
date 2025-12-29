package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Orqaga qaytish
        binding.btnBack.setOnClickListener { finish() }

        // Ro'yxatdan o'tishga o'tish
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish() // Login oynasini yopamiz
        }

        // Kirish tugmasi
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Iltimos, barcha maydonlarni to'ldiring", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tugmani bloklash
            binding.btnLogin.isEnabled = false
            Toast.makeText(this, "Tekshirilmoqda...", Toast.LENGTH_SHORT).show()

            // Firebase orqali kirish
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Xush kelibsiz!", Toast.LENGTH_SHORT).show()

                        // Asosiy oynaga o'tish
                        val intent = Intent(this, MainActivity::class.java)
                        // Orqaga qaytganda Login oynasiga qaytmaslik uchun
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        binding.btnLogin.isEnabled = true
                        val error = task.exception?.message ?: "Xatolik yuz berdi"
                        Toast.makeText(this, "Kirishda xatolik: $error", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
