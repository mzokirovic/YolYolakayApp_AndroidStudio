package com.example.yol_yolakay

import android.app.Activity
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate = ""
    private var seatsCount = 1

    // Launcherlarni oldindan e'lon qilamiz
    private lateinit var fromCityLauncher: ActivityResultLauncher<Intent>
    private lateinit var toCityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // "Qayerdan" uchun
        fromCityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val city = result.data?.getStringExtra("SELECTED_CITY")
                if (_binding != null) {
                    binding.tvSearchFrom.text = city
                    binding.tvSearchFrom.setTextColor(resources.getColor(android.R.color.black, null))
                }
            }
        }

        // "Qayerga" uchun
        toCityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val city = result.data?.getStringExtra("SELECTED_CITY")
                if (_binding != null) {
                    binding.tvSearchTo.text = city
                    binding.tvSearchTo.setTextColor(resources.getColor(android.R.color.black, null))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()

        // Standart holatda bugungi sanani qo'yamiz
        if (selectedDate.isEmpty()) {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale("uz", "UZ"))
            selectedDate = dateFormat.format(calendar.time)
            binding.tvSearchDate.text = selectedDate
        }
    }

    private fun setupClickListeners() {
        binding.containerFrom.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "FROM")
            fromCityLauncher.launch(intent)
        }

        binding.containerTo.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "TO")
            toCityLauncher.launch(intent)
        }

        // ZAMONAVIY KALENDARNI CHAQIRISH
        binding.containerDate.setOnClickListener {
            showDatePicker()
        }

        binding.containerSeats.setOnClickListener {
            val seatPicker = SeatPickerFragment.newInstance(seatsCount)
            seatPicker.onSeatsConfirmed = { count ->
                seatsCount = count
                if (_binding != null) {
                    binding.tvSearchSeats.text = "$count yo'lovchi"
                }
            }
            seatPicker.show(parentFragmentManager, "seat_picker")
        }

        binding.btnSearch.setOnClickListener {
            val from = binding.tvSearchFrom.text.toString()
            val to = binding.tvSearchTo.text.toString()

            if (from == "Qayerdan" || to == "Qayerga") {
                Toast.makeText(requireContext(), "Iltimos, manzillarni tanlang!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(requireContext(), SearchResultActivity::class.java)
                intent.putExtra("FROM_CITY", from)
                intent.putExtra("TO_CITY", to)
                intent.putExtra("DATE", selectedDate)
                intent.putExtra("SEATS", seatsCount)
                startActivity(intent)
            }
        }
    }

    // --- ZAMONAVIY KALENDAR FUNKSIYASI ---
    // --- ZAMONAVIY KALENDAR FUNKSIYASI (BARABAN BILAN) ---
    // --- ZAMONAVIY KALENDAR FUNKSIYASI (HOME FRAGMENT UCHUN) ---
    // --- ZAMONAVIY KALENDAR FUNKSIYASI (HOME FRAGMENT UCHUN) ---
    private fun showDatePicker() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // XML faylni yuklaymiz
        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        dialog.setContentView(view)

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

        val btnConfirm = view.findViewById<View>(R.id.btnConfirmDate)
        val btnCancel = view.findViewById<View>(R.id.btnCancelDate)

        // Baraban elementlari
        val npDay = view.findViewById<android.widget.NumberPicker>(R.id.npDay)
        val npMonth = view.findViewById<android.widget.NumberPicker>(R.id.npMonth)
        val npYear = view.findViewById<android.widget.NumberPicker>(R.id.npYear)

        val dateFormatHeader = SimpleDateFormat("d MMMM", Locale("uz", "UZ"))
        val yearFormatHeader = SimpleDateFormat("yyyy", Locale("uz", "UZ"))
        val searchFormat = SimpleDateFormat("dd-MM-yyyy", Locale("uz", "UZ"))

        val calendar = Calendar.getInstance()

        // MUHIM FIX: Agar avval sana tanlangan bo'lsa, o'shani o'rnatamiz
        if (selectedDate.isNotEmpty()) {
            try {
                // selectedDate formatini tekshiramiz (dd-MM-yyyy)
                val date = searchFormat.parse(selectedDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        calendarView.minDate = System.currentTimeMillis() - 1000
        calendarView.date = calendar.timeInMillis

        tvDateHeader.text = dateFormatHeader.format(calendar.time)
        tvYearHeader.text = yearFormatHeader.format(calendar.time)

        // Vaqtinchalik tanlangan qiymatlar
        var tempDay = calendar.get(Calendar.DAY_OF_MONTH)
        var tempMonth = calendar.get(Calendar.MONTH)
        var tempYear = calendar.get(Calendar.YEAR)

        // 1. ASOSIY KALENDAR O'ZGARISHI
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            tempYear = year
            tempMonth = month
            tempDay = dayOfMonth

            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)

            tvDateHeader.text = dateFormatHeader.format(newDate.time)
            tvYearHeader.text = yearFormatHeader.format(newDate.time)
        }

        // 2. YILNI BOSGANDA (Barabanni ochish)
        tvYearHeader.setOnClickListener {
            if (wheelContainer.visibility == View.VISIBLE) {
                wheelContainer.visibility = View.GONE
                calendarView.visibility = View.VISIBLE
            } else {
                calendarView.visibility = View.GONE
                wheelContainer.visibility = View.VISIBLE

                // BARABANNI SOZLASH
                val currentCal = Calendar.getInstance()
                currentCal.set(tempYear, tempMonth, tempDay)

                // Oylar
                val months = arrayOf("YAN", "FEV", "MAR", "APR", "MAY", "IYN", "IYL", "AVG", "SEN", "OKT", "NOY", "DEK")
                npMonth.minValue = 0
                npMonth.maxValue = 11
                npMonth.displayedValues = months
                npMonth.value = tempMonth
                npMonth.wrapSelectorWheel = true

                // Yillar
                val thisYear = Calendar.getInstance().get(Calendar.YEAR)
                npYear.minValue = thisYear
                npYear.maxValue = thisYear + 10
                npYear.value = tempYear
                npYear.wrapSelectorWheel = false

                // Kunlar
                fun updateDaysInMonth() {
                    val tempCal = Calendar.getInstance()
                    tempCal.set(npYear.value, npMonth.value, 1)
                    val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                    npDay.minValue = 1
                    npDay.maxValue = maxDay
                    if (npDay.value > maxDay) npDay.value = maxDay
                }
                npDay.value = tempDay
                updateDaysInMonth()

                npYear.setOnValueChangedListener { _, _, _ -> updateDaysInMonth() }
                npMonth.setOnValueChangedListener { _, _, _ -> updateDaysInMonth() }
            }
        }

        // 3. TASDIQLASH
        btnConfirm.setOnClickListener {
            if (wheelContainer.visibility == View.VISIBLE) {
                tempDay = npDay.value
                tempMonth = npMonth.value
                tempYear = npYear.value
            }

            val newDate = Calendar.getInstance()
            newDate.set(tempYear, tempMonth, tempDay)

            // Formatlash: "05-01-2026"
            selectedDate = searchFormat.format(newDate.time)

            if (_binding != null) {
                // Rasmga mos format: "Qachon (05-01-2026)"
                // \n belgisi matnni yangi qatorga tushiradi
                binding.tvSearchDate.text = selectedDate
                binding.tvSearchDate.setTextColor(resources.getColor(android.R.color.black, null))
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// HomeFragment uchun maxsus YearAdapter (Nomlar to'qnashmasligi uchun YearAdapterHome deb nomladim)
class YearAdapterHome(
    private val years: List<Int>,
    private val selectedYear: Int,
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<YearAdapterHome.YearViewHolder>() {

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
