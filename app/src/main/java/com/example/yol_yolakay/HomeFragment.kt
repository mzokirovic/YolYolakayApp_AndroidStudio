package com.example.yol_yolakay

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentHomeBinding
import java.util.Calendar

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

        // -----------------------------------------------------------------------------
        // ENG ISHONCHLI USUL: Launcherlarni onCreate ichida ro'yxatdan o'tkazish
        // -----------------------------------------------------------------------------

        // "Qayerdan" uchun
        fromCityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val city = result.data?.getStringExtra("SELECTED_CITY")
                // _binding null emasligini tekshirish kerak, chunki natija kelganda view yo'qolgan bo'lishi mumkin
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
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            selectedDate = "$day-$month-$year"
            binding.tvSearchDate.text = "Bugun ($selectedDate)"
        }
    }

    private fun setupClickListeners() {
        // 1. "Qayerdan" bosilganda
        binding.containerFrom.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "FROM")
            fromCityLauncher.launch(intent)
        }

        // 2. "Qayerga" bosilganda
        binding.containerTo.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "TO")
            toCityLauncher.launch(intent)
        }

        // 3. Sana tanlash
        binding.containerDate.setOnClickListener {
            showDatePicker()
        }

        // 4. O'rindiqlar sonini tanlash
        binding.containerSeats.setOnClickListener {
            // Agar SeatPickerFragment mavjud bo'lsa:
            val seatPicker = SeatPickerFragment.newInstance(seatsCount)
            seatPicker.onSeatsConfirmed = { count ->
                seatsCount = count
                if (_binding != null) {
                    binding.tvSearchSeats.text = "$count yo'lovchi"
                }
            }
            seatPicker.show(parentFragmentManager, "seat_picker")
        }

        // 5. QIDIRISH tugmasi
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

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val correctMonth = selectedMonth + 1
                selectedDate = "$selectedDay-$correctMonth-$selectedYear"
                if (_binding != null) {
                    binding.tvSearchDate.text = selectedDate
                    binding.tvSearchDate.setTextColor(resources.getColor(android.R.color.black, null))
                }
            },
            year, month, day
        )
        // O'tib ketgan sanalarni tanlay olmaslik uchun
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
