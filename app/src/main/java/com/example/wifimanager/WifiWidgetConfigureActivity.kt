package com.example.wifimanager

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WifiWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val PERMISSIONS_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Устанавливаем результат по умолчанию
        setResult(RESULT_CANCELED)
        
        // Получаем ID виджета
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Проверяем и запрашиваем разрешения
        if (checkAndRequestPermissions()) {
            setupWidget()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupWidget()
            } else {
                finish()
            }
        }
    }

    private fun setupWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val views = RemoteViews(packageName, R.layout.widget_wifi_layout)
        
        // Настраиваем RemoteViewsService для списка
        val intent = Intent(this, WifiListRemoteViewsService::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        views.setRemoteAdapter(R.id.wifi_list, intent)

        // Создаем интент для обновления списка
        val refreshIntent = Intent(this, WifiWidgetProvider::class.java).apply {
            action = WifiWidgetProvider.REFRESH_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val refreshPendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            0,
            refreshIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or 
            android.app.PendingIntent.FLAG_IMMUTABLE or
            android.app.PendingIntent.FLAG_ONE_SHOT
        )
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

        // Создаем интент для переключения Wi-Fi
        val toggleIntent = Intent(this, WifiWidgetProvider::class.java).apply {
            action = WifiWidgetProvider.TOGGLE_WIFI_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val togglePendingIntent = android.app.PendingIntent.getBroadcast(
            this,
            0,
            toggleIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or 
            android.app.PendingIntent.FLAG_IMMUTABLE or
            android.app.PendingIntent.FLAG_ONE_SHOT
        )
        views.setOnClickPendingIntent(R.id.toggle_wifi_button, togglePendingIntent)

        // Добавляем интент для открытия настроек Wi-Fi при нажатии на список
        val settingsIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                   Intent.FLAG_ACTIVITY_CLEAR_TASK or
                   Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                   Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        val settingsPendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            settingsIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or 
            android.app.PendingIntent.FLAG_IMMUTABLE or
            android.app.PendingIntent.FLAG_ONE_SHOT
        )
        views.setPendingIntentTemplate(R.id.wifi_list, settingsPendingIntent)

        // Обновляем виджет
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Устанавливаем результат и завершаем активность
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setupWidget()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setupWidget()
                }
            }
        }
    }
} 