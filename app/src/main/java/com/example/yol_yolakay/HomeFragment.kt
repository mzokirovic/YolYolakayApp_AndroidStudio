package com.example.yol_yolakay

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SANA TANLASH DIALOGINI SOZLASH
        setupDatePicker()

        // QIDIRISH TUGMASI BOSILGANDA
        binding.btnSearch.setOnClickListener {
            // 1. Ma'lumotlarni o'qiymiz
            val from = binding.etSearchFrom.text.toString().trim()
            val to = binding.etSearchTo.text.toString().trim()
            val date = binding.etSearchDate.text.toString().trim()

            // 2. Tekshiramiz (Validatsiya)
            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(context, "Iltimos, 'Qayerdan' va 'Qayerga' maydonlarini to'ldiring", Toast.LENGTH_SHORT).show()
            } else {
                // 3. Yangi oynaga (SearchResultsActivity) o'tamiz
                val intent = Intent(requireContext(), SearchResultActivity::class.java)
                intent.putExtra("FROM_CITY", from)
                intent.putExtra("TO_CITY", to)
                intent.putExtra("DATE", date)
                startActivity(intent)
            }
        }
    }

    private fun setupDatePicker() {
        binding.etSearchDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    // Oy 0 dan boshlangani uchun +1 qo'shamiz
                    val formattedDate = String.format("%02d.%02d.%d", day, month + 1, year)
                    binding.etSearchDate.setText(formattedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // O'tib ketgan sanalarni tanlashni cheklash
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
