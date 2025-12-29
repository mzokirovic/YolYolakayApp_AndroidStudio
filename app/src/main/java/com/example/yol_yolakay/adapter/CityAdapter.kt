package com.example.yol_yolakay.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.databinding.ItemLocationBinding // Yoki item_city_suggestion.xml

class CityAdapter(
    private var cities: List<String>, // Endi bu yerda String ro'yxati
    private val onCityClick: (String) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    inner class CityViewHolder(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cityName: String) {
            binding.tvLocationName.text = cityName // XML dagi ID

            binding.root.setOnClickListener {
                onCityClick(cityName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount(): Int = cities.size

    fun filterList(filteredCities: List<String>) {
        cities = filteredCities
        notifyDataSetChanged()
    }
}
