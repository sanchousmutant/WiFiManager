package com.example.wifimanager

import android.content.Context
import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class WifiListAdapter(
    context: Context,
    private val currentNetwork: String?
) : ArrayAdapter<ScanResult>(context, R.layout.wifi_list_item) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.wifi_list_item, parent, false)

        val wifiItem = getItem(position)
        val ssid = wifiItem?.SSID ?: "Unknown"

        val textView = view.findViewById<TextView>(R.id.wifi_name)
        textView.text = ssid

        // Выделяем текущую сеть
        if (ssid == currentNetwork) {
            textView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
        } else {
            textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        return view
    }
} 