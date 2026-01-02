package com.example.yol_yolakay

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView // TextView import qilindi
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
        // Boshlang'ich holat
        updateStepVisibility()

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepVisibility()
            } else {
                finish()
            }
        }

        // --- Shahar tanlash ---
        binding.etFrom.setOnClickListener {
            val intent = Intent(this, CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "FROM")
            startLocationLauncher.launch(intent)
        }

        binding.etTo.setOnClickListener {
            val intent = Intent(this, CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "TO")
            endLocationLauncher.launch(intent)
        }

        // Sana va Vaqt
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etTime.setOnClickListener { showTimePicker() }

        binding.btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < 7) {
                    saveStepData()
                    currentStep++
                    updateStepVisibility()
                } else {
                    checkProfileAndProceed()
                }
            }
        }
    }

    private fun updateStepVisibility() {
        binding.step1From.visibility = View.GONE
        binding.step2To.visibility = View.GONE
        binding.step3Date.visibility = View.GONE
        binding.step4Time.visibility = View.GONE
        binding.step5Seats.visibility = View.GONE
        binding.step6Price.visibility = View.GONE
        binding.step7Info.visibility = View.GONE

        // Progress bar animatsiyasi
        binding.progressBar.max = 1000
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

        if (currentStep < 7) {
            binding.btnNext.setImageResource(R.drawable.ic_arrow_right)
        }
    }

    private fun validateCurrentStep(): Boolean {
        // MUHIM O'ZGARISH: TextView va EditText ni alohida tekshiramiz
        return when (currentStep) {
            1 -> checkEmpty(binding.etFrom, "Manzilni tanlang")
            2 -> checkEmpty(binding.etTo, "Manzilni tanlang")
            // 3 va 4 qadam endi TextView (checkTextEmpty funksiyasiga uzatamiz)
            3 -> checkTextEmpty(binding.etDate, "Sanani tanlang", "Sanani tanlang")
            4 -> checkTextEmpty(binding.etTime, "Vaqtni tanlang", "Vaqtni tanlang")
            5 -> checkEmpty(binding.etSeats, "Joy sonini kiriting")
            6 -> checkEmpty(binding.etPrice, "Narxni kiriting")
            else -> true
        }
    }

    // Bu funksiya EditText lar uchun (Shahar, Narx, Joy)
    private fun checkEmpty(view: android.widget.EditText, msg: String): Boolean {
        if (view.text.isNullOrEmpty()) {
            if (view.isFocusable) view.error = msg else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // YANGI FUNKSIYA: Bu TextView lar uchun (Sana, Vaqt)
    private fun checkTextEmpty(view: TextView, msg: String, defaultText: String): Boolean {
        val text = view.text.toString()
        // Agar matn bo'sh bo'lsa yoki hali ham "Sanani tanlang" deb turgan bo'lsa -> Xato
        if (text.isEmpty() || text == defaultText) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Haydovchi"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: "+998xxxxxxxxx"
                val fullName = "$firstName $lastName".trim()

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
            seats = seats,
            info = info,
            driverName = driverName,
            driverPhone = driverPhone
        )

        database.child(tripId).setValue(trip)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                showSuccessDialog(trip)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format("%02d.%02d.%d", dayOfMonth, month + 1, year)
                // TextView bo'lgani uchun setText o'rniga text = ... ishlatsa ham bo'ladi,
                // lekin setText ham ishlaydi. Eng asosiysi TextView bu metodni qo'llab-quvvatlaydi.
                binding.etDate.text = date
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
                binding.etTime.text = time
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun showSuccessDialog(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val btnViewTrip = dialogView.findViewById<android.widget.Button>(R.id.btnViewTrip)
        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnCloseDialog)

        btnViewTrip.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, TripDetailsActivity::class.java)
            val gson = com.google.gson.Gson()
            val tripJson = gson.toJson(trip)
            intent.putExtra("TRIP_JSON", tripJson)
            intent.putExtra("IS_PREVIEW", true)
            startActivity(intent)
            finish()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}
