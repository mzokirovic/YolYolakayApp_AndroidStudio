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
                    // E'lon berish oynasini ochamiz
                    startActivity(Intent(this, AddTripActivity::class.java))
                    // O'ZGARTIRILDI: false o'rniga true qaytaramiz.
                    // Bu tizimga "tugma bosildi va qayta ishlandi" deb xabar beradi.
                    true
                }
                R.id.nav_favorites -> {
                    loadFragment(FavoritesFragment())
                    true
                }
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
