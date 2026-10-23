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
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionName: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("User-Agent", "Zhi-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    // No releases published yet on GitHub
                    return@withContext Result.success(
                        UpdateInfo(
                            hasUpdate = false,
                            latestVersion = currentVersionName,
                            releaseTag = "v$currentVersionName",
                            releaseNotes = "暂无公开发布版本。",
                            apkUrl = RELEASES_PAGE,
                            htmlUrl = RELEASES_PAGE
                        )
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API 响应失败 (${response.code})"))
                }

                val jsonStr = response.body?.string() ?: ""
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

                val hasUpdate = isNewerVersion(tagName, currentVersionName)

                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        releaseTag = root.optString("tag_name", "v$tagName"),
                        releaseNotes = body,
                        apkUrl = apkDownloadUrl,
                        htmlUrl = htmlUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
