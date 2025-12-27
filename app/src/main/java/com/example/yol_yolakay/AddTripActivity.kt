package com.example.yol_yolakay

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
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
    private lateinit var database: com.google.firebase.database.DatabaseReference

    // Hozirgi qadam (1 dan 7 gacha)
    private var currentStep = 1

    // Ma'lumotlarni vaqtincha saqlash
    private var fromCity: String = ""
    private var toCity: String = ""
    private var tripDate: String = ""
    private var tripTime: String = ""

    // --- YANGI: Shahar tanlash uchun Launcherlar ---
    private val startLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etFrom.setText(city)
                // Avtomatik keyingi qadamga o'tishimiz mumkin (ixtiyoriy)
                // lekin hozircha foydalanuvchi "Keyingisi" ni bosgani ma'qul
            }
        }
    }

    private val endLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etTo.setText(city)
            }
        }
    }
    // -----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Trips")

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

        // --- YANGI: Shahar tanlash oynalarini ulash ---
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
                    goToPreviewActivity()
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
        // Progress barni aniqligini oshirish uchun layout faylida android:max="1000" qilingan bo'lsa yaxshi,
        // bo'lmasa shu yerni o'zida to'g'irlaymiz.
        binding.progressBar.max = 1000

        // Har bir qadam ~142.8 ball (1000 / 7)
        val targetProgress = (currentStep * 142.8).toInt()

        android.animation.ObjectAnimator.ofInt(
            binding.progressBar,
            "progress",
            binding.progressBar.progress,
            targetProgress
        ).apply {
            duration = 600 // Biroz tezroq (300ms) - bu dinamikroq tuyuladi
            // FastOutSlowInInterpolator eng tabiiy harakatni beradi (boshida tez, oxirida sekin)
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
            binding.btnNext.setImageResource(R.drawable.ic_arrow_right) // Ikonka nomini tekshiring
        }
    }



    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (binding.etFrom.text.isNullOrEmpty()) {
                    binding.etFrom.error = "Manzilni tanlang"
                    false
                } else true
            }
            2 -> {
                if (binding.etTo.text.isNullOrEmpty()) {
                    binding.etTo.error = "Manzilni tanlang"
                    false
                } else true
            }
            3 -> {
                if (binding.etDate.text.isNullOrEmpty()) {
                    Toast.makeText(this, "Sanani tanlang", Toast.LENGTH_SHORT).show()
                    false
                } else true
            }
            4 -> {
                if (binding.etTime.text.isNullOrEmpty()) {
                    Toast.makeText(this, "Vaqtni tanlang", Toast.LENGTH_SHORT).show()
                    false
                } else true
            }
            5 -> {
                if (binding.etSeats.text.isNullOrEmpty()) {
                    binding.etSeats.error = "Joy sonini kiriting"
                    false
                } else true
            }
            6 -> {
                if (binding.etPrice.text.isNullOrEmpty()) {
                    binding.etPrice.error = "Narxni kiriting"
                    false
                } else true
            }
            else -> true
        }
    }

    private fun saveStepData() {
        when (currentStep) {
            1 -> fromCity = binding.etFrom.text.toString().trim()
            2 -> toCity = binding.etTo.text.toString().trim()
            3 -> tripDate = binding.etDate.text.toString().trim()
            4 -> tripTime = binding.etTime.text.toString().trim()
        }
    }

    private fun goToPreviewActivity() {
        val seatsStr = binding.etSeats.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val info = binding.etInfo.text.toString().trim()

        // Raqamga aylantirish (xatolik bo'lsa default qiymat)
        val seats = seatsStr.toIntOrNull() ?: 1
        val price = priceStr.toLongOrNull() ?: 0

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val driverName = sharedPref.getString("USER_NAME", "Haydovchi") ?: "Haydovchi"
        val driverPhone = sharedPref.getString("USER_PHONE", "") ?: ""

        // 1. Trip obyektini yaratamiz
        val trip = com.example.yol_yolakay.model.Trip(
            id = null,
            from = fromCity,
            to = toCity,
            date = tripDate,
            time = tripTime,
            seats = seats,
            price = price,
            info = info,
            driverName = driverName,
            driverPhone = driverPhone
        )

        // 2. Intent yaratamiz
        val intent = android.content.Intent(this, TripDetailsActivity::class.java)

        // 3. --- ENG MUHIM QISM: JSON ORQALI JO'NATISH ---
        val gson = com.google.gson.Gson()
        val tripJson = gson.toJson(trip)

        intent.putExtra("TRIP_JSON", tripJson) // <--- TripDetailsActivity shu nom bilan kutmoqda
        intent.putExtra("IS_PREVIEW", true)

        startActivity(intent)
        // Eslatma: finish() ni bu yerda chaqirmay turing, foydalanuvchi "Orqaga" qaytib tahrirlay olishi uchun
    }



    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format("%02d.%02d.%d", dayOfMonth, month + 1, year)
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
