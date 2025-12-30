package com.example.yol_yolakay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Model: So'rov yuborgan odam haqida ma'lumot
data class UserRequest(
    val userId: String = "",
    val name: String = "",
    val phone: String = ""
)

class RequestAdapter(
    private val requestList: ArrayList<UserRequest>,
    private val onAction: (UserRequest, Boolean) -> Unit // true=Qabul, false=Rad
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRequesterName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnDecline: Button = itemView.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        holder.tvName.text = request.name

        // Qabul qilish (Yashil tugma)
        holder.btnAccept.setOnClickListener {
            onAction(request, true)
        }

        // Rad etish (Qizil tugma)
        holder.btnDecline.setOnClickListener {
            onAction(request, false)
        }
    }

    override fun getItemCount(): Int = requestList.size
}
