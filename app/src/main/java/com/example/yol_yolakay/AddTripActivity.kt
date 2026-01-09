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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.databinding.ActivityAddTripBinding
import com.example.yol_yolakay.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTripActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTripBinding
    private lateinit var auth: FirebaseAuth

    private var currentStep = 1
    private var fromCity: String? = null
    private var toCity: String? = null
    private var tripDate: String? = null
    private var tripTime: String? = null

    // YANGI: BlaBlaCar Stepper uchun o'zgaruvchi
    private var seatCount = 1

    // Shahar tanlash uchun launcherlar
    private val startLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etFrom.setText(city)
                binding.etFrom.error = null
            }
        }
    }

    private val endLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.etTo.setText(city)
                binding.etTo.error = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        updateStepVisibility()

        setupPriceFormatting() // Narxni formatlashni yoqish
        setupSeatStepper()     // Stepper (plus/minus) ni yoqish

        binding.btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepVisibility()
            } else {
                finish()
            }
        }

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

    // --- PROFESSIONALLIK: BlaBlaCar Stepper Mantiqi ---
    private fun setupSeatStepper() {
        binding.btnMinusSeat?.setOnClickListener {
            if (seatCount > 1) {
                seatCount--
                updateSeatUI()
            }
        }
        binding.btnPlusSeat?.setOnClickListener {
            if (seatCount < 4) { // Talabga binoan max 4 kishi
                seatCount++
                updateSeatUI()
            }
        }
    }

    private fun updateSeatUI() {
        // Katta raqamni yangilaydi
        binding.tvSeatDisplay?.text = seatCount.toString()
        // Yashirin EditText ni yangilaydi (validation buzilmasligi uchun)
        binding.etSeats.setText(seatCount.toString())
    }

    private fun setupPriceFormatting() {
        binding.etPrice.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() != current) {
                    binding.etPrice.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[^0-9]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0

                    val formatted = java.text.DecimalFormat("#,###").format(parsed).replace(",", " ")

                    current = formatted
                    binding.etPrice.setText(formatted)
                    binding.etPrice.setSelection(formatted.length)

                    binding.etPrice.addTextChangedListener(this)
                }
            }
        })
    }

    private fun updateStepVisibility() {
        binding.step1From.visibility = View.GONE
        binding.step2To.visibility = View.GONE
        binding.step3Date.visibility = View.GONE
        binding.step4Time.visibility = View.GONE
        binding.step5Seats.visibility = View.GONE
        binding.step6Price.visibility = View.GONE
        binding.step7Info.visibility = View.GONE

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
        return when (currentStep) {
            1 -> checkEmpty(binding.etFrom, "Manzilni tanlang")
            2 -> checkEmpty(binding.etTo, "Manzilni tanlang")
            3 -> checkTextEmpty(binding.etDate, "Sanani tanlang", "Sanani tanlang")
            4 -> checkTextEmpty(binding.etTime, "Vaqtni tanlang", "Vaqtni tanlang")
            5 -> checkEmpty(binding.etSeats, "Joy sonini kiriting")
            6 -> checkEmpty(binding.etPrice, "Narxni kiriting")
            else -> true
        }
    }

    private fun checkEmpty(view: android.widget.EditText, msg: String): Boolean {
        if (view.text.isNullOrEmpty()) {
            if (view.isFocusable) view.error = msg else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkTextEmpty(view: TextView, msg: String, defaultText: String): Boolean {
        val text = view.text.toString()
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
        binding.progressBar.visibility = View.VISIBLE

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val dbFirstName = snapshot.child("firstName").getValue(String::class.java)
            val dbLastName = snapshot.child("lastName").getValue(String::class.java)
            val dbPhone = snapshot.child("phone").getValue(String::class.java) ?: snapshot.child("phoneNumber").getValue(String::class.java)

            val finalName = if (!dbFirstName.isNullOrEmpty()) {
                "$dbFirstName ${dbLastName ?: ""}".trim()
            } else {
                currentUser.displayName ?: "Haydovchi"
            }

            val finalPhone = dbPhone ?: currentUser.phoneNumber ?: "+998"

            proceedToPreview(finalName, finalPhone)

        }.addOnFailureListener {
            proceedToPreview(currentUser.displayName ?: "Haydovchi", "+998")
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("users").child(userId).keepSynced(true)
    }

    private fun proceedToPreview(driverName: String, driverPhone: String) {
        val seats = binding.etSeats.text.toString().toIntOrNull() ?: 1
        val price = binding.etPrice.text.toString().replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L

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
            info = binding.etInfo.text.toString().trim(),
            driverName = driverName,
            driverPhone = driverPhone,
            status = "active"
        )

        val intent = Intent(this, TripDetailsActivity::class.java)
        intent.putExtra("TRIP_OBJ", trip)
        intent.putExtra("IS_PREVIEW", true)
        startActivity(intent)

        binding.btnNext.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun showDatePicker() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val wheelContainer = view.findViewById<View>(R.id.wheelContainer)
        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val tvYearHeader = view.findViewById<TextView>(R.id.tvYearHeader)
        val btnConfirm = view.findViewById<View>(R.id.btnConfirmDate)
        val btnCancel = view.findViewById<View>(R.id.btnCancelDate)
        val npDay = view.findViewById<android.widget.NumberPicker>(R.id.npDay)
        val npMonth = view.findViewById<android.widget.NumberPicker>(R.id.npMonth)
        val npYear = view.findViewById<android.widget.NumberPicker>(R.id.npYear)

        val dateFormatHeader = SimpleDateFormat("d MMMM", Locale("uz", "UZ"))
        val yearFormatHeader = SimpleDateFormat("yyyy", Locale("uz", "UZ"))
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale("uz", "UZ"))
        val calendar = Calendar.getInstance()

        val currentText = binding.etDate.text.toString().trim()
        if (currentText.isNotEmpty()) {
            try {
                outputFormat.parse(currentText)?.let { calendar.time = it }
            } catch (e: Exception) { e.printStackTrace() }
        }

        calendarView.minDate = System.currentTimeMillis() - 1000
        calendarView.date = calendar.timeInMillis
        tvDateHeader.text = dateFormatHeader.format(calendar.time)
        tvYearHeader.text = yearFormatHeader.format(calendar.time)

        var tempDay = calendar.get(Calendar.DAY_OF_MONTH)
        var tempMonth = calendar.get(Calendar.MONTH)
        var tempYear = calendar.get(Calendar.YEAR)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            tempYear = year; tempMonth = month; tempDay = dayOfMonth
            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)
            tvDateHeader.text = dateFormatHeader.format(newDate.time)
            tvYearHeader.text = yearFormatHeader.format(newDate.time)
        }

        tvYearHeader.setOnClickListener {
            if (wheelContainer.visibility == View.VISIBLE) {
                wheelContainer.visibility = View.GONE; calendarView.visibility = View.VISIBLE
            } else {
                calendarView.visibility = View.GONE; wheelContainer.visibility = View.VISIBLE
                val months = arrayOf("YAN", "FEV", "MAR", "APR", "MAY", "IYN", "IYL", "AVG", "SEN", "OKT", "NOY", "DEK")
                npMonth.minValue = 0; npMonth.maxValue = 11; npMonth.displayedValues = months
                npMonth.value = tempMonth; npMonth.wrapSelectorWheel = true
                val thisYear = Calendar.getInstance().get(Calendar.YEAR)
                npYear.minValue = thisYear; npYear.maxValue = thisYear + 5
                npYear.value = tempYear; npYear.wrapSelectorWheel = false

                fun updateDays() {
                    val temp = Calendar.getInstance()
                    temp.set(npYear.value, npMonth.value, 1)
                    val max = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
                    npDay.minValue = 1; npDay.maxValue = max
                    if (npDay.value > max) npDay.value = max
                }
                npDay.value = tempDay; updateDays()
                npYear.setOnValueChangedListener { _, _, _ -> updateDays() }
                npMonth.setOnValueChangedListener { _, _, _ -> updateDays() }
            }
        }

        btnConfirm.setOnClickListener {
            if (wheelContainer.visibility == View.VISIBLE) {
                tempDay = npDay.value; tempMonth = npMonth.value; tempYear = npYear.value
            }
            val newDate = Calendar.getInstance()
            newDate.set(tempYear, tempMonth, tempDay)
            binding.etDate.text = outputFormat.format(newDate.time)
            binding.etDate.error = null
            binding.etDate.setTextColor(Color.parseColor("#1E293B"))
            dialog.dismiss()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showTimePicker() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.dialog_time_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

        val npHour = view.findViewById<android.widget.NumberPicker>(R.id.npHour)
        val npMinute = view.findViewById<android.widget.NumberPicker>(R.id.npMinute)
        val btnConfirm = view.findViewById<View>(R.id.btnConfirmTime)
        val btnCancel = view.findViewById<View>(R.id.btnCancelTime)

        npHour.minValue = 0; npHour.maxValue = 23; npHour.setFormatter { i -> String.format("%02d", i) }
        npMinute.minValue = 0; npMinute.maxValue = 59; npMinute.setFormatter { i -> String.format("%02d", i) }

        val currentText = binding.etTime.text.toString().trim()
        if (currentText.isNotEmpty() && currentText.contains(":")) {
            try {
                val parts = currentText.split(":")
                npHour.value = parts[0].toInt(); npMinute.value = parts[1].toInt()
            } catch (e: Exception) {
                npHour.value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                npMinute.value = Calendar.getInstance().get(Calendar.MINUTE)
            }
        } else {
            npHour.value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            npMinute.value = Calendar.getInstance().get(Calendar.MINUTE)
        }

        btnConfirm.setOnClickListener {
            binding.etTime.text = String.format("%02d:%02d", npHour.value, npMinute.value)
            binding.etTime.error = null
            binding.etTime.setTextColor(Color.parseColor("#1E293B"))
            dialog.dismiss()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

// YearAdapter va boshqa yordamchi klasslar klassdan tashqarida saqlanadi
class YearAdapter(
    private val years: List<Int>,
    private val selectedYear: Int,
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<YearAdapter.YearViewHolder>() {
    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvYear: TextView = itemView.findViewById(R.id.tvYearItem)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        return YearViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_year_text, parent, false))
    }
    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        val year = years[position]
        holder.tvYear.text = year.toString()
        if (year == selectedYear) {
            holder.tvYear.setTextColor(Color.parseColor("#2E5BFF")); holder.tvYear.textSize = 20f
        } else {
            holder.tvYear.setTextColor(Color.parseColor("#1E293B")); holder.tvYear.textSize = 16f
        }
        holder.itemView.setOnClickListener { onYearClick(year) }
    }
    override fun getItemCount(): Int = years.size
}
