package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // 1. Agar foydalanuvchi tizimga kirmagan bo'lsa
        if (auth.currentUser == null) {
            handleGuestMode()
            return
        }

        // 2. Ma'lumotlarni yuklash
        loadUserData()

        // 3. Tugmalarni sozlash
        setupButtons()
    }

    private fun handleGuestMode() {
        if (_binding == null) return

        binding.tvFullName.text = "Mehmon"
        binding.tvPhone.text = "Tizimga kirmagansiz"

        // Inputlarni o'chirib qo'yamiz
        binding.etCarModel.isEnabled = false
        binding.etCarColor.isEnabled = false
        binding.etCarNumber.isEnabled = false

        // Yangi dizaynda btnSaveCar (kichik yashil tugma) bor, katta btnSave yo'q.
        // Xavfsizlik uchun '?' qo'yamiz.
        binding.btnSaveCar?.isEnabled = false
        binding.btnSaveCar?.alpha = 0.5f

        // Chiqish tugmasi (layout) o'rniga "Kirish" tugmasini ishlatamiz
        binding.btnLogout.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("Users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return

                // Shaxsiy ma'lumotlar
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Foydalanuvchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: "+998 -- --- -- --"

                binding.tvFullName.text = "$firstName $lastName"
                binding.tvPhone.text = phone

                // Statistika
                val rating = snapshot.child("rating").getValue(String::class.java) ?: "5.0"
                val tripCount = snapshot.child("tripCount").getValue(Int::class.java) ?: 0

                binding.tvRating.text = rating
                binding.tvTripCount.text = tripCount.toString()

                // Mashina ma'lumotlari (bularni EditTextga qo'yamiz)
                val carModel = snapshot.child("carModel").getValue(String::class.java) ?: ""
                val carColor = snapshot.child("carColor").getValue(String::class.java) ?: ""
                val carNumber = snapshot.child("carNumber").getValue(String::class.java) ?: ""

                // Faqat foydalanuvchi yozayotgan paytda o'zgarib ketmasligi uchun tekshiramiz
                if (!binding.etCarModel.hasFocus()) binding.etCarModel.setText(carModel)
                if (!binding.etCarColor.hasFocus()) binding.etCarColor.setText(carColor)
                if (!binding.etCarNumber.hasFocus()) binding.etCarNumber.setText(carNumber)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xatolikni jimgina o'tkazib yuboramiz
            }
        })
    }

    private fun setupButtons() {
        // SAQLASH TUGMASI (Yangi dizayn: btnSaveCar)
        // Agar btnSaveCar topilmasa, kod qulamaydi (safe call)
        binding.btnSaveCar?.setOnClickListener {
            saveCarDetails()
        }

        // Tahrirlash (Headerdagi yozuv)
        binding.btnEditProfile?.setOnClickListener {
            Toast.makeText(context, "Tahrirlash oynasi tez orada...", Toast.LENGTH_SHORT).show()
        }

        // CHIQISH TUGMASI
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            // Login oynasiga qaytish
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // SOZLAMALAR (Eski btnLanguage o'rniga btnSettings)
        binding.btnSettings?.setOnClickListener {
            Toast.makeText(context, "Sozlamalar bo'limi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCarDetails() {
        val userId = auth.currentUser?.uid ?: return

        val model = binding.etCarModel.text.toString().trim()
        val color = binding.etCarColor.text.toString().trim()
        val number = binding.etCarNumber.text.toString().trim()

        if (model.isEmpty() && color.isEmpty() && number.isEmpty()) {
            Toast.makeText(context, "Hech bo'lmasa bitta ma'lumot kiriting", Toast.LENGTH_SHORT).show()
            return
        }

        // Animatsiya: Tugmani vaqtincha o'chirib turamiz
        binding.btnSaveCar?.isEnabled = false
        binding.btnSaveCar?.alpha = 0.5f

        val updates = mapOf<String, Any>(
            "carModel" to model,
            "carColor" to color,
            "carNumber" to number
        )

        database.getReference("Users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(context, "Mashina ma'lumotlari saqlandi! ✅", Toast.LENGTH_SHORT).show()

                    // Tugmani qayta yoqamiz
                    binding.btnSaveCar?.isEnabled = true
                    binding.btnSaveCar?.alpha = 1.0f

                    // Klaviatura yopilishi va fokus yo'qolishi uchun
                    binding.etCarModel.clearFocus()
                    binding.etCarColor.clearFocus()
                    binding.etCarNumber.clearFocus()
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    Toast.makeText(context, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
                    binding.btnSaveCar?.isEnabled = true
                    binding.btnSaveCar?.alpha = 1.0f
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
