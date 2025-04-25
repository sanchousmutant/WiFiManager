package com.example.wifimanager

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast

class WifiWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_WIDGET_RECEIVER = "com.example.wifimanager.ACTION_WIDGET_RECEIVER"
        const val TOGGLE_WIFI_ACTION = "com.example.wifimanager.TOGGLE_WIFI"
        const val REFRESH_ACTION = "com.example.wifimanager.REFRESH"
        const val OPEN_SETTINGS_ACTION = "com.example.wifimanager.OPEN_SETTINGS"
        private var mRunnable: ActivationRunnable? = null
    }

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Обновляем каждый виджет в массиве
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Создаем RemoteViews для макета виджета
        val views = RemoteViews(context.packageName, R.layout.widget_wifi_layout)

        // Настраиваем адаптер для списка
        val serviceIntent = Intent(context, WifiListRemoteViewsService::class.java)
        views.setRemoteAdapter(R.id.wifi_list, serviceIntent)

        // Создаем интент для переключения Wi-Fi
        val toggleIntent = Intent(context, WifiWidgetProvider::class.java).apply {
            action = TOGGLE_WIFI_ACTION
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.toggle_wifi_button, togglePendingIntent)

        // Создаем интент для обновления
        val refreshIntent = Intent(context, WifiWidgetProvider::class.java).apply {
            action = REFRESH_ACTION
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

        // Получаем текущее состояние Wi-Fi
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var iconId = R.drawable.wifi_off
        var text = context.getString(R.string.wifi_unknown)

        when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_DISABLED -> {
                iconId = R.drawable.wifi_off
                text = context.getString(R.string.wifi_off)
            }
            WifiManager.WIFI_STATE_DISABLING -> {
                text = context.getString(R.string.wifi_disabling)
            }
            WifiManager.WIFI_STATE_ENABLING -> {
                text = context.getString(R.string.wifi_enabling)
            }
            WifiManager.WIFI_STATE_ENABLED -> {
                iconId = R.drawable.wifi_on
                val wifiInfo = wifiManager.connectionInfo
                text = if (wifiInfo.networkId != -1) {
                    val ssid = wifiInfo.ssid
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid.substring(1, ssid.length - 1)
                    } else {
                        ssid
                    }
                } else {
                    context.getString(R.string.wifi_on)
                }
            }
            WifiManager.WIFI_STATE_UNKNOWN -> {
                text = context.getString(R.string.wifi_unknown)
            }
        }

        // Обновляем вид виджета
        views.setTextViewText(R.id.wifi_status, text)
        views.setImageViewResource(R.id.toggle_wifi_button, iconId)

        // Настраиваем пустое состояние
        views.setEmptyView(R.id.wifi_list, R.id.empty_view)

        // Обновляем виджет
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_WIDGET_RECEIVER, TOGGLE_WIFI_ACTION -> {
                if (mRunnable == null) {
                    mRunnable = ActivationRunnable(this).apply {
                        setContext(context)
                    }
                    mHandler.postDelayed(mRunnable!!, 300)
                } else {
                    mRunnable?.setDouble(true)
                }
            }
            REFRESH_ACTION -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, WifiWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            WifiManager.WIFI_STATE_CHANGED_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
            WifiManager.RSSI_CHANGED_ACTION -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, WifiWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            OPEN_SETTINGS_ACTION -> {
                // Открываем настройки Wi-Fi
                val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK or
                           Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
                context.startActivity(settingsIntent)
            }
        }
    }

    private fun changeWiFiState(context: Context) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_DISABLED -> wifiManager.isWifiEnabled = true
            WifiManager.WIFI_STATE_ENABLED -> wifiManager.isWifiEnabled = false
        }
    }

    private inner class ActivationRunnable(private val widget: WifiWidgetProvider) : Runnable {
        private var context: Context? = null
        private var isDouble = false

        fun setContext(context: Context) {
            this.context = context
        }

        fun setDouble(isDouble: Boolean) {
            this.isDouble = isDouble
        }

        override fun run() {
            context?.let {
                if (!isDouble) {
                    widget.changeWiFiState(it)
                }
                mRunnable = null
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context != null && appWidgetManager != null) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
} 