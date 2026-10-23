package com.zhi.app.service

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COPY = "com.zhi.app.ACTION_COPY"
        const val EXTRA_COPY_TEXT = "extra_copy_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COPY) {
            val text = intent.getStringExtra(EXTRA_COPY_TEXT) ?: return
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Zhi Copy", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已复制: $text", Toast.LENGTH_SHORT).show()
        }
    }
}
