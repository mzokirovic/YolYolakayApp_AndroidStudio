package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.databinding.ActivityCitySelectionBinding

class CitySelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitySelectionBinding
    private lateinit var adapter: CityAdapter

    // O'zbekistondagi barcha asosiy shaharlar va tumanlar ro'yxati
    private val allCities = listOf(
        "Toshkent", "Samarqand", "Buxoro", "Andijon", "Farg'ona",
        "Namangan", "Navoiy", "Jizzax", "Qarshi", "Termiz",
        "Nukus", "Urganch", "Xiva", "Qo'qon", "Marg'ilon",
        "Shahrisabz", "Guliston", "Angren", "Olmaliq", "Bekobod",
        "Chirchiq", "Yangiyo'l", "Denov", "To'rtko'l", "Zarafshon"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupSearchLogic()
    }

    private fun setupUI() {
        val type = intent.getStringExtra("TYPE") ?: "FROM"

        if (type == "FROM") {
            binding.tvTitle.text = "Jo'nash manzili"
            binding.etSearchCity.hint = "Qayerdan ketasiz?"
            binding.imgSearchIcon.setImageResource(R.drawable.shape_circle_green)
        } else {
            binding.tvTitle.text = "Borish manzili"
            binding.etSearchCity.hint = "Qayerga borasiz?"
            binding.imgSearchIcon.setImageResource(R.drawable.shape_circle_green)
            binding.imgSearchIcon.setColorFilter(android.graphics.Color.RED)
        }

        binding.etSearchCity.requestFocus()
        binding.btnClose.setOnClickListener { finish() }

        binding.btnUseLocation.setOnClickListener {
            returnResult("Toshkent (Lokatsiya)")
        }

        binding.btnClear.setOnClickListener {
            binding.etSearchCity.text.clear()
        }
    }

    private fun setupRecyclerView() {
        // Adapter yaratamiz. Bosilganda returnResult ishlaydi
        adapter = CityAdapter(allCities) { selectedCity ->
            returnResult(selectedCity)
        }

        binding.rvCities.layoutManager = LinearLayoutManager(this)
        binding.rvCities.adapter = adapter
    }

    private fun setupSearchLogic() {
        binding.etSearchCity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString()

                if (searchText.isNotEmpty()) {
                    binding.btnClear.visibility = View.VISIBLE
                    filter(searchText) // Filtrlash funksiyasini chaqiramiz
                } else {
                    binding.btnClear.visibility = View.GONE
                    // Agar bo'sh bo'lsa, hammasini yoki hech narsani ko'rsatish mumkin
                    // Hozircha hammasini ko'rsatamiz:
                    adapter.filterList(allCities)
                }
            }
        })
    }

    // 🔍 QIDIRUV MANTIGI SHU YERDA
    private fun filter(text: String) {
        val filteredList = ArrayList<String>()

        for (city in allCities) {
            // Katta-kichik harfga qaramasdan qidirish (ignoreCase = true)
            if (city.lowercase().contains(text.lowercase())) {
                filteredList.add(city)
            }
        }

        // Adapterga yangi ro'yxatni beramiz
        adapter.filterList(filteredList)
    }

    private fun returnResult(city: String) {
        val intent = Intent()
        intent.putExtra("SELECTED_CITY", city)
        setResult(RESULT_OK, intent)
        finish()
    }
}
