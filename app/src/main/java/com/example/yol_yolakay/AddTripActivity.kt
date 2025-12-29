package com.example.yol_yolakay

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityAddTripBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class AddTripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripBinding
    private lateinit var auth: FirebaseAuth
    // Database referenceni har doim to'g'ri olish uchun funksiya ichida chaqiramiz

    // Hozirgi qadam (1 dan 7 gacha)
    private var currentStep = 1

    // Ma'lumotlarni vaqtincha saqlash
    private var fromCity: String = ""
    private var toCity: String = ""
    private var tripDate: String = ""
    private var tripTime: String = ""

    // --- Shahar tanlash uchun Launcherlar ---
    private val startLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etFrom.setText(city)
                binding.etFrom.error = null
            }
        }
    }

    private val endLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etTo.setText(city)
                binding.etTo.error = null
            }
        }
    }
    // -----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupUI()
    }

    private fun setupUI() {
        // Boshlang'ich holat: faqat 1-qadam ko'rinadi
        updateStepVisibility()

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepVisibility()
            } else {
                finish()
            }
        }

        // --- Shahar tanlash oynalarini ulash ---
        // 1-qadam: Qayerdan
        binding.etFrom.isFocusable = false
        binding.etFrom.setOnClickListener {
            val intent = Intent(this, CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "FROM")
            startLocationLauncher.launch(intent)
        }

        // 2-qadam: Qayerga
        binding.etTo.isFocusable = false
        binding.etTo.setOnClickListener {
            val intent = Intent(this, CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "TO")
            endLocationLauncher.launch(intent)
        }
        // ----------------------------------------------

        // Sana tanlash
        binding.etDate.isFocusable = false
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Vaqt tanlash
        binding.etTime.isFocusable = false
        binding.etTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < 7) {
                    // Ma'lumotlarni saqlab olamiz
                    saveStepData()
                    currentStep++
                    updateStepVisibility()
                } else {
                    // Oxirgi qadamda Firebasega yozamiz
                    checkProfileAndProceed()
                }
            }
        }
    }


    private fun updateStepVisibility() {
        // Barchasini yashirish
        binding.step1From.visibility = View.GONE
        binding.step2To.visibility = View.GONE
        binding.step3Date.visibility = View.GONE
        binding.step4Time.visibility = View.GONE
        binding.step5Seats.visibility = View.GONE
        binding.step6Price.visibility = View.GONE
        binding.step7Info.visibility = View.GONE

        // --- MUKAMMAL SILLIQ ANIMATSIYA ---
        binding.progressBar.max = 1000

        // Har bir qadam ~142.8 ball (1000 / 7)
        val targetProgress = (currentStep * 142.8).toInt()

        android.animation.ObjectAnimator.ofInt(
            binding.progressBar,
            "progress",
            binding.progressBar.progress,
            targetProgress
        ).apply {
            duration = 600
            interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
            start()
        }
        // -------------------------------------

        // Kerakli qadamni ko'rsatish
        when (currentStep) {
            1 -> binding.step1From.visibility = View.VISIBLE
            2 -> binding.step2To.visibility = View.VISIBLE
            3 -> binding.step3Date.visibility = View.VISIBLE
            4 -> binding.step4Time.visibility = View.VISIBLE
            5 -> binding.step5Seats.visibility = View.VISIBLE
            6 -> binding.step6Price.visibility = View.VISIBLE
            7 -> {
                binding.step7Info.visibility = View.VISIBLE
                binding.btnNext.setImageResource(R.drawable.ic_check)
            }
        }

        // Agar 7-qadam bo'lmasa, strelka qaytib kelishi kerak
        if (currentStep < 7) {
            binding.btnNext.setImageResource(R.drawable.ic_arrow_right)
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> checkEmpty(binding.etFrom, "Manzilni tanlang")
            2 -> checkEmpty(binding.etTo, "Manzilni tanlang")
            3 -> checkEmpty(binding.etDate, "Sanani tanlang")
            4 -> checkEmpty(binding.etTime, "Vaqtni tanlang")
            5 -> checkEmpty(binding.etSeats, "Joy sonini kiriting")
            6 -> checkEmpty(binding.etPrice, "Narxni kiriting")
            else -> true
        }
    }

    // Yordamchi funksiya kodni ixchamlashtirish uchun
    private fun checkEmpty(view: android.widget.EditText, msg: String): Boolean {
        if (view.text.isNullOrEmpty()) {
            if (view.isFocusable) view.error = msg else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveStepData() {
        when (currentStep) {
            1 -> fromCity = binding.etFrom.text.toString().trim()
            2 -> toCity = binding.etTo.text.toString().trim()
            3 -> tripDate = binding.etDate.text.toString().trim()
            4 -> tripTime = binding.etTime.text.toString().trim()
        }
    }

    // --- BU YER O'ZGARTIRILDI: BAZAGA HAQIQIY YOZISH ---
    private fun checkProfileAndProceed() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Iltimos, avval tizimga kiring!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnNext.isEnabled = false
        Toast.makeText(this, "Ma'lumotlar saqlanmoqda...", Toast.LENGTH_SHORT).show()

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.uid)

        userRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // Profildan ism va telefonni olamiz
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Haydovchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: "+998xxxxxxxxx"

                val fullName = "$firstName $lastName".trim()

                // DIQQAT: Preview ga emas, to'g'ridan-to'g'ri bazaga yozamiz
                publishTripToFirebase(fullName, phone)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                binding.btnNext.isEnabled = true
                Toast.makeText(this@AddTripActivity, "Xatolik: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun publishTripToFirebase(driverName: String, driverPhone: String) {
        val seatsStr = binding.etSeats.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val info = binding.etInfo.text.toString().trim()

        val seats = seatsStr.toIntOrNull() ?: 1
        val price = priceStr.toLongOrNull() ?: 0

        // SearchResultActivity 'trips' (kichik harf) da qidiryapti, shunga moslaymiz
        val database = FirebaseDatabase.getInstance().getReference("trips")
        val tripId = database.push().key ?: return

        val trip = Trip(
            id = tripId,
            userId = auth.currentUser?.uid,
            from = fromCity,
            to = toCity,
            date = tripDate,
            time = tripTime,
            price = price,
            seats = seats, // <-- Faqat 'seats' ni o'zini qoldiramiz
            // seatsAvailable = seats,  <-- BU QATORNI O'CHIRING, modelda yo'q ekan
            info = info,
            driverName = driverName,
            driverPhone = driverPhone
        )


        database.child(tripId).setValue(trip)
            .addOnSuccessListener {
                Toast.makeText(this, "E'lon muvaffaqiyatli joylandi!", Toast.LENGTH_LONG).show()
                finish() // Activity yopiladi va asosiy oynaga qaytadi
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
            }
    }
    // ----------------------------------------------------

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // SearchResultActivity bilan bir xil formatda bo'lishi kerak
                val date = String.format("%02d-%02d-%d", dayOfMonth, month + 1, year)
                binding.etDate.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = String.format("%02d:%02d", hourOfDay, minute)
                binding.etTime.setText(time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
}
