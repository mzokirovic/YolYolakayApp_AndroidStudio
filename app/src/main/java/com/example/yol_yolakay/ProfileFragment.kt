package com.example.yol_yolakay

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Saqlangan ma'lumotlarni yuklash
        loadProfileData()

        // 2. Tablarni boshqarish
        binding.tabAboutYou.setOnClickListener { switchTab(true) }
        binding.tabAccount.setOnClickListener { switchTab(false) }

        // 3. Tahrirlash bosilganda
        binding.btnEditDetails.setOnClickListener {
            showEditDialog()
        }
    }

    private fun loadProfileData() {
        val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("USER_NAME", "Foydalanuvchi")
        val phone = sharedPref.getString("USER_PHONE", "+998")

        binding.tvUserName.text = name
        binding.tvUserPhone.text = phone
    }

    private fun switchTab(isAbout: Boolean) {
        if (isAbout) {
            // "Siz haqingizda" tabini ochish
            binding.layoutAboutYou.visibility = View.VISIBLE
            binding.layoutAccount.visibility = View.GONE

            binding.tvTabAbout.setTextColor(Color.parseColor("#00C896"))
            binding.lineTabAbout.visibility = View.VISIBLE

            binding.tvTabAccount.setTextColor(Color.parseColor("#757575"))
            binding.lineTabAccount.visibility = View.INVISIBLE
        } else {
            // "Hisob" tabini ochish
            binding.layoutAboutYou.visibility = View.GONE
            binding.layoutAccount.visibility = View.VISIBLE

            binding.tvTabAbout.setTextColor(Color.parseColor("#757575"))
            binding.lineTabAbout.visibility = View.INVISIBLE

            binding.tvTabAccount.setTextColor(Color.parseColor("#00C896"))
            binding.lineTabAccount.visibility = View.VISIBLE
        }
    }

    private fun showEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ma'lumotlarni tahrirlash")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Ism input
        val inputName = EditText(requireContext())
        inputName.hint = "Ismingiz"
        inputName.setText(binding.tvUserName.text)
        layout.addView(inputName)

        // Telefon input
        val inputPhone = EditText(requireContext())
        inputPhone.hint = "Telefon raqamingiz"
        inputPhone.inputType = android.text.InputType.TYPE_CLASS_PHONE
        inputPhone.setText(binding.tvUserPhone.text)
        layout.addView(inputPhone)

        builder.setView(layout)

        builder.setPositiveButton("Saqlash") { dialog, _ ->
            val newName = inputName.text.toString().trim()
            val newPhone = inputPhone.text.toString().trim()

            if (newName.isNotEmpty()) {
                // Xotiraga yozish
                val sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("USER_NAME", newName)
                editor.putString("USER_PHONE", newPhone)
                editor.apply()

                // Ekranni yangilash
                binding.tvUserName.text = newName
                binding.tvUserPhone.text = newPhone
                Toast.makeText(context, "Saqlandi!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Bekor qilish") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
