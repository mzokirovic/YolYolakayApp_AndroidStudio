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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "Xatolik: User ID topilmadi", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = database.getReference("users").child(userId)

        // Diagnostika uchun Toast
        // Toast.makeText(context, "Ma'lumotlar yuklanmoqda...", Toast.LENGTH_SHORT).show()

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return

                // 1. SHAXSIY MA'LUMOTLARNI YUKLASH
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

                // TEST UCHUN: Ekranga ma'lumotni chiqaramiz
                // Agar carModel null bo'lsa "Bo'sh", aks holda mashina nomini chiqaradi
                // Toast.makeText(context, "Firebase'dan keldi: ${carModel ?: "Bo'sh"}", Toast.LENGTH_LONG).show()

                if (!carModel.isNullOrEmpty()) {
                    // --- MASHINA BOR -> PTICHKA ---
                    binding.tvCarInfoTitle.text = carModel
                    binding.ivCarStatusIcon.setImageResource(R.drawable.ic_check)
                    binding.ivCarStatusIcon.setColorFilter(Color.WHITE)
                    binding.ivCarStatusIcon.background.setTint(Color.parseColor("#10B981")) // Yashil
                } else {
                    // --- MASHINA YO'Q -> PLYUS ---
                    binding.tvCarInfoTitle.text = "Mening mashinam"
                    binding.ivCarStatusIcon.setImageResource(R.drawable.ic_add)
                    binding.ivCarStatusIcon.setColorFilter(Color.parseColor("#2E5BFF"))
                    binding.ivCarStatusIcon.background.setTint(Color.parseColor("#EFF6FF")) // Ko'k
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // XATOLIK BO'LSA EKRANGA CHIQARAMIZ
                Toast.makeText(context, "Firebase Xatosi: ${error.message}", Toast.LENGTH_LONG).show()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
