package com.example.yol_yolakay

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityAddTripBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class AddTripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripBinding
    private lateinit var database: DatabaseReference
    private var currentStep = 1 // Hozirgi qadam (jami 7 ta)
    private val calendar = Calendar.getInstance()

    // Ma'lumotlarni vaqtincha saqlash
    private var fromCity: String = ""
    private var toCity: String = ""
    private var tripDate: String = ""
    private var tripTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("trips")

        // Boshlang'ich holat
        updateUI()

        // Orqaga qaytish tugmasi
        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateUI()
            } else {
                finish()
            }
        }

        // Sana tanlash (3-qadam)
        binding.etDate.setOnClickListener { showDatePicker() }

        // Vaqt tanlash (4-qadam)
        binding.etTime.setOnClickListener { showTimePicker() }

        // KEYINGI tugmasi (FAB)
        binding.btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < 7) {
                    currentStep++
                    updateUI()
                } else {
                    // 7-bosqichdan keyin saqlash
                    saveTripToFirebase()
                }
            }
        }
    }

    private fun updateUI() {
        // 1. Hamma bosqichlarni yashiramiz
        binding.step1From.visibility = View.GONE
        binding.step2To.visibility = View.GONE
        binding.step3Date.visibility = View.GONE
        binding.step4Time.visibility = View.GONE
        binding.step5Seats.visibility = View.GONE
        binding.step6Price.visibility = View.GONE
        binding.step7Info.visibility = View.GONE

        // 2. Tugma iconkasini standart holatga (o'ngga strelka) qaytaramiz
        binding.btnNext.setImageResource(R.drawable.ic_arrow_right)

        // 3. Joriy qadamni ochamiz va progressni yangilaymiz
        when (currentStep) {
            1 -> {
                binding.step1From.visibility = View.VISIBLE
                binding.progressBar.progress = 14
            }
            2 -> {
                binding.step2To.visibility = View.VISIBLE
                binding.progressBar.progress = 28
            }
            3 -> {
                binding.step3Date.visibility = View.VISIBLE
                binding.progressBar.progress = 42
            }
            4 -> {
                binding.step4Time.visibility = View.VISIBLE
                binding.progressBar.progress = 56
            }
            5 -> {
                binding.step5Seats.visibility = View.VISIBLE
                binding.progressBar.progress = 70
            }
            6 -> {
                binding.step6Price.visibility = View.VISIBLE
                binding.progressBar.progress = 85
            }
            7 -> {
                binding.step7Info.visibility = View.VISIBLE
                binding.progressBar.progress = 100
                // Oxirgi qadamda tugma iconkasini "Tasdiqlash" (pachka) ga o'zgartiramiz
                // Eslatma: ic_check yoki ic_done iconkasi bo'lishi kerak.
                // Agar yo'q bo'lsa, vaqtincha ic_arrow_right qolaversin.
                binding.btnNext.setImageResource(R.drawable.ic_arrow_right) // Yoki o'zingizda bor boshqa icon
            }
        }
    }

    private fun validateCurrentStep(): Boolean {
        when (currentStep) {
            1 -> {
                fromCity = binding.etFrom.text.toString().trim()
                if (fromCity.isEmpty()) {
                    binding.etFrom.error = "Manzilni kiriting"
                    return false
                }
            }
            2 -> {
                toCity = binding.etTo.text.toString().trim()
                if (toCity.isEmpty()) {
                    binding.etTo.error = "Manzilni kiriting"
                    return false
                }
            }
            3 -> {
                tripDate = binding.etDate.text.toString().trim()
                if (tripDate.isEmpty()) {
                    Toast.makeText(this, "Sanani tanlang", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            4 -> {
                tripTime = binding.etTime.text.toString().trim()
                if (tripTime.isEmpty()) {
                    Toast.makeText(this, "Vaqtni tanlang", Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            5 -> {
                val seats = binding.etSeats.text.toString().trim()
                if (seats.isEmpty()) {
                    binding.etSeats.error = "Joy sonini kiriting"
                    return false
                }
            }
            6 -> {
                val price = binding.etPrice.text.toString().trim()
                if (price.isEmpty()) {
                    binding.etPrice.error = "Narxni kiriting"
                    return false
                }
            }
            7 -> {
                // Qo'shimcha ma'lumot ixtiyoriy, shuning uchun tekshirish shart emas
                return true
            }
        }
        return true
    }

    private fun saveTripToFirebase() {
        // Ma'lumotlarni yig'ib olish
        val seatsStr = binding.etSeats.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val info = binding.etInfo.text.toString().trim() // XML da ID: etInfo

        // Sonlarga o'tkazish
        val seats = seatsStr.toIntOrNull() ?: 1
        val price = priceStr.toLongOrNull() ?: 0

        val id = database.push().key ?: return

        val trip = Trip(id, fromCity, toCity, tripDate, tripTime, seats, price, info)

        // Tugmani bloklash (qayta bosishni oldini olish)
        binding.btnNext.isEnabled = false

        database.child(id).setValue(trip)
            .addOnSuccessListener {
                showSuccessDialog(trip)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Xatolik: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
            }
    }

    private fun showSuccessDialog(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnViewTrip = dialogView.findViewById<Button>(R.id.btnViewTrip)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDialog)

        btnViewTrip.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, TripDetailsActivity::class.java)
            intent.putExtra("trip", trip)
            startActivity(intent)
            finish()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            val selectedDate = String.format("%02d.%02d.%d", day, month + 1, year)
            binding.etDate.setText(selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(this, { _, hour, minute ->
            binding.etTime.setText(String.format("%02d:%02d", hour, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePicker.show()
    }
}
