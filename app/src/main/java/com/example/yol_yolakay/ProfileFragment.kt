package com.example.yol_yolakay

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase sozlamalari
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Foydalanuvchi ma'lumotlarini yuklash
        loadUserInfo()

        // ---------------------------------------------------------
        // TABLAR LOGIKASI (Siz haqingizda | Hisob)
        // ---------------------------------------------------------

        // 1. "Siz haqingizda" tabini bosganda
        binding.tabAboutYou.setOnClickListener {
            // UI: About qismini ko'rsatish, Accountni yashirish
            binding.layoutAboutYou.visibility = View.VISIBLE
            binding.layoutAccount.visibility = View.GONE

            // Ranglarni o'zgartirish (Aktiv - Moviy, Passiv - Kulrang)
            binding.tvTabAbout.setTextColor(Color.parseColor("#00AFF5"))
            binding.lineTabAbout.visibility = View.VISIBLE

            binding.tvTabAccount.setTextColor(Color.parseColor("#757575"))
            binding.lineTabAccount.visibility = View.INVISIBLE
        }

        // 2. "Hisob" tabini bosganda
        binding.tabAccount.setOnClickListener {
            // UI: Account qismini ko'rsatish, Aboutni yashirish
            binding.layoutAboutYou.visibility = View.GONE
            binding.layoutAccount.visibility = View.VISIBLE

            // Ranglarni o'zgartirish
            binding.tvTabAccount.setTextColor(Color.parseColor("#00AFF5"))
            binding.lineTabAccount.visibility = View.VISIBLE

            binding.tvTabAbout.setTextColor(Color.parseColor("#757575"))
            binding.lineTabAbout.visibility = View.INVISIBLE
        }

        // ---------------------------------------------------------
        // TUGMALAR LOGIKASI
        // ---------------------------------------------------------

        // Chiqish (Log out)
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireActivity(), RegistrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Parol
        binding.btnPassword.setOnClickListener {
            Toast.makeText(context, "Parolni o'zgartirish", Toast.LENGTH_SHORT).show()
        }

        // Bildirishnomalar
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(context, "Bildirishnomalar", Toast.LENGTH_SHORT).show()
        }

        // Qoidalar
        binding.btnTerms.setOnClickListener {
            Toast.makeText(context, "Foydalanish qoidalari", Toast.LENGTH_SHORT).show()
        }

        // Rasm qo'shish
        binding.btnEditPhoto.setOnClickListener {
            Toast.makeText(context, "Rasm yuklash funksiyasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.child("Users").child(userId).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val firstName = snapshot.child("firstName").value.toString()
                        val lastName = snapshot.child("lastName").value.toString()

                        binding.tvUserName.text = "$firstName $lastName"
                    }
                }
                .addOnFailureListener {
                    binding.tvUserName.text = "Foydalanuvchi"
                }
        }
    }
}
