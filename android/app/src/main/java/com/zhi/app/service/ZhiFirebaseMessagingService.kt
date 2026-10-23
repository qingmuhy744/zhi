package com.zhi.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zhi.app.MainActivity
import com.zhi.app.R
import com.zhi.app.data.local.ZhiDatabase
import com.zhi.app.data.model.PushMessage
import com.zhi.app.data.preferences.UserPreferences
import com.zhi.app.data.remote.ServerApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ZhiFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ZhiMessagingService"
        private val notificationIdCounter = AtomicInteger(1000)

        const val CHANNEL_DEFAULT = "zhi_channel_default"
        const val CHANNEL_URGENT = "zhi_channel_urgent"
        const val CHANNEL_GROUP_PREFIX = "zhi_group_"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")

        val prefs = UserPreferences(applicationContext)
        val apiClient = ServerApiClient()

        serviceScope.launch {
            prefs.setFCMToken(token)
            val serverUrl = prefs.effectiveServerUrlFlow.first()
            val deviceKey = prefs.getOrCreateDeviceKey()
            if (deviceKey.isNotEmpty() && serverUrl.isNotEmpty()) {
                val result = apiClient.registerDevice(serverUrl, deviceKey, token)
                if (result.isSuccess) {
                    prefs.setRegistered(true)
                    Log.d(TAG, "Device registered to $serverUrl successfully")
                } else {
                    prefs.setRegistered(false)
                    Log.w(TAG, "Registration failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification
        val prefs = UserPreferences(applicationContext)

        serviceScope.launch {
            var title = data["title"] ?: notification?.title ?: "Zhi"
            var body = data["body"] ?: notification?.body ?: ""
            var group = data["group"] ?: data["category"] ?: ""
            var icon = data["icon"] ?: ""
            var image = data["image"] ?: notification?.imageUrl?.toString() ?: ""
            var sound = data["sound"] ?: ""
            var url = data["url"] ?: ""
            var copy = data["copy"] ?: ""
            var badge = data["badge"]?.toIntOrNull() ?: 0
            var level = data["level"] ?: "active"
            var collapseKey = data["collapse_key"] ?: data["collapseKey"] ?: ""
            var actionsRaw = data["actions"] ?: ""
            var isArchive = data["isArchive"] != "0"

            // Scheme C: Check for ciphertext (End-to-End Encryption)
            val ciphertext = data["ciphertext"]
            if (!ciphertext.isNullOrEmpty()) {
                val aesKey = prefs.aesKeyFlow.first()
                if (aesKey.isEmpty()) {
                    title = "收到端到端加密消息"
                    body = "无法解密：未在 App 设置中配置 AES 密钥。"
                    level = "timeSensitive"
                } else {
                    try {
                        val decrypted = com.zhi.app.util.CryptoUtils.decrypt(ciphertext, aesKey, data["iv"])
                        // Try parsing decrypted content as JSON
                        try {
                            val json = org.json.JSONObject(decrypted)
                            if (json.has("title")) title = json.optString("title", title)
                            if (json.has("body")) body = json.optString("body", body)
                            if (json.has("group")) group = json.optString("group", group)
                            if (json.has("icon")) icon = json.optString("icon", icon)
                            if (json.has("image")) image = json.optString("image", image)
                            if (json.has("sound")) sound = json.optString("sound", sound)
                            if (json.has("url")) url = json.optString("url", url)
                            if (json.has("copy")) copy = json.optString("copy", copy)
                            if (json.has("badge")) badge = json.optInt("badge", badge)
                            if (json.has("level")) level = json.optString("level", level)
                            if (json.has("collapse_key")) collapseKey = json.optString("collapse_key", collapseKey)
                            if (json.has("actions")) actionsRaw = json.optString("actions", actionsRaw)
                            if (json.has("isArchive")) isArchive = json.optInt("isArchive", 1) != 0
                        } catch (_: Exception) {
                            // If not JSON, treat decrypted string as plain body
                            body = decrypted
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "E2EE Decryption failed", e)
                        title = "端到端消息解密失败"
                        body = "密钥不匹配或数据损坏，无法解密该推送。"
                        level = "timeSensitive"
                    }
                }
            }

            // Handle auto-copy if enabled
            if (copy.isNotEmpty()) {
                val autoCopy = prefs.autoCopyFlow.first()
                if (autoCopy) {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Zhi Auto-Copy", copy)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(applicationContext, "已自动复制: $copy", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Auto copy failed", e)
                        }
                    }
                }
            }

            // Save to Room Database
            if (isArchive) {
                val db = ZhiDatabase.getInstance(applicationContext)
                val message = PushMessage(
                    title = title,
                    body = body,
                    group = group,
                    icon = icon,
                    image = image,
                    sound = sound,
                    url = url,
                    copy = copy,
                    badge = badge,
                    level = level,
                    collapseKey = collapseKey,
                    isArchive = true,
                    receivedAt = System.currentTimeMillis()
                )
                db.messageDao().insertMessage(message)
            }

            // Show system notification
            showNotification(title, body, group, url, copy, level, image, collapseKey, actionsRaw)
        }
    }

    private data class WebhookActionSpec(
        val title: String,
        val url: String,
        val method: String = "GET"
    )

    private fun parseWebhookActions(actionsRaw: String): List<WebhookActionSpec> {
        val raw = actionsRaw.trim()
        if (raw.isEmpty()) return emptyList()
        val list = mutableListOf<WebhookActionSpec>()
        if (raw.startsWith("[")) {
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until minOf(arr.length(), 3)) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val t = obj.optString("title").trim()
                    val u = obj.optString("url").trim()
                    val m = obj.optString("method", "GET").uppercase()
                    if (t.isNotEmpty() && (u.startsWith("http://") || u.startsWith("https://"))) {
                        list.add(WebhookActionSpec(t, u, m))
                    }
                }
                return list
            } catch (_: Exception) {
                // fallback to shorthand below
            }
        }
        // Shorthand: "同意部署:https://example.com/approve|拒绝:https://example.com/reject"
        raw.split("|").take(3).forEach { segment ->
            val idx = segment.indexOf(":http")
            if (idx > 0) {
                val t = segment.substring(0, idx).trim()
                val u = segment.substring(idx + 1).trim()
                if (t.isNotEmpty() && (u.startsWith("http://") || u.startsWith("https://"))) {
                    list.add(WebhookActionSpec(t, u, "GET"))
                }
            }
        }
        return list
    }

    private fun showNotification(
        title: String,
        body: String,
        group: String,
        url: String,
        copy: String,
        level: String,
        image: String = "",
        collapseKey: String = "",
        actionsRaw: String = ""
    ) {
        val channelId = getOrCreateChannel(group, level)
        val notificationId = if (collapseKey.isNotEmpty()) {
            collapseKey.hashCode()
        } else {
            notificationIdCounter.incrementAndGet()
        }

        // Content intent when tapped
        val tapIntent = if (url.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingTapIntent = PendingIntent.getActivity(
            this,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingTapIntent)
            .setAutoCancel(true)

        // Asynchronously or directly load big picture image if URL provided
        var hasBigPicture = false
        if (image.isNotEmpty()) {
            try {
                val imageLoader = coil.ImageLoader(this)
                val request = coil.request.ImageRequest.Builder(this)
                    .data(image)
                    .allowHardware(false)
                    .build()
                val result = kotlinx.coroutines.runBlocking { imageLoader.execute(request) }
                val drawable = result.drawable
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(drawable.bitmap)
                            .setSummaryText(body)
                    )
                    builder.setLargeIcon(drawable.bitmap)
                    hasBigPicture = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load notification big picture from $image: ${e.message}")
            }
        }

        if (!hasBigPicture) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // Set priority according to level
        if (level == "timeSensitive") {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        } else if (level == "passive") {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        // Add "复制内容" action button
        if (copy.isNotEmpty()) {
            val copyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COPY
                putExtra(NotificationActionReceiver.EXTRA_COPY_TEXT, copy)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 1,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "复制: $copy", copyPendingIntent)
        }

        // Add "打开链接" action button if url is provided (using explicit Intent to prevent hijacking)
        if (url.isNotEmpty()) {
            val openUrlIntent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_URL
                putExtra(MainActivity.EXTRA_URL, url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openUrlPendingIntent = PendingIntent.getActivity(
                this,
                notificationId + 2,
                openUrlIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "打开链接", openUrlPendingIntent)
        }

        // Add interactive Webhook callback action buttons
        val webhookActions = parseWebhookActions(actionsRaw)
        webhookActions.forEachIndexed { index, spec ->
            val webhookIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_WEBHOOK
                putExtra(NotificationActionReceiver.EXTRA_WEBHOOK_URL, spec.url)
                putExtra(NotificationActionReceiver.EXTRA_WEBHOOK_TITLE, spec.title)
                putExtra(NotificationActionReceiver.EXTRA_WEBHOOK_METHOD, spec.method)
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val webhookPendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId + 10 + index,
                webhookIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, spec.title, webhookPendingIntent)
        }

        try {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }

    private fun getOrCreateChannel(group: String, level: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return CHANNEL_DEFAULT
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return when {
            level == "timeSensitive" -> {
                if (nm.getNotificationChannel(CHANNEL_URGENT) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_URGENT,
                        "紧急重要通知",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "高优先级推送通知"
                        enableVibration(true)
                        enableLights(true)
                        lightColor = Color.RED
                    }
                    nm.createNotificationChannel(channel)
                }
                CHANNEL_URGENT
            }
            group.isNotEmpty() -> {
                val channelId = "$CHANNEL_GROUP_PREFIX$group"
                if (nm.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "分组: $group",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "来自分组 $group 的通知"
                        enableVibration(true)
                    }
                    nm.createNotificationChannel(channel)
                }
                channelId
            }
            else -> {
                if (nm.getNotificationChannel(CHANNEL_DEFAULT) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_DEFAULT,
                        "常规通知",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "默认推送通知"
                        enableVibration(true)
                    }
                    nm.createNotificationChannel(channel)
                }
                CHANNEL_DEFAULT
            }
        }
    }
}
