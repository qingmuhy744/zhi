package com.zhi.app.data.remote

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun registerDevice(
        serverUrl: String,
        deviceKey: String,
        fcmToken: String,
        deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/register"
            val json = JSONObject().apply {
                put("device_key", deviceKey)
                put("fcm_token", fcmToken)
                put("device_name", deviceName)
            }
            val body = json.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(respStr)
                } else {
                    Result.failure(Exception("Registration failed (${response.code}): $respStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ping(serverUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/ping"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTestPush(
        serverUrl: String,
        deviceKey: String,
        title: String = "测试推送",
        body: String = "恭喜！Zhi 推送服务工作正常！",
        group: String = "系统测试"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "${serverUrl.removeSuffix("/")}/push"
            val json = JSONObject().apply {
                put("device_key", deviceKey)
                put("title", title)
                put("body", body)
                put("group", group)
                put("copy", "这是自动测试复制的内容")
                put("level", "timeSensitive")
            }
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Push failed (${response.code}): ${response.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
