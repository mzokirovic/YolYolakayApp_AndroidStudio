package com.example.yol_yolakay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.yol_yolakay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ilova ochilganda HomeFragment tursin
        loadFragment(HomeFragment())

        // MainActivity.kt ichida

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddTripActivity::class.java))
                    true
                }

                // --- O'ZGARTIRILGAN QISM ---
                // Agar menyuyingizda ID "nav_favorites" bo'lsa, shunday qolaversin,
                // lekin ichida TripsFragment() ochilishi kerak.
                // Agar menyuda "nav_trips" qilgan bo'lsangiz, R.id.nav_trips deb yozing.

                R.id.nav_trips -> {  // Yoki R.id.nav_favorites (Menyu fayliga qarang)
                    loadFragment(TripsFragment()) // <--- MANA SHU YERDA TripsFragment ochilishi kerak
                    true
                }
                // ---------------------------

                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }


    }

    // Fragmentni almashtirish funksiyasi
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
