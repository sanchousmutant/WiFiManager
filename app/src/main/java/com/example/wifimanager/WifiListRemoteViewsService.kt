package com.example.wifimanager

import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WifiListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return WifiListRemoteViewsFactory(this.applicationContext)
    }
}

class WifiListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var wifiList: List<ScanResult> = emptyList()
    private var currentNetwork: String? = null
    private val TAG = "WifiListFactory"
    private var isUpdating = false
    private var isWifiEnabled = false

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        updateWifiList()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged")
        if (!isUpdating) {
            updateWifiList()
        }
    }

    private fun updateWifiList() {
        if (isUpdating) {
            Log.d(TAG, "Обновление уже выполняется")
            return
        }

        isUpdating = true
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            isWifiEnabled = wifiManager?.isWifiEnabled == true
            
            if (isWifiEnabled) {
                Log.d(TAG, "Wi-Fi включен, начинаем сканирование")
                val success = wifiManager?.startScan() ?: false
                Log.d(TAG, "Сканирование запущено: $success")
                
                if (success) {
                    // Даем время на сканирование
                    var attempts = 0
                    var results = wifiManager?.scanResults ?: emptyList()
                    while (results.isEmpty() && attempts < 5) {
                        Thread.sleep(1000)
                        results = wifiManager?.scanResults ?: emptyList()
                        attempts++
                        Log.d(TAG, "Попытка $attempts: найдено ${results.size} сетей")
                    }
                    
                    // Если сети не найдены, пробуем еще раз через 2 секунды
                    if (results.isEmpty()) {
                        Thread.sleep(2000)
                        results = wifiManager?.scanResults ?: emptyList()
                        Log.d(TAG, "Повторное сканирование: найдено ${results.size} сетей")
                    }
                    
                    if (results.isNotEmpty()) {
                        wifiList = results.sortedByDescending { it.level }
                        currentNetwork = wifiManager?.connectionInfo?.ssid?.removeSurrounding("\"")
                        Log.d(TAG, "Обновление завершено. Найдено сетей: ${wifiList.size}, текущая сеть: $currentNetwork")
                    } else {
                        Log.d(TAG, "Сети не найдены, сохраняем предыдущий список")
                    }
                } else {
                    Log.e(TAG, "Не удалось запустить сканирование")
                }
            } else {
                Log.d(TAG, "Wi-Fi выключен")
                wifiList = emptyList()
                currentNetwork = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении списка сетей", e)
            wifiList = emptyList()
            currentNetwork = null
        } finally {
            isUpdating = false
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        wifiList = emptyList()
        currentNetwork = null
    }

    override fun getCount(): Int {
        Log.d(TAG, "getCount: ${wifiList.size}")
        return wifiList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.wifi_list_item)
        try {
            if (position < wifiList.size) {
                val wifiItem = wifiList[position]
                val ssid = wifiItem.SSID
                val signalLevel = WifiManager.calculateSignalLevel(wifiItem.level, 5)

                Log.d(TAG, "Отображение сети: $ssid, уровень сигнала: $signalLevel")
                
                // Устанавливаем имя сети
                views.setTextViewText(R.id.wifi_name, ssid)
                
                // Устанавливаем уровень сигнала
                val signalText = "Сигнал: ${signalLevel + 1}/5"
                views.setTextViewText(R.id.signal_level, signalText)

                // Выделяем текущую сеть
                if (ssid == currentNetwork) {
                    views.setTextColor(R.id.wifi_name, android.R.color.holo_green_dark)
                    views.setTextColor(R.id.signal_level, android.R.color.holo_green_dark)
                } else {
                    views.setTextColor(R.id.wifi_name, android.R.color.white)
                    views.setTextColor(R.id.signal_level, android.R.color.white)
                }

                // Добавляем интент для открытия настроек Wi-Fi при нажатии
                val fillInIntent = Intent().apply {
                    action = android.provider.Settings.ACTION_WIFI_SETTINGS
                }
                views.setOnClickFillInIntent(R.id.wifi_name, fillInIntent)
            } else {
                Log.e(TAG, "Позиция $position выходит за пределы списка")
                views.setTextViewText(R.id.wifi_name, "Ошибка")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при создании view", e)
            views.setTextViewText(R.id.wifi_name, "Ошибка")
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
} 