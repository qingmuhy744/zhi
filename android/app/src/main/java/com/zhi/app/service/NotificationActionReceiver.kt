package com.zhi.app.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COPY = "com.zhi.app.ACTION_COPY"
        const val EXTRA_COPY_TEXT = "extra_copy_text"

        const val ACTION_WEBHOOK = "com.zhi.app.ACTION_WEBHOOK"
        const val EXTRA_WEBHOOK_TITLE = "extra_webhook_title"
        const val EXTRA_WEBHOOK_URL = "extra_webhook_url"
        const val EXTRA_WEBHOOK_METHOD = "extra_webhook_method"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COPY -> {
                val text = intent.getStringExtra(EXTRA_COPY_TEXT) ?: return
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Zhi Copy", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制: $text", Toast.LENGTH_SHORT).show()
            }

            ACTION_WEBHOOK -> {
                val title = intent.getStringExtra(EXTRA_WEBHOOK_TITLE) ?: "操作"
                val url = intent.getStringExtra(EXTRA_WEBHOOK_URL) ?: return
                val method = (intent.getStringExtra(EXTRA_WEBHOOK_METHOD) ?: "POST").uppercase()
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val reqBuilder = Request.Builder().url(url)
                        if (method == "GET") {
                            reqBuilder.get()
                        } else {
                            val json = JSONObject().apply {
                                put("action", title)
                                put("timestamp", System.currentTimeMillis())
                            }
                            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                            reqBuilder.method(method, body)
                        }

                        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    Toast.makeText(context, "已执行「$title」(${resp.code})", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "「$title」回调返回 HTTP ${resp.code}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "「$title」回调失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
