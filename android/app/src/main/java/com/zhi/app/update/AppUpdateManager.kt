package com.zhi.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseTag: String,
    val releaseNotes: String,
    val apkUrl: String,
    val htmlUrl: String
)

class AppUpdateManager(private val context: Context) {

    companion object {
        const val GITHUB_REPO = "qingmuhy744/zhi"
        const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
        const val RELEASES_PAGE = "https://github.com/$GITHUB_REPO/releases"
        private const val AUTOMATIC_CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val preferences = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)

    suspend fun checkForUpdate(currentVersionName: String, manual: Boolean = true): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!manual && now - preferences.getLong("last_attempt", 0L) < AUTOMATIC_CHECK_INTERVAL_MS) {
            val cached = preferences.getString("latest_release", null)
            return@withContext if (cached.isNullOrBlank()) {
                Result.success(noRelease(currentVersionName))
            } else {
                runCatching { parseRelease(cached, currentVersionName) }
            }
        }
        preferences.edit().putLong("last_attempt", now).apply()

        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("User-Agent", "Zhi-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // No releases published yet on GitHub
                    preferences.edit().remove("latest_release").apply()
                    return@withContext Result.success(
                        noRelease(currentVersionName)
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API 响应失败 (${response.code})"))
                }

                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isBlank()) return@withContext Result.failure(Exception("GitHub 返回内容为空"))
                preferences.edit().putString("latest_release", jsonStr).apply()
                Result.success(parseRelease(jsonStr, currentVersionName))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun noRelease(currentVersionName: String) = UpdateInfo(
        hasUpdate = false,
        latestVersion = currentVersionName,
        releaseTag = "v$currentVersionName",
        releaseNotes = "暂无公开发布版本。",
        apkUrl = RELEASES_PAGE,
        htmlUrl = RELEASES_PAGE
    )

    private fun parseRelease(jsonStr: String, currentVersionName: String): UpdateInfo {
        val root = JSONObject(jsonStr)

        val tagName = root.optString("tag_name", "").removePrefix("v").trim()
        val body = root.optString("body", "暂无更新说明。")
        val htmlUrl = root.optString("html_url", RELEASES_PAGE)

        var apkDownloadUrl = htmlUrl
        val assets = root.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                    break
                }
            }
        }

        return UpdateInfo(
            hasUpdate = isNewerVersion(tagName, currentVersionName),
            latestVersion = tagName,
            releaseTag = root.optString("tag_name", "v$tagName"),
            releaseNotes = body,
            apkUrl = apkDownloadUrl,
            htmlUrl = htmlUrl
        )
    }

    fun openDownloadPage(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isEmpty() || current.isEmpty()) return false
        val vLatest = parseVersion(latest)
        val vCurrent = parseVersion(current)

        for (i in 0 until maxOf(vLatest.size, vCurrent.size)) {
            val numLatest = vLatest.getOrElse(i) { 0 }
            val numCurrent = vCurrent.getOrElse(i) { 0 }
            if (numLatest > numCurrent) return true
            if (numLatest < numCurrent) return false
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> {
        return v.split(".")
            .map { part ->
                part.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
    }
}
