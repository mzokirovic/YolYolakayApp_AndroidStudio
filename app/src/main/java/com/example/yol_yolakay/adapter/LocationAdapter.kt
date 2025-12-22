package com.example.yol_yolakay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.R

class LocationAdapter(
    private var allLocations: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(), Filterable {

    var filteredLocations: List<String> = allLocations

    inner class LocationViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false) as TextView
        return LocationViewHolder(textView)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = filteredLocations[position]
        holder.textView.text = location
        holder.itemView.setOnClickListener {
            onItemClick(location)
        }
    }

    override fun getItemCount(): Int = filteredLocations.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase() ?: ""
                filteredLocations = if (charString.isEmpty()) {
                    allLocations
                } else {
                    allLocations.filter { it.lowercase().contains(charString) }
                }
                return FilterResults().apply { values = filteredLocations }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredLocations = if (results?.values == null)
                    ArrayList()
                else
                    results.values as List<String>
                notifyDataSetChanged()
            }
        }
    }
}
