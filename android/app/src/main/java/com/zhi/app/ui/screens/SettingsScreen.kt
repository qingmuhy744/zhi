package com.zhi.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.runtime.setValue
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

    val deviceKey by viewModel.deviceKey.collectAsState()
    val serverMode by viewModel.serverMode.collectAsState()
    val customServerUrl by viewModel.customServerUrl.collectAsState()
    val effectiveServerUrl by viewModel.effectiveServerUrl.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val autoCopy by viewModel.autoCopy.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var customUrlInput by remember(customServerUrl) { mutableStateOf(customServerUrl) }
    var showRegenerateKeyDialog by remember { mutableStateOf(false) }
    var isTestingPing by remember { mutableStateOf(false) }

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
                        Text(text = "默认官方服务 (默认 FCM)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "开箱即用，无需搭建服务器", fontSize = 12.sp, color = Slate400)
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

                Text(text = "FCM Registration Token:", fontSize = 12.sp, color = Slate400)
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
                        color = Slate700,
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
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "复制 Token", tint = Slate500, modifier = Modifier.size(14.dp))
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

        // 4. 通知偏好设置
        SectionTitle(icon = Icons.Outlined.Notifications, title = "通知首选项")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        Text(text = "跳转至系统设置调整免打扰及悬浮横幅", fontSize = 12.sp, color = Slate400)
                    }
                    Icon(imageVector = Icons.Outlined.OpenInNew, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. 关于
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Zhi (知 / 吱) v1.0.0", color = Slate500, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Android 版 Bark 兼容轻量推送服务", color = Slate400, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
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
