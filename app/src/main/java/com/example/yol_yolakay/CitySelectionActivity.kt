package com.example.yol_yolakay

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapters.CityAdapter // Adapterni to'g'ri papkadan olish
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
            binding.etSearch.hint = "Qayerdan ketasiz?"
            // Ikonka resurslari mavjudligiga ishonch hosil qiling, yo'q bo'lsa standartini qo'ying
            binding.imgSearchIcon.setImageResource(R.drawable.ic_search)
        } else {
            binding.tvTitle.text = "Borish manzili"
            binding.etSearch.hint = "Qayerga borasiz?"
            binding.imgSearchIcon.setImageResource(R.drawable.ic_search)
            binding.imgSearchIcon.setColorFilter(Color.RED)
        }

        binding.etSearch.requestFocus()
        binding.btnClose.setOnClickListener { finish() }

        // XML faylda btnUseLocation borligini tekshiring
        binding.btnUseLocation.setOnClickListener {
            returnResult("Toshkent (Lokatsiya)")
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }
    }

    private fun setupRecyclerView() {
        // Adapter String ro'yxatini qabul qiladi
        adapter = CityAdapter(allCities) { selectedCity ->
            returnResult(selectedCity)
        }

        binding.rvCities.layoutManager = LinearLayoutManager(this)
        binding.rvCities.adapter = adapter
    }

    private fun setupSearchLogic() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString()

                if (searchText.isNotEmpty()) {
                    binding.btnClear.visibility = View.VISIBLE
                    filter(searchText)
                } else {
                    binding.btnClear.visibility = View.GONE
                    adapter.filterList(allCities)
                }
            }
        })
    }

    private fun filter(text: String) {
        val filteredList = ArrayList<String>()
        for (city in allCities) {
            if (city.lowercase().contains(text.lowercase())) {
                filteredList.add(city)
            }
        }
        adapter.filterList(filteredList)
    }

    private fun returnResult(city: String) {
        val intent = Intent()
        intent.putExtra("SELECTED_CITY", city)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
