package com.zhi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.zhi.app.service.ZhiFirebaseMessagingService

class ZhiApp : Application() {

    private companion object {
        const val NOTIFICATION_CHANNELS_PREFS = "notification_channels"
        // Recreate channels after removing the app-level DND policy permission so
        // channels created by older builds cannot retain bypassDnd=true.
        const val NOTIFICATION_CHANNELS_VERSION = 7
        const val LEGACY_DEFAULT_CHANNEL = "zhi_channel_default"
        const val LEGACY_URGENT_CHANNEL = "zhi_channel_urgent"
        const val LEGACY_SILENT_CHANNEL = "zhi_channel_silent"
        const val LEGACY_DEFAULT_CHANNEL_V2 = "zhi_channel_default_v2"
        const val LEGACY_URGENT_CHANNEL_V2 = "zhi_channel_urgent_v2"
        const val LEGACY_SILENT_CHANNEL_V2 = "zhi_channel_silent_v2"
        const val LEGACY_DEFAULT_CHANNEL_V3 = "zhi_channel_default_v3"
        const val LEGACY_URGENT_CHANNEL_V3 = "zhi_channel_urgent_v3"
        const val LEGACY_SILENT_CHANNEL_V3 = "zhi_channel_silent_v3"
        const val LEGACY_URGENT_DND_CHANNEL = "zhi_channel_urgent_dnd"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            migrateNotificationChannels(nm)

            val defaultChannel = NotificationChannel(
                ZhiFirebaseMessagingService.CHANNEL_DEFAULT,
                getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_default_desc)
                setZhiSound(R.raw.zhi_notification)
                enableVibration(true)
                enableLights(true)
                setBypassDnd(false)
            }

            val urgentChannel = createUrgentChannel()

            val silentChannel = NotificationChannel(
                ZhiFirebaseMessagingService.CHANNEL_SILENT,
                getString(R.string.channel_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_silent_desc)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setBypassDnd(false)
            }

            nm.createNotificationChannels(listOf(defaultChannel, urgentChannel, silentChannel))
        }
    }

    private fun createUrgentChannel(): NotificationChannel {
        return NotificationChannel(
            ZhiFirebaseMessagingService.CHANNEL_URGENT,
            getString(R.string.channel_urgent_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_urgent_desc)
            setZhiSound(R.raw.zhi_notification_urgent)
            enableVibration(true)
            enableLights(true)
            lightColor = Color.RED
            // Follow the Android channel contract. The user may allow this
            // channel in system notification settings, but the app never asks
            // for an application-wide DND policy exception.
            setBypassDnd(false)
        }
    }

    private fun migrateNotificationChannels(nm: NotificationManager) {
        val prefs = getSharedPreferences(NOTIFICATION_CHANNELS_PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt("version", 0) >= NOTIFICATION_CHANNELS_VERSION) return

        nm.notificationChannels
            .filter { channel ->
                channel.id == LEGACY_URGENT_DND_CHANNEL ||
                    channel.id == LEGACY_DEFAULT_CHANNEL ||
                    channel.id == LEGACY_URGENT_CHANNEL ||
                    channel.id == LEGACY_SILENT_CHANNEL ||
                    channel.id == LEGACY_DEFAULT_CHANNEL_V2 ||
                    channel.id == LEGACY_URGENT_CHANNEL_V2 ||
                    channel.id == LEGACY_SILENT_CHANNEL_V2 ||
                    channel.id == LEGACY_DEFAULT_CHANNEL_V3 ||
                    channel.id == LEGACY_URGENT_CHANNEL_V3 ||
                    channel.id == LEGACY_SILENT_CHANNEL_V3 ||
                    channel.id == ZhiFirebaseMessagingService.CHANNEL_DEFAULT ||
                    channel.id == ZhiFirebaseMessagingService.CHANNEL_URGENT ||
                    channel.id == ZhiFirebaseMessagingService.CHANNEL_SILENT ||
                    channel.id.startsWith(ZhiFirebaseMessagingService.CHANNEL_GROUP_PREFIX)
            }
            .forEach { channel -> nm.deleteNotificationChannel(channel.id) }

        prefs.edit().putInt("version", NOTIFICATION_CHANNELS_VERSION).apply()
    }

    private fun NotificationChannel.setZhiSound(resourceId: Int) {
        val soundUri = Uri.parse("android.resource://$packageName/$resourceId")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        setSound(soundUri, audioAttributes)
    }
}
