package com.example.yol_yolakay

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yol_yolakay.databinding.ActivityDateOfBirthBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        // Sana maydonini bosganda ZAMONAVIY Kalendarni ochish
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Davom etish
        binding.btnNext.setOnClickListener {
            val dateText = binding.etDate.text.toString()

            if (dateText.isEmpty()) {
                Toast.makeText(this, "Iltimos, tug'ilgan sanangizni kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Yoshni tekshirish (18 yosh)
            if (!isAdult(selectedYear, selectedMonth, selectedDay)) {
                Toast.makeText(this, "Uzr, siz 18 yoshdan katta bo'lishingiz kerak", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Jins tanlash oynasiga o'tish
            val intent = Intent(this, GenderSelectionActivity::class.java)
            intent.putExtra("EXTRA_NAME", firstName)
            intent.putExtra("EXTRA_SURNAME", lastName)
            intent.putExtra("EXTRA_EMAIL", email)
            intent.putExtra("EXTRA_DOB", dateText)
            startActivity(intent)
        }
    }

    private fun showDatePicker() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // XML faylni yuklaymiz
        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        dialog.setContentView(view)

        // Orqa fon shaffof
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // View elementlarini topamiz
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val wheelContainer = view.findViewById<View>(R.id.wheelContainer)

        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val tvYearHeader = view.findViewById<TextView>(R.id.tvYearHeader)

        // Buttonlar
        val btnConfirm = view.findViewById<View>(R.id.btnConfirmDate)
        val btnCancel = view.findViewById<View>(R.id.btnCancelDate)

        // Baraban elementlari (NumberPicker)
        val npDay = view.findViewById<android.widget.NumberPicker>(R.id.npDay)
        val npMonth = view.findViewById<android.widget.NumberPicker>(R.id.npMonth)
        val npYear = view.findViewById<android.widget.NumberPicker>(R.id.npYear)

        // Asosiy o'zgarish: Kalendarni yashiramiz, Barabanni ochamiz
        calendarView.visibility = View.GONE
        wheelContainer.visibility = View.VISIBLE

        // Header formatlari
        val dateFormatHeader = SimpleDateFormat("d MMMM", Locale("uz", "UZ"))
        val yearFormatHeader = SimpleDateFormat("yyyy", Locale("uz", "UZ"))

        // Hozirgi vaqt yoki tanlangan vaqtni olamiz
        val calendar = Calendar.getInstance()
        if (selectedYear != 0) {
            calendar.set(selectedYear, selectedMonth, selectedDay)
        } else {
            // Agar hali tanlanmagan bo'lsa, standart 18 yosh (2006 yil atrofida) qilib qo'yamiz
            calendar.add(Calendar.YEAR, -18)
        }

        // Headerga boshlang'ich matnni yozamiz
        tvDateHeader.text = dateFormatHeader.format(calendar.time)
        tvYearHeader.text = yearFormatHeader.format(calendar.time)

        // --- BARABANNI SOZLASH ---

        // 1. OYLAR
        val months = arrayOf(
            "YAN", "FEV", "MAR", "APR", "MAY", "IYN",
            "IYL", "AVG", "SEN", "OKT", "NOY", "DEK"
        )
        npMonth.minValue = 0
        npMonth.maxValue = 11
        npMonth.displayedValues = months
        npMonth.value = calendar.get(Calendar.MONTH)
        npMonth.wrapSelectorWheel = true

        // 2. YILLAR (1950 dan Hozirgacha - 18 yil)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        // Ro'yxatdan o'tish uchun minimum yosh chegarasi bo'lsa (masalan 2026 - 5 = 2021)
        val maxYear = currentYear
        val minYear = 1950

        npYear.minValue = minYear
        npYear.maxValue = maxYear
        npYear.value = calendar.get(Calendar.YEAR)
        npYear.wrapSelectorWheel = false

        // 3. KUNLAR (Oy va yilga qarab o'zgaradi)
        fun updateDaysInMonth() {
            val tempCal = Calendar.getInstance()
            tempCal.set(npYear.value, npMonth.value, 1)
            val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            npDay.minValue = 1
            npDay.maxValue = maxDay

            // Agar tanlangan kun (masalan 31) yangi oyda yo'q bo'lsa (masalan Fevral 28), uni to'g'irlash
            if (npDay.value > maxDay) {
                npDay.value = maxDay
            }
        }

        // Boshlanishiga kunni to'g'irlaymiz
        npDay.minValue = 1
        npDay.maxValue = 31 // Vaqtincha, updateDaysInMonth to'g'irlaydi
        npDay.value = calendar.get(Calendar.DAY_OF_MONTH)
        updateDaysInMonth()

        // Baraban aylanganda Headerni va kunlar sonini yangilash
        val updateListener = {
            // Kunlar sonini yangilash
            updateDaysInMonth()

            // Headerni yangilash
            val newCal = Calendar.getInstance()
            newCal.set(npYear.value, npMonth.value, npDay.value)
            tvDateHeader.text = dateFormatHeader.format(newCal.time)
            tvYearHeader.text = yearFormatHeader.format(newCal.time)
        }

        npYear.setOnValueChangedListener { _, _, _ -> updateListener() }
        npMonth.setOnValueChangedListener { _, _, _ -> updateListener() }
        npDay.setOnValueChangedListener { _, _, _ ->
            // Kun o'zgarganda faqat headerni yangilaymiz
            val newCal = Calendar.getInstance()
            newCal.set(npYear.value, npMonth.value, npDay.value)
            tvDateHeader.text = dateFormatHeader.format(newCal.time)
            tvYearHeader.text = yearFormatHeader.format(newCal.time)
        }

        // --- TASDIQLASH ---
        btnConfirm.setOnClickListener {
            selectedDay = npDay.value
            selectedMonth = npMonth.value
            selectedYear = npYear.value

            // Formatlash: 05/01/2006
            val formattedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
            binding.etDate.setText(formattedDate)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
