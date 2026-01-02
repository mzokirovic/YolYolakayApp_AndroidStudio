package com.example.yol_yolakay

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Biz yasagan layoutni ulaymiz
        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        dialog.setContentView(view)

        // Orqa fonni shaffof qilamiz va o'lchamni to'g'irlaymiz
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // View elementlarini topamiz
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val rvYears = view.findViewById<RecyclerView>(R.id.rvYears)
        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val tvYearHeader = view.findViewById<TextView>(R.id.tvYearHeader)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirmDate)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelDate)

        val dateFormat = SimpleDateFormat("d MMMM", Locale("uz", "UZ"))
        val yearFormat = SimpleDateFormat("yyyy", Locale("uz", "UZ"))

        val calendar = Calendar.getInstance()

        // Agar avval sana tanlangan bo'lsa, o'shani o'rnatamiz, aks holda 18 yil orqaga
        if (selectedYear != 0) {
            calendar.set(selectedYear, selectedMonth, selectedDay)
        } else {
            calendar.add(Calendar.YEAR, -18) // Qulaylik uchun 18 yil orqaga
        }

        // Kelajakni yopamiz (Tug'ilgan kun kelajakda bo'lolmaydi)
        calendarView.maxDate = System.currentTimeMillis()
        calendarView.date = calendar.timeInMillis // Kalendarni surish

        // Boshlang'ich matnlar
        tvDateHeader.text = dateFormat.format(calendar.time)
        tvYearHeader.text = yearFormat.format(calendar.time)

        // Dastlabki qiymatlar
        var tempDay = calendar.get(Calendar.DAY_OF_MONTH)
        var tempMonth = calendar.get(Calendar.MONTH)
        var tempYear = calendar.get(Calendar.YEAR)

        // Kalendar o'zgarganda
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            tempYear = year
            tempMonth = month
            tempDay = dayOfMonth

            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)

            tvDateHeader.text = dateFormat.format(newDate.time)
            tvYearHeader.text = yearFormat.format(newDate.time)
        }

        // Yilni bosganda ro'yxatni ochish
        tvYearHeader.setOnClickListener {
            if (rvYears.visibility == View.VISIBLE) {
                rvYears.visibility = View.GONE
                calendarView.visibility = View.VISIBLE
                try {
                    tvYearHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
                } catch (e: Exception) {}
            } else {
                calendarView.visibility = View.GONE
                rvYears.visibility = View.VISIBLE

                // Tug'ilgan kun uchun yillar: 1950 dan Hozirgacha
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                // reversed() qildik, shunda yangi yillar tepada turadi
                val yearsList = (1950..currentYear).toList().reversed()

                rvYears.layoutManager = LinearLayoutManager(this)
                rvYears.adapter = YearAdapterDOB(yearsList, tempYear) { clickedYear ->
                    tempYear = clickedYear

                    val newDate = Calendar.getInstance()
                    newDate.set(tempYear, tempMonth, tempDay)

                    // Agar tanlangan sana maxDate dan o'tib ketsa, to'g'irlaymiz
                    if (newDate.timeInMillis > calendarView.maxDate) {
                        newDate.timeInMillis = calendarView.maxDate
                    }

                    calendarView.date = newDate.timeInMillis
                    tvYearHeader.text = clickedYear.toString()
                    tvDateHeader.text = dateFormat.format(newDate.time)

                    rvYears.visibility = View.GONE
                    calendarView.visibility = View.VISIBLE
                }
            }
        }

        btnConfirm.setOnClickListener {
            // Asl o'zgaruvchilarga saqlaymiz
            selectedDay = tempDay
            selectedMonth = tempMonth
            selectedYear = tempYear

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

// Boshqa fayldagi adapter bilan nomuvofiqlik bo'lmasligi uchun nomini o'zgartirdim (YearAdapterDOB)
// Agar YearAdapter allaqachon alohida faylda bo'lsa, shunchaki o'shani ishlating.
class YearAdapterDOB(
    private val years: List<Int>,
    private val selectedYear: Int,
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<YearAdapterDOB.YearViewHolder>() {

    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvYear: TextView = itemView.findViewById(R.id.tvYearItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_year_text, parent, false)
        return YearViewHolder(view)
    }

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        val year = years[position]
        holder.tvYear.text = year.toString()

        if (year == selectedYear) {
            holder.tvYear.setTextColor(Color.parseColor("#2E5BFF"))
            holder.tvYear.textSize = 20f
        } else {
            holder.tvYear.setTextColor(Color.parseColor("#1E293B"))
            holder.tvYear.textSize = 16f
        }

        holder.itemView.setOnClickListener { onYearClick(year) }
    }

    override fun getItemCount(): Int = years.size
}
