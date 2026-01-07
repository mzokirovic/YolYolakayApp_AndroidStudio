package com.example.yol_yolakay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout // <-- MUHIM: Button emas, FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yol_yolakay.R
import com.example.yol_yolakay.model.UserRequest

class RequestAdapter(
    private val requestList: ArrayList<UserRequest>,
    private val onAction: (UserRequest, Boolean) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRequesterName)

        // SIZNING XML FAYLINGIZDA BULAR FRAMELAYOUT EKAN:
        val btnAccept: FrameLayout = itemView.findViewById(R.id.btnAccept)
        val btnDecline: FrameLayout = itemView.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        holder.tvName.text = if (request.name.isNotEmpty()) request.name else "Noma'lum"

        // FrameLayout bo'lsa ham setOnClickListener ishlayveradi
        holder.btnAccept.setOnClickListener { onAction(request, true) }
        holder.btnDecline.setOnClickListener { onAction(request, false) }
    }

    override fun getItemCount(): Int = requestList.size
}
