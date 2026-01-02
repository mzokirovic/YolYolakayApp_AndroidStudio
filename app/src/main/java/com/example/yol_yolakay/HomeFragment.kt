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
            binding.tvSearchDate.text = "Bugun ($selectedDate)"
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
    private fun showDatePicker() {
        // Fragment ichida 'requireContext()' ishlatamiz
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = layoutInflater.inflate(R.layout.dialog_calendar_picker, null)
        dialog.setContentView(view)

        // Orqa fon shaffof
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
        val rvYears = view.findViewById<RecyclerView>(R.id.rvYears)
        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val tvYearHeader = view.findViewById<TextView>(R.id.tvYearHeader)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirmDate)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelDate)

        val dateFormatHeader = SimpleDateFormat("d MMMM", Locale("uz", "UZ"))
        val yearFormat = SimpleDateFormat("yyyy", Locale("uz", "UZ"))

        // Qidiruv uchun kerakli format (DD-MM-YYYY)
        val searchFormat = SimpleDateFormat("dd-MM-yyyy", Locale("uz", "UZ"))

        val calendar = Calendar.getInstance()
        // O'tmishdagi sanalar qidiruv uchun kerak emas
        calendarView.minDate = System.currentTimeMillis() - 1000

        tvDateHeader.text = dateFormatHeader.format(calendar.time)
        tvYearHeader.text = yearFormat.format(calendar.time)

        var selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        var selectedMonth = calendar.get(Calendar.MONTH)
        var selectedYear = calendar.get(Calendar.YEAR)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth

            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)

            tvDateHeader.text = dateFormatHeader.format(newDate.time)
            tvYearHeader.text = yearFormat.format(newDate.time)
        }

        // Yil bosilganda (Faqat shu yil va keyingi yil kerak xolos qidiruvga)
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

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                // Qidiruv uchun juda uzoq yillar shart emas, masalan keyingi 2 yil
                val yearsList = (currentYear..currentYear + 2).toList()

                rvYears.layoutManager = LinearLayoutManager(requireContext())
                rvYears.adapter = YearAdapterHome(yearsList, selectedYear) { clickedYear ->
                    selectedYear = clickedYear

                    val newDate = Calendar.getInstance()
                    newDate.set(selectedYear, selectedMonth, selectedDay)

                    // Agar tanlangan sana minDate dan kichik bo'lib qolsa (yil o'zgarganda)
                    if (newDate.timeInMillis < calendarView.minDate) {
                        newDate.timeInMillis = calendarView.minDate
                    }

                    calendarView.date = newDate.timeInMillis
                    tvYearHeader.text = clickedYear.toString()
                    tvDateHeader.text = dateFormatHeader.format(newDate.time)

                    rvYears.visibility = View.GONE
                    calendarView.visibility = View.VISIBLE
                }
            }
        }

        btnConfirm.setOnClickListener {
            // Tanlangan sanani formatlash
            val newDate = Calendar.getInstance()
            newDate.set(selectedYear, selectedMonth, selectedDay)
            selectedDate = searchFormat.format(newDate.time)

            if (_binding != null) {
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
