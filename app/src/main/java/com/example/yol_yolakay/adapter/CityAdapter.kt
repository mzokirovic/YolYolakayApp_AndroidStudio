package com.example.yol_yolakay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CityAdapter(
    private var cities: List<String>,
    private val onCityClicked: (String) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    // Asl ro'yxatni saqlab qolish uchun (filtrlashda kerak bo'ladi)
    private var allCities = ArrayList<String>(cities)

    inner class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(android.R.id.text1)
        val icon: ImageView = itemView.findViewById(android.R.id.icon)

        // Agar maxsus layout ishlatmasak, Androidning oddiy list itemidan foydalanamiz
        // Lekin bizga sal chiroyliroq kerak, shuning uchun pastda o'zgartiramiz
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        // Androidning tayyor "simple_list_item_1" layoutini ishlatamiz (yoki o'zimiznikini yasashimiz mumkin)
        // Keling, osonroq bo'lishi uchun hozircha o'zimiz oddiy view yasaymiz
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city_suggestion, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.tvCityName.text = city

        holder.itemView.setOnClickListener {
            onCityClicked(city)
        }
    }

    override fun getItemCount(): Int = cities.size

    // Qidiruv funksiyasi
    fun filterList(filteredList: List<String>) {
        cities = filteredList
        notifyDataSetChanged()
    }
}
