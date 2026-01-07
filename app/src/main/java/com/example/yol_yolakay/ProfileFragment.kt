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

        // MUHIM: Tugmalarni eng boshida sozlaymiz, shunda ular hamma uchun ishlaydi
        setupButtons()

        // 1. Agar foydalanuvchi tizimga kirmagan bo'lsa (Mehmon)
        if (auth.currentUser == null) {
            handleGuestMode()
            return
        }

        // 2. Agar tizimga kirgan bo'lsa, ma'lumotlarni yuklaymiz
        loadUserData()
    }

    private fun handleGuestMode() {
        if (_binding == null) return

        binding.tvFullName.text = "Mehmon"
        binding.tvPhone.text = "Tizimga kirmagansiz"

        // O'ZGARTIRILDI: Tugmani o'chirmaymiz, u bosiladigan bo'lishi kerak
        // binding.btnOpenCarInfo.isEnabled = false  <-- BU KERAK EMAS ENDI

        // Chiqish tugmasi
        binding.btnLogout.setOnClickListener {
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return

                // 1. SHAXSIY MA'LUMOTLAR
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Foydalanuvchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                binding.tvFullName.text = "$firstName $lastName"
                binding.tvPhone.text = phone

                val rating = snapshot.child("rating").getValue(String::class.java) ?: "5.0"
                val tripCount = snapshot.child("tripCount").getValue(Int::class.java) ?: 0
                binding.tvRating.text = rating
                binding.tvTripCount.text = tripCount.toString()

                // 2. MASHINA STATUSINI TEKSHIRISH
                val carModel = snapshot.child("carModel").getValue(String::class.java)
                val carColor = snapshot.child("carColor").getValue(String::class.java) ?: ""
                val carNumber = snapshot.child("carNumber").getValue(String::class.java) ?: ""

                // Mashina modeli kiritilganmi?
                if (!carModel.isNullOrEmpty()) {
                    // --- MASHINA BOR (Ma'lumotlarni ko'rsatamiz) ---

                    // Ikonkalar
                    binding.ivCarStatusIcon.setImageResource(R.drawable.ic_car)
                    binding.ivCarStatusIcon.setColorFilter(Color.parseColor("#6366F1")) // Binafsha
                    binding.flCarIconContainer.background.setTint(Color.parseColor("#EEF2FF")) // Och fon

                    // O'ngdagi ikonka -> Yashil Check (✅)
                    binding.ivEditCarIcon.setImageResource(R.drawable.ic_check)
                    binding.ivEditCarIcon.setColorFilter(Color.parseColor("#10B981"))

                    // Matnlar (Yangi ID lar ishlatilmoqda)
                    binding.tvCarModel.text = carModel      // Masalan: Chevrolet Cobalt
                    binding.tvCarColor.text = carColor      // Masalan: Oq
                    binding.tvCarNumber.text = carNumber    // Masalan: 01 A 777 AA

                    // Qo'shimcha qatorlarni ko'rsatamiz
                    binding.tvCarColor.visibility = View.VISIBLE
                    binding.tvCarNumber.visibility = View.VISIBLE

                } else {
                    // --- MASHINA YO'Q (Qo'shish holati) ---

                    // Ikonkalar
                    binding.ivCarStatusIcon.setImageResource(R.drawable.ic_car)
                    binding.ivCarStatusIcon.setColorFilter(Color.parseColor("#94A3B8")) // Kulrang
                    binding.flCarIconContainer.background.setTint(Color.parseColor("#F1F5F9")) // Kulrang fon

                    // O'ngdagi ikonka -> Plyus (+)
                    binding.ivEditCarIcon.setImageResource(R.drawable.ic_add)
                    binding.ivEditCarIcon.setColorFilter(Color.parseColor("#6366F1"))

                    // Matn
                    binding.tvCarModel.text = "Mashina qo'shish"

                    // Qo'shimcha qatorlarni yashiramiz (xunuk ko'rinmasligi uchun)
                    binding.tvCarColor.visibility = View.GONE
                    binding.tvCarNumber.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (context != null) {
                    Toast.makeText(context, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }





    private fun setupButtons() {
        if (_binding == null) return

        // --- YANGI: MASHINA QO'SHISH TUGMASI LOGIKASI ---
        binding.btnOpenCarInfo.setOnClickListener {
            if (auth.currentUser == null) {
                // 1. Agar MEHMON bo'lsa -> LoginActivity ga o'tadi
                Toast.makeText(context, "Avval ro'yxatdan o'ting", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            } else {
                // 2. Agar A'ZO bo'lsa -> CarInfoActivity ga o'tadi
                val intent = Intent(requireContext(), CarInfoActivity::class.java)
                startActivity(intent)
            }
        }

        // Tahrirlash (Buni ham mehmonlar uchun Login ga yo'naltirish mumkin)
        binding.btnEditProfile?.setOnClickListener {
            if (auth.currentUser == null) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            } else {
                Toast.makeText(context, "Tahrirlash oynasi tez orada...", Toast.LENGTH_SHORT).show()
            }
        }

        // Chiqish
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Sozlamalar
        binding.btnSettings?.setOnClickListener {
            Toast.makeText(context, "Sozlamalar bo'limi", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotifications.setOnClickListener {
            // Yangi yaratgan NotificationsActivity ni ochish
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
