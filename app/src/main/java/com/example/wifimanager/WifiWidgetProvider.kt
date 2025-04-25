package com.example.wifimanager

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast

class WifiWidgetProvider : AppWidgetProvider() {
    companion object {
        const val TOGGLE_WIFI_ACTION = "com.example.wifimanager.TOGGLE_WIFI"
        const val REFRESH_ACTION = "com.example.wifimanager.REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            TOGGLE_WIFI_ACTION -> {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                Toast.makeText(context, 
                    if (wifiManager.isWifiEnabled) "Wi-Fi включен" else "Wi-Fi выключен",
                    Toast.LENGTH_SHORT).show()
                
                // Обновляем все виджеты после переключения Wi-Fi
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, WifiWidgetProvider::class.java)
                )
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            REFRESH_ACTION -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, WifiWidgetProvider::class.java)
                )
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_wifi_layout)
        
        // Получаем статус Wi-Fi
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiEnabled = wifiManager.isWifiEnabled
        
        // Обновляем статус Wi-Fi
        views.setTextViewText(R.id.wifi_status, 
            if (isWifiEnabled) "Wi-Fi включен" else "Wi-Fi выключен")
        
        // Настраиваем RemoteViewsService для списка
        val serviceIntent = Intent(context, WifiListRemoteViewsService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        serviceIntent.data = android.net.Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.wifi_list, serviceIntent)

        // Создаем интент для обновления списка
        val refreshIntent = Intent(context, WifiWidgetProvider::class.java).apply {
            action = REFRESH_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_ONE_SHOT
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

        // Создаем интент для переключения Wi-Fi
        val toggleIntent = Intent(context, WifiWidgetProvider::class.java).apply {
            action = TOGGLE_WIFI_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_ONE_SHOT
        )
        views.setOnClickPendingIntent(R.id.toggle_wifi_button, togglePendingIntent)

        // Добавляем интент для открытия настроек Wi-Fi при нажатии на список
        val settingsIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                   Intent.FLAG_ACTIVITY_CLEAR_TASK or
                   Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                   Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            PendingIntent.FLAG_IMMUTABLE or
            PendingIntent.FLAG_ONE_SHOT
        )
        views.setPendingIntentTemplate(R.id.wifi_list, settingsPendingIntent)

        // Устанавливаем пустое состояние
        views.setEmptyView(R.id.wifi_list, R.id.empty_view)

        // Обновляем виджет
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
} 