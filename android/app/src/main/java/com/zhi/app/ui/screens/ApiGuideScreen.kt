package com.zhi.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.zhi.app.ui.theme.BlueBackground
import com.zhi.app.ui.theme.BluePrimary
import com.zhi.app.ui.theme.Slate100
import com.zhi.app.ui.theme.Slate200
import com.zhi.app.ui.theme.Slate400
import com.zhi.app.ui.theme.Slate700
import com.zhi.app.ui.theme.Slate800
import com.zhi.app.ui.theme.Slate900
import com.zhi.app.ui.viewmodel.MainViewModel

@Composable
fun ApiGuideScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceKey by viewModel.deviceKey.collectAsState()
    val serverUrl by viewModel.effectiveServerUrl.collectAsState()
    val scrollState = rememberScrollState()

    var testTitle by remember { mutableStateOf("服务器告警") }
    var testBody by remember { mutableStateOf("CPU使用率已超过90%，请尽快排查！") }
    var testGroup by remember { mutableStateOf("运维") }
    var testCopy by remember { mutableStateOf("top -b -n 1") }
    var isSendingServerTest by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = "推送调用指南",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. GET 简短调用
        val simpleGetUrl = "$serverUrl/$deviceKey/这里填写推送内容"
        ApiCodeCard(
            title = "1. 最简 GET 请求 (Bark 兼容)",
            code = simpleGetUrl,
            description = "最简单的调用方式，URL末尾即为推送正文"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 带标题与参数 GET 调用
        val advanceGetUrl = "$serverUrl/$deviceKey/服务提醒/数据库已备份完成?group=系统&copy=backup_2026.sql&level=timeSensitive"
        ApiCodeCard(
            title = "2. 带标题与高级参数 GET 请求",
            code = advanceGetUrl,
            description = "支持 group (分组)、copy (复制)、level (时效级别)、url (跳转) 等"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. POST JSON 调用
        val curlPostExample = """
curl -X POST "$serverUrl/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "$deviceKey",
    "title": "支付成功通知",
    "body": "您已成功购买专业版服务，订单号: 20261023",
    "group": "财务",
    "copy": "20261023",
    "url": "https://example.com/orders",
    "level": "timeSensitive"
  }'
        """.trimIndent()

        ApiCodeCard(
            title = "3. cURL POST JSON 请求",
            code = curlPostExample,
            description = "适用于后端系统、自动化脚本与 Webhook"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 参数说明
        Text(
            text = "参数说明 (Query 或 JSON)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ParamRow("title", "通知标题（可选，缺省显示应用名）")
                ParamRow("body", "通知正文（必填）")
                ParamRow("group / category", "通知分组，用于分组折叠与筛选")
                ParamRow("image", "大图 URL，通知栏下拉展示大图 (Big Picture)")
                ParamRow("collapse_key", "折叠 Key，更新覆盖已有通知（避免刷屏）")
                ParamRow("actions", "交互回调按钮，如 同意:https://...|拒绝:https://...")
                ParamRow("ttl", "消息存活秒数（如 ttl=300 表示 5分钟过期废弃）")
                ParamRow("topic", "群发广播频道 Secret（所有订阅设备同时响铃）")
                ParamRow("system_notify", "1: 附带系统级通知托管兜底；默认 0: 纯 Data 增强模式")
                ParamRow("copy", "指定复制的内容，支持自动复制或通知栏按钮复制")
                ParamRow("url", "点击通知时自动打开的网址或第三方 App URL Scheme")
                ParamRow("level", "active (默认) / timeSensitive (紧急时效) / passive (静默但可见)")
                ParamRow("display", "1: 显示通知（默认）；0: 后台处理并归档，但不显示通知")
                ParamRow("sound", "通知铃声名称")
                ParamRow("isArchive", "1: 存储到本地历史 (默认); 0: 不归档")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 交互测试区域
        Text(
            text = "在线推送测试",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = testTitle,
                    onValueChange = { testTitle = it },
                    label = { Text("标题 (title)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = testBody,
                    onValueChange = { testBody = it },
                    label = { Text("正文 (body)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = testGroup,
                        onValueChange = { testGroup = it },
                        label = { Text("分组 (group)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = testCopy,
                        onValueChange = { testCopy = it },
                        label = { Text("复制内容 (copy)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 本地模拟推送
                    OutlinedButton(
                        onClick = {
                            viewModel.sendLocalTestPush(testTitle, testBody, testGroup, testCopy, "")
                            Toast.makeText(context, "已生成本地测试推送，可切到「消息」查看！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("本地测试", fontSize = 13.sp, maxLines = 1)
                    }

                    // 服务端真实测试
                    Button(
                        onClick = {
                            isSendingServerTest = true
                            viewModel.sendServerTestPush { success, msg ->
                                isSendingServerTest = false
                                Toast.makeText(context, if (success) "推送已提交: $msg" else "推送失败: $msg", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSendingServerTest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSendingServerTest) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("发送推送", fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ApiCodeCard(
    title: String,
    code: String,
    description: String
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Zhi API", code))
                        copied = true
                        Toast.makeText(context, "已复制调用代码", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "复制",
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = code,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamRow(param: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = param,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = desc,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}
