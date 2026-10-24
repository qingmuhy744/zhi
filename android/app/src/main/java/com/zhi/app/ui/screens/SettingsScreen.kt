package com.zhi.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhi.app.data.preferences.ServerMode
import com.zhi.app.data.preferences.ThemeMode
import com.zhi.app.data.preferences.UserPreferences
import com.zhi.app.ui.components.ServerStatusBadge
import com.zhi.app.ui.theme.BluePrimary
import com.zhi.app.ui.theme.RedDanger
import com.zhi.app.ui.theme.Slate200
import com.zhi.app.ui.theme.Slate400
import com.zhi.app.ui.theme.Slate500
import com.zhi.app.ui.theme.Slate700
import com.zhi.app.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val deviceKey by viewModel.deviceKey.collectAsState()
    val serverMode by viewModel.serverMode.collectAsState()
    val customServerUrl by viewModel.customServerUrl.collectAsState()
    val effectiveServerUrl by viewModel.effectiveServerUrl.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val autoCopy by viewModel.autoCopy.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val e2eeEnabled by viewModel.e2eeEnabled.collectAsState()
    val aesKey by viewModel.aesKey.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val subscribedTopics by viewModel.subscribedTopics.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember(customServerUrl) { mutableStateOf(customServerUrl) }
    var aesKeyInput by remember(aesKey) { mutableStateOf(aesKey) }
    var topicInput by remember { mutableStateOf("") }
    var showRegenerateKeyDialog by remember { mutableStateOf(false) }
    var isTestingPing by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupJson { json ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "备份文件已成功导出！", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    viewModel.importBackupJson(json) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取备份文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "设置与配置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "管理设备 Key、服务器来源与通知偏好",
            color = Slate400,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. 设备 Key 卡片
        SectionTitle(icon = Icons.Outlined.Key, title = "设备凭证 (Device Key)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "您的唯一设备标识（不可泄露给陌生人）", fontSize = 12.sp, color = Slate400)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceKey.ifEmpty { "正在生成设备 Key..." },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device Key", deviceKey))
                            Toast.makeText(context, "已复制 Device Key", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "复制", tint = Slate500, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showRegenerateKeyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重置并生成新的 Device Key", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 推送服务器配置 (官方默认 vs 自建服务)
        SectionTitle(icon = Icons.Outlined.Dns, title = "推送服务器 (Server Selection)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 选项 1: 官方默认服务
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setServerMode(ServerMode.DEFAULT) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = serverMode == ServerMode.DEFAULT,
                        onClick = { viewModel.setServerMode(ServerMode.DEFAULT) },
                        colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(text = "默认公共服务 (https://push.qingmuhy.com)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "开箱即用，零配置支持端到端加密盲转发", fontSize = 12.sp, color = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(8.dp))

                // 选项 2: 自建服务器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setServerMode(ServerMode.CUSTOM) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = serverMode == ServerMode.CUSTOM,
                        onClick = { viewModel.setServerMode(ServerMode.CUSTOM) },
                        colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(text = "自建私有服务器 (Self-Hosted)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "连接您自己部署的 Zhi 推送服务端", fontSize = 12.sp, color = Slate400)
                    }
                }

                // 自建服务器输入框
                if (serverMode == ServerMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("自建服务器地址") },
                        placeholder = { Text("https://push.yourdomain.com") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 测试连接按钮
                        OutlinedButton(
                            onClick = {
                                if (customUrlInput.isBlank()) {
                                    Toast.makeText(context, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isTestingPing = true
                                viewModel.testServerPing(customUrlInput) { success, msg ->
                                    isTestingPing = false
                                    Toast.makeText(context, if (success) "连接成功！$msg" else "连接失败: $msg", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isTestingPing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingPing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("测试连接", fontSize = 13.sp)
                            }
                        }

                        // 保存并切换按钮
                        Button(
                            onClick = {
                                if (customUrlInput.isBlank()) {
                                    Toast.makeText(context, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.setCustomServerUrl(customUrlInput)
                                Toast.makeText(context, "已保存自建服务器地址", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("保存并同步", fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "当前生效服务: $effectiveServerUrl",
                    fontSize = 12.sp,
                    color = Slate500,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. FCM 状态与服务器同步
        SectionTitle(icon = Icons.Outlined.Sync, title = "FCM 与同步状态")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "同步状态", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    ServerStatusBadge(isRegistered = isRegistered)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "FCM Registration Token:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (fcmToken.isEmpty()) "正在连接 Google FCM 服务..." else "${fcmToken.take(24)}...${fcmToken.takeLast(8)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (fcmToken.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", fcmToken))
                                Toast.makeText(context, "已复制完整 FCM Token", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "复制 Token", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    } else {
                        IconButton(
                            onClick = {
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        val p = com.zhi.app.data.preferences.UserPreferences(context)
                                        coroutineScope.launch {
                                            p.setFCMToken(token)
                                            viewModel.syncToServer()
                                        }
                                        Toast.makeText(context, "已成功获取 FCM Token 并同步", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "获取失败: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "刷新 Token", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.syncToServer { success, msg ->
                            Toast.makeText(context, if (success) "同步成功！" else "同步失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在同步...", fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即同步 Token 到服务器", fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. 群发与广播频道 (Topic Channels)
        SectionTitle(icon = Icons.Outlined.Campaign, title = "群发广播频道 (Topic Channels)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "支持多台手机同时订阅同一个秘密频道。发送方只需推送一次，所有订阅该频道的设备同时响铃，无需维护多设备列表。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = { Text("输入或生成频道 Secret (≥8位)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val t = topicInput.trim()
                            if (t.isNotEmpty()) {
                                viewModel.subscribeTopic(t) { ok, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (ok) topicInput = ""
                                }
                            }
                        },
                        enabled = topicInput.trim().length >= 8,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("订阅", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val newSecret = viewModel.generateTopicSecret()
                        topicInput = newSecret
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("随机生成高强度频道 Secret", fontSize = 12.sp)
                }

                if (subscribedTopics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "已订阅的广播频道 (${subscribedTopics.size})：", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    subscribedTopics.forEach { topic ->
                        val pushUrl = "$effectiveServerUrl/$topic/广播测试/大家都能收到这条消息！"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "点击右侧复制推送 URL",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Topic Push URL", pushUrl))
                                        Toast.makeText(context, "已复制群发推送 URL", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "复制URL", tint = Slate500, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.unsubscribeTopic(topic) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "退订", tint = RedDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. 端到端加密 (E2EE) 配置
        SectionTitle(icon = Icons.Outlined.Lock, title = "端到端加密 (E2EE - 方案 C)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PreferenceSwitchRow(
                    title = "开启端到端加密",
                    subtitle = "公网网关盲目转发密文，零知识无法查看任何明文",
                    checked = e2eeEnabled,
                    onCheckedChange = { viewModel.setE2EEEnabled(it) }
                )

                if (e2eeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Slate200)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "AES 加密密钥 (Key Passphrase):", fontSize = 12.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = aesKeyInput,
                        onValueChange = {
                            aesKeyInput = it
                            viewModel.setAesKey(it)
                        },
                        placeholder = { Text("输入或生成 16/32 位 AES 密钥") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.generateNewAesKey()
                                Toast.makeText(context, "已生成新的 AES-256 密钥", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("生成新密钥", fontSize = 12.sp)
                        }

                        if (aesKey.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Zhi AES Key", aesKey))
                                    Toast.makeText(context, "已复制 AES 密钥", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("复制密钥", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：发送端使用此密钥加密 JSON 后发送至 /push（携带 ciphertext 字段），手机收到后在本地秒级解密展示。",
                        fontSize = 12.sp,
                        color = Slate500,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. 通知与外观偏好设置
        SectionTitle(icon = Icons.Outlined.Notifications, title = "外观与通知首选项")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "界面主题模式 (Theme Mode)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = "支持跟随系统、荒原米白（浅色）、曜石深邃黑（深色）", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeChip(
                        title = "跟随系统",
                        icon = Icons.Outlined.BrightnessAuto,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeChip(
                        title = "米白浅色",
                        icon = Icons.Outlined.LightMode,
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeChip(
                        title = "曜石深色",
                        icon = Icons.Outlined.DarkMode,
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 14.dp))

                // 自动复制
                PreferenceSwitchRow(
                    title = "自动复制 (Auto Copy)",
                    subtitle = "收到含 copy 参数的通知时自动写入剪贴板",
                    checked = autoCopy,
                    onCheckedChange = { viewModel.setAutoCopy(it) }
                )

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // 声音
                PreferenceSwitchRow(
                    title = "通知声音",
                    subtitle = "收到新推送时播放提示音",
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // 震动
                PreferenceSwitchRow(
                    title = "通知震动",
                    subtitle = "收到新推送时震动提醒",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // 系统通知设置页面快捷跳转
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "系统通知权限与渠道设置", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(text = "跳转至系统设置调整通知渠道、声音、振动与横幅", fontSize = 12.sp, color = Slate400)
                    }
                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 12.dp))

                // 电池优化白名单（兼顾后台高优先级 Data 唤醒与端到端解密/大图/按钮）
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(fallback)
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "后台省电白名单 (已开启无限制 ✅)" else "开启后台省电无限制白名单 (推荐)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isIgnoringBatteryOptimizations) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "允许后台唤醒以保障锁屏状态下正常解密 E2EE、加载大图、展示交互按钮与落盘归档",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. 数据备份与迁移 (Backup & Restore)
        SectionTitle(icon = Icons.Outlined.Sync, title = "数据备份与迁移 (Backup & Restore)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "支持将设备 Key、服务器地址、E2EE 密钥、已订阅频道以及全部历史推送消息打包导出为 JSON，方便换机或跨设备恢复。",
                    fontSize = 12.sp,
                    color = Slate400,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportFileLauncher.launch("zhi-backup-${System.currentTimeMillis() / 1000}.json")
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("导出 JSON 文件", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("从文件导入恢复", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.exportBackupJson { json ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Zhi Backup JSON", json))
                                Toast.makeText(context, "已复制备份 JSON 到剪贴板", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("复制备份到剪贴板", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                            if (text.isNullOrBlank()) {
                                Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.importBackupJson(text) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("从剪贴板恢复", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. 版本与 GitHub Release 更新检查
        SectionTitle(icon = Icons.Outlined.Refresh, title = "关于与更新 (GitHub Releases)")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Zhi (知 / 吱)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(text = "当前版本: v${viewModel.currentVersionName}", fontSize = 12.sp, color = Slate400)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.checkForAppUpdate { hasUpdate, message ->
                                if (hasUpdate) {
                                    showUpdateDialog = true
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("检查中...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("检查新版本", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/qingmuhy744/zhi"))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "GitHub 开源仓库", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(text = "qingmuhy744/zhi", fontSize = 12.sp, color = Slate400)
                    }
                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 新版本更新提示弹窗
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 ${updateInfo?.releaseTag} 🎉") },
            text = {
                Column {
                    Text(text = "更新说明：", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateInfo?.releaseNotes ?: "修复了一些已知问题并提升了稳定性。",
                        fontSize = 13.sp,
                        color = Slate700,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        viewModel.openUpdateUrl(updateInfo?.apkUrl ?: updateInfo?.htmlUrl ?: "https://github.com/qingmuhy744/zhi/releases")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("前往下载更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }

    // 重置 Device Key 确认对话框
    if (showRegenerateKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateKeyDialog = false },
            title = { Text("重置并生成新 Device Key？") },
            text = { Text("重置后，原有的推送 URL 将立刻失效，您需要在调用的外部脚本或服务中更换为新生成的 Device Key。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.regenerateDeviceKey()
                        showRegenerateKeyDialog = false
                        Toast.makeText(context, "已生成新的 Device Key 并尝试同步", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("重置", color = RedDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateKeyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 12.sp, color = Slate400)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BluePrimary)
        )
    }
}

@Composable
private fun ThemeModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
