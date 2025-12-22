package com.example.yol_yolakay // Yoki o'zingizning paketingiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.yol_yolakay.databinding.DialogSeatPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SeatPickerFragment : BottomSheetDialogFragment() {

    private lateinit var binding: DialogSeatPickerBinding
    private var currentSeats = 1

    // Natijani HomeFragmentga qaytarish uchun callback
    var onSeatsConfirmed: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogSeatPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Boshlang'ich qiymatni o'rnatish
        currentSeats = arguments?.getInt("current_seats") ?: 1
        updateSeatCountText()

        // "Minus" tugmasi bosilganda
        binding.btnMinus.setOnClickListener {
            if (currentSeats > 1) {
                currentSeats--
                updateSeatCountText()
            }
        }

        // "Plus" tugmasi bosilganda
        binding.btnPlus.setOnClickListener {
            if (currentSeats < 8) { // Maksimal 8 kishi
                currentSeats++
                updateSeatCountText()
            }
        }

        // "Tasdiqlash" tugmasi bosilganda
        binding.btnConfirm.setOnClickListener {
            onSeatsConfirmed?.invoke(currentSeats)
            dismiss() // Dialog oynasini yopish
        }
    }

    private fun updateSeatCountText() {
        binding.tvSeatCount.text = currentSeats.toString()
    }

    // Companion object orqali fragment yaratish qulayroq
    companion object {
        fun newInstance(currentSeats: Int): SeatPickerFragment {
            val fragment = SeatPickerFragment()
            val args = Bundle()
            args.putInt("current_seats", currentSeats)
            fragment.arguments = args
            return fragment
        }
    }
}
