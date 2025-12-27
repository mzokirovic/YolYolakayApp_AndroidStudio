package com.example.yol_yolakay

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Tanlangan sanani saqlash
    private var selectedDateString: String = ""
    // Tanlangan o'rindiqlar soni
    private var selectedSeatsCount: Int = 1

    // 1. "Qayerdan" oynasidan natijani qabul qilish
    private val fromLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Yangi Activityda kalit so'z "SELECTED_CITY" edi
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.tvSearchFrom.text = city
                binding.tvSearchFrom.setTextColor(Color.BLACK)
            }
        }
    }

    // 2. "Qayerga" oynasidan natijani qabul qilish
    private val toLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Yangi Activityda kalit so'z "SELECTED_CITY" edi
            val city = result.data?.getStringExtra("SELECTED_CITY")
            if (!city.isNullOrEmpty()) {
                binding.tvSearchTo.text = city
                binding.tvSearchTo.setTextColor(Color.BLACK)
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

        // 1. "Qayerdan" bosilganda -> Yangi CitySelectionActivity ochiladi
        binding.containerFrom.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "FROM") // Sarlavha "Jo'nash manzili" bo'lishi uchun
            fromLocationLauncher.launch(intent)
        }

        // 2. "Qayerga" bosilganda -> Yangi CitySelectionActivity ochiladi
        binding.containerTo.setOnClickListener {
            val intent = Intent(requireContext(), CitySelectionActivity::class.java)
            intent.putExtra("TYPE", "TO") // Sarlavha "Borish manzili" bo'lishi uchun
            toLocationLauncher.launch(intent)
        }

        // 3. Sana bosilganda -> Kalendar ochiladi
        binding.containerDate.setOnClickListener {
            showDatePicker()
        }

        // 4. O'rindiqlar soni bosilganda -> Dialog ochiladi
        binding.containerSeats.setOnClickListener {
            showSeatPicker()
        }

        // 5. Qidirish tugmasi
        binding.btnSearch.setOnClickListener {
            val from = binding.tvSearchFrom.text.toString()
            val to = binding.tvSearchTo.text.toString()

            // Tekshiruv
            if (from == "Qayerdan" || to == "Qayerga") {
                Toast.makeText(context, "Iltimos, manzilni tanlang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // SearchResultActivity ga ma'lumot jo'natish
            val intent = Intent(requireContext(), SearchResultActivity::class.java)
            intent.putExtra("FROM_CITY", from)
            intent.putExtra("TO_CITY", to)
            intent.putExtra("DATE", selectedDateString)
            intent.putExtra("SEATS", selectedSeatsCount)
            startActivity(intent)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDateString = String.format("%02d.%02d.%d", dayOfMonth, month + 1, year)
                binding.tvSearchDate.text = selectedDateString
                binding.tvSearchDate.setTextColor(Color.BLACK)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showSeatPicker() {
        val seatPickerFragment = SeatPickerFragment.newInstance(selectedSeatsCount)
        seatPickerFragment.show(childFragmentManager, "SeatPicker")

        seatPickerFragment.onSeatsConfirmed = { newSeatCount ->
            selectedSeatsCount = newSeatCount
            binding.tvSearchSeats.text = "$newSeatCount kishi"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
