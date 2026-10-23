package com.zhi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.zhi.app.service.ZhiFirebaseMessagingService

class ZhiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultChannel = NotificationChannel(
                ZhiFirebaseMessagingService.CHANNEL_DEFAULT,
                getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_default_desc)
                enableVibration(true)
                enableLights(true)
            }

            val urgentChannel = NotificationChannel(
                ZhiFirebaseMessagingService.CHANNEL_URGENT,
                getString(R.string.channel_urgent_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_urgent_desc)
                enableVibration(true)
                enableLights(true)
                lightColor = Color.RED
            }

            nm.createNotificationChannels(listOf(defaultChannel, urgentChannel))
        }
    }
}
