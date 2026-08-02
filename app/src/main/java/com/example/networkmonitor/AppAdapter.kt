package com.example.networkmonitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var appList: List<AppModel>,
    private val onItemClick: (AppModel) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtMobile: TextView = view.findViewById(R.id.txtMobile)
        val txtWifi: TextView = view.findViewById(R.id.txtWifi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = appList[position]
        holder.txtName.text = item.appName
        holder.imgIcon.setImageDrawable(item.icon)
        holder.txtMobile.text = "Mobile: " + formatBytes(item.mobileUsageBytes)
        holder.txtWifi.text = "Wi-Fi: " + formatBytes(item.wifiUsageBytes)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = appList.size

    fun updateList(newList: List<AppModel>) {
        appList = newList
        notifyDataSetChanged()
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
}
