package com.zhi.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.zhi.app.data.preferences.UserPreferences
import com.zhi.app.ui.screens.MainScreen
import com.zhi.app.ui.theme.ZhiTheme
import com.zhi.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_OPEN_URL = "com.zhi.app.ACTION_OPEN_URL"
        const val EXTRA_URL = "extra_url"
    }

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        fetchFCMToken()
        handleOpenUrlIntent(intent)
        handleFcmSystemTrayIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            ZhiTheme(themeMode = themeMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenUrlIntent(intent)
        handleFcmSystemTrayIntent(intent)
    }

    private fun handleFcmSystemTrayIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        // When system_notify=1 is used and app is in background, Android system tray puts data keys into Activity intent extras
        val hasFcmId = extras.containsKey("google.message_id") || extras.containsKey("gcm.notification.body")
        val body = extras.getString("body")
        val ciphertext = extras.getString("ciphertext")
        if (!hasFcmId && body.isNullOrEmpty() && ciphertext.isNullOrEmpty()) return

        val title = extras.getString("title") ?: "Zhi"
        val group = extras.getString("group") ?: extras.getString("category") ?: ""
        val copy = extras.getString("copy") ?: ""
        val url = extras.getString("url") ?: ""
        val image = extras.getString("image") ?: ""
        val level = extras.getString("level") ?: "active"
        val collapseKey = extras.getString("collapse_key") ?: ""
        val iv = extras.getString("iv")

        // Remove key from intent so configuration change won't insert duplicate
        intent.removeExtra("body")
        intent.removeExtra("ciphertext")
        intent.removeExtra("google.message_id")

        viewModel.archiveFromSystemNotificationIntent(
            title = title,
            body = body ?: "",
            group = group,
            copy = copy,
            url = url,
            image = image,
            level = level,
            collapseKey = collapseKey,
            ciphertext = ciphertext,
            iv = iv
        )
    }

    private fun handleOpenUrlIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_URL) {
            val url = intent.getStringExtra(EXTRA_URL)
            if (!url.isNullOrBlank()) {
                try {
                    val parsed = Uri.parse(url)
                    if (parsed.scheme == "http" || parsed.scheme == "https") {
                        startActivity(Intent(Intent.ACTION_VIEW, parsed).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to launch intent for url: $url", e)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun fetchFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("MainActivity", "Fetched FCM token: $token")
                val prefs = UserPreferences(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    prefs.setFCMToken(token)
                    viewModel.syncToServer()
                }
            }
    }
}
