package com.example.yol_yolakay

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Trips")

        setupUI()
    }


    private fun goToPreviewActivity() {
        // Oxirgi qadam ma'lumotlarini olish
        val seatsStr = binding.etSeats.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val info = binding.etInfo.text.toString().trim()

        val seats = seatsStr.toIntOrNull() ?: 1
        val price = priceStr.toLongOrNull() ?: 0

        // Vaqtinchalik ID yaratamiz (yoki null qoldiramiz, Firebase o'zi beradi)
        // Lekin obyektni to'liq yasab olishimiz kerak
        val trip = Trip(
            id = null, // Hali ID yo'q
            from = fromCity,
            to = toCity,
            date = tripDate,
            time = tripTime,
            seats = seats,
            price = price,
            info = info
        )

        val intent = android.content.Intent(this, TripDetailsActivity::class.java)
        intent.putExtra("TRIP_DATA", trip)
        intent.putExtra("IS_PREVIEW", true) // Bu muhim: Biz ko'rish rejimida ekanligimizni bildiramiz
        startActivity(intent)
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

        // Sana tanlash
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Vaqt tanlash
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

        // Progress barni yangilash (jami 7 qadam, 100% / 7 ≈ 14.3)
        val progress = (currentStep * 14.3).toInt()
        binding.progressBar.setProgressCompat(progress, true)

        // Kerakli qadamni ko'rsatish
        when (currentStep) {
            1 -> {
                binding.step1From.visibility = View.VISIBLE
                binding.etFrom.requestFocus()
            }
            2 -> {
                binding.step2To.visibility = View.VISIBLE
                binding.etTo.requestFocus()
            }
            3 -> binding.step3Date.visibility = View.VISIBLE
            4 -> binding.step4Time.visibility = View.VISIBLE
            5 -> binding.step5Seats.visibility = View.VISIBLE
            6 -> binding.step6Price.visibility = View.VISIBLE
            7 -> {
                binding.step7Info.visibility = View.VISIBLE
                // Oxirgi qadamda tugma ikonkasi o'zgarishi mumkin (masalan check)
                binding.btnNext.setImageResource(R.drawable.ic_check) // Agar ic_check bor bo'lsa
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (binding.etFrom.text.isNullOrEmpty()) {
                    binding.etFrom.error = "Manzilni kiriting"
                    false
                } else true
            }
            2 -> {
                if (binding.etTo.text.isNullOrEmpty()) {
                    binding.etTo.error = "Manzilni kiriting"
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

    private fun saveTripToFirebase() {
        val userId = auth.currentUser?.uid

        // 1. Agar foydalanuvchi tizimga kirmagan bo'lsa
        if (userId == null) {
            showLoginDialog() // Yangi funksiyani chaqiramiz
            return
        }

        // 2. Agar tizimga kirgan bo'lsa, davom etamiz...
        val seatsStr = binding.etSeats.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val info = binding.etInfo.text.toString().trim()

        val seats = seatsStr.toIntOrNull() ?: 1
        val price = priceStr.toLongOrNull() ?: 0

        val tripId = database.push().key ?: return

        val trip = Trip(
            id = tripId,
            from = fromCity,
            to = toCity,
            date = tripDate,
            time = tripTime,
            seats = seats,
            price = price,
            info = info
        )

        binding.btnNext.isEnabled = false

        database.child(tripId).setValue(trip)
            .addOnSuccessListener {
                showSuccessDialog(trip)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
            }
    }

    private fun showLoginDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        dialog.setTitle("Diqqat!")
        dialog.setMessage("E'lon berish uchun avval tizimga kirishingiz yoki ro'yxatdan o'tishingiz kerak.")
        dialog.setCancelable(false) // Oynani chetini bosib yopib bo'lmaydi

        // "Ro'yxatdan o'tish" tugmasi
        dialog.setPositiveButton("Ro'yxatdan o'tish") { _, _ ->
            // RegistrationActivity ga o'tish
            val intent = android.content.Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        // "Bekor qilish" tugmasi
        dialog.setNegativeButton("Bekor qilish") { d, _ ->
            d.dismiss()
            finish() // Agar xohlasangiz, oynani yopib yuborish mumkin
        }

        dialog.show()
    }


    // ... (qolgan kodlar o'zgarishsiz)

    private fun showSuccessDialog(trip: Trip) { // Trip obyektini qabul qiladi
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        try {
            dialog.setContentView(R.layout.dialog_success)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(false)

            // 1. "E'lonni ko'rish" tugmasi
            val btnViewTrip = dialog.findViewById<Button>(R.id.btnViewTrip)
            btnViewTrip?.setOnClickListener {
                dialog.dismiss()

                val intent = Intent(this@AddTripActivity, TripDetailsActivity::class.java)
                // DIQQAT: Bu yerda ham kalit so'z "TRIP_DATA" bo'lishi SHART
                intent.putExtra("TRIP_DATA", trip)
                startActivity(intent)

                finish()
            }

            // 2. "Yopish" tugmasi
            val btnClose = dialog.findViewById<Button>(R.id.btnCloseDialog)
            btnClose?.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            dialog.show()
        } catch (e: Exception) {
            // Agar XML da xato bo'lsa, shunchaki xabar chiqarib yopamiz
            Toast.makeText(this, "Safar muvaffaqiyatli qo'shildi!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ... (qolgan kodlar o'zgarishsiz)


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
