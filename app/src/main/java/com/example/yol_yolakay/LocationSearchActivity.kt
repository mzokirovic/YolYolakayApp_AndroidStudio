package com.example.yol_yolakay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yol_yolakay.adapter.LocationAdapter // <--- BU IMPORT JUDA MUHIM
import com.example.yol_yolakay.databinding.ActivityLocationSearchBinding

class LocationSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationSearchBinding
    private lateinit var locationAdapter: LocationAdapter

    // O'zbekistonning barcha asosiy shaharlari
    private val allCities = listOf(
        "Toshkent", "Samarqand", "Buxoro", "Xiva", "Andijon",
        "Farg'ona", "Namangan", "Qarshi", "Termiz", "Urganch",
        "Nukus", "Jizzax", "Navoiy", "Guliston", "Qo'qon", "Shahrisabz"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar sozlamalari
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Adapter va RecyclerView sozlamalari
        locationAdapter = LocationAdapter(allCities) { city ->
            // Shahar tanlanganda natijani HomeFragment'ga qaytarish
            val resultIntent = Intent()
            resultIntent.putExtra("selected_location", city)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        binding.rvLocations.layoutManager = LinearLayoutManager(this)
        binding.rvLocations.adapter = locationAdapter

        // Qidiruv maydoni (EditText) o'zgarishini kuzatish
        binding.etSearchLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Adapterni filtrlash
                locationAdapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Klaviatura darhol chiqib turishi uchun (ixtiyoriy)
        binding.etSearchLocation.requestFocus()
    }
}
