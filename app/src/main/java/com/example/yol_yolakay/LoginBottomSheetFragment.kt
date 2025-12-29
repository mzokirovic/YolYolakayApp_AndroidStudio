package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.yol_yolakay.databinding.FragmentLoginBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LoginBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentLoginBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // X tugmasi - yopish
        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        // Kirish tugmasi
        binding.btnOptionLogin.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        // Ro'yxatdan o'tish tugmasi
        binding.btnOptionRegister.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
