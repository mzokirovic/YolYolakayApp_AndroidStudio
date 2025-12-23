package com.example.yol_yolakay

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityDateOfBirthBinding
import java.util.Calendar

class DateOfBirthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDateOfBirthBinding

    // Tanlangan sanani saqlash uchun o'zgaruvchilar
    private var selectedDay = 0
    private var selectedMonth = 0
    private var selectedYear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateOfBirthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Oldingi oynalardan kelgan ma'lumotlarni qabul qilish
        val firstName = intent.getStringExtra("EXTRA_NAME")
        val lastName = intent.getStringExtra("EXTRA_SURNAME")
        val email = intent.getStringExtra("EXTRA_EMAIL")

        // Orqaga qaytish
        binding.btnBack.setOnClickListener { finish() }

        // Sana maydonini bosganda Kalendarni ochish
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Davom etish
        // ... DateOfBirthActivity.kt ichida ...

// Davom etish
        binding.btnNext.setOnClickListener {
            val dateText = binding.etDate.text.toString()

            if (dateText.isEmpty()) {
                Toast.makeText(this, "Iltimos, tug'ilgan sanangizni kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAdult(selectedYear, selectedMonth, selectedDay)) {
                Toast.makeText(this, "Uzr, siz 18 yoshdan katta bo'lishingiz kerak", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // YANGI KOD: Jins tanlash oynasiga o'tish
            val intent = Intent(this, GenderSelectionActivity::class.java)
            intent.putExtra("EXTRA_NAME", firstName)
            intent.putExtra("EXTRA_SURNAME", lastName)
            intent.putExtra("EXTRA_EMAIL", email)
            intent.putExtra("EXTRA_DOB", dateText) // Sanani ham olib ketamiz
            startActivity(intent)
        }

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 18 yil oldingi sanani hisoblash (Kalendarni ochganda qulay bo'lishi uchun)
        // Yoki shunchaki hozirgi sana bilan ochsa ham bo'ladi.

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year1, month1, dayOfMonth1 ->
                // Tanlangan sanani saqlab olamiz
                selectedYear = year1
                selectedMonth = month1
                selectedDay = dayOfMonth1

                // Ekranga yozish (Oy 0 dan boshlangani uchun +1 qilamiz)
                val formattedDate = String.format("%02d/%02d/%d", dayOfMonth1, month1 + 1, year1)
                binding.etDate.setText(formattedDate)
            },
            year - 18, // Kalendar ochilganda taxminan 18 yil oldingi sanada tursin
            month,
            day
        )

        // Kelajakdagi sanani tanlab bo'lmasligi uchun
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    // 18 yoshga to'lganligini tekshiruvchi funksiya
    private fun isAdult(year: Int, month: Int, day: Int): Boolean {
        if (year == 0) return false // Hali tanlanmagan

        val dob = Calendar.getInstance()
        dob.set(year, month, day)

        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age >= 18
    }
}
