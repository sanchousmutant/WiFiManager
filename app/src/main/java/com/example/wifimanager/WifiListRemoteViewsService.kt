package com.example.wifimanager

import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WifiListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WifiListRemoteViewsFactory(applicationContext)
    }
}

class WifiListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var wifiList: List<ScanResult> = emptyList()
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var currentSsid: String? = null

    override fun onCreate() {
        updateWifiList()
    }

    override fun onDataSetChanged() {
        updateWifiList()
    }

    private fun updateWifiList() {
        // Получаем текущую сеть
        val connectionInfo = wifiManager.connectionInfo
        currentSsid = connectionInfo?.ssid?.removeSurrounding("\"")

        // Запускаем сканирование
        wifiManager.startScan()
        
        // Получаем результаты сканирования
        wifiList = wifiManager.scanResults.distinctBy { it.SSID }.filter { it.SSID.isNotEmpty() }
    }

    override fun onDestroy() {
        wifiList = emptyList()
    }

    override fun getCount(): Int = wifiList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= wifiList.size) {
            return RemoteViews(context.packageName, R.layout.wifi_list_item)
        }

        val network = wifiList[position]
        val views = RemoteViews(context.packageName, R.layout.wifi_list_item)

        // Устанавливаем имя сети
        val ssid = network.SSID
        views.setTextViewText(R.id.wifi_name, ssid)

        // Отмечаем текущую сеть
        if (ssid == currentSsid) {
            views.setTextColor(R.id.wifi_name, context.getColor(android.R.color.holo_green_light))
        } else {
            views.setTextColor(R.id.wifi_name, context.getColor(android.R.color.white))
        }

        // Добавляем уровень сигнала
        val signalLevel = WifiManager.calculateSignalLevel(network.level, 5)
        views.setTextViewText(R.id.wifi_signal, "$signalLevel/5")

        // Создаем fillInIntent для обработки нажатия
        val fillInIntent = Intent().apply {
            putExtra("network_ssid", ssid)
        }
        views.setOnClickFillInIntent(R.id.wifi_item_layout, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
} 