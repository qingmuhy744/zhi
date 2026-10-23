package com.zhi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class PushMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val group: String = "",
    val icon: String = "",
    val image: String = "",
    val sound: String = "",
    val url: String = "",
    val copy: String = "",
    val badge: Int = 0,
    val level: String = "active", // active, timeSensitive, passive
    val collapseKey: String = "",
    val isArchive: Boolean = true,
    val receivedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
