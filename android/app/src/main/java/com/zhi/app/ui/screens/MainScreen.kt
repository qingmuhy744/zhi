package com.zhi.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.zhi.app.ui.components.MarkdownText
import com.zhi.app.ui.theme.BluePrimary
import com.zhi.app.ui.viewmodel.MainViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val hapticFeedback = LocalHapticFeedback.current
    val updateInfo by viewModel.updateInfo.collectAsState()
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(updateInfo?.releaseTag, updateInfo?.hasUpdate) {
        if (updateInfo?.hasUpdate == true) {
            showUpdateDialog = true
        }
    }

    val navItems = listOf(
        NavItem(
            title = "消息",
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications
        ),
        NavItem(
            title = "接口",
            selectedIcon = Icons.Filled.Code,
            unselectedIcon = Icons.Outlined.Code
        ),
        NavItem(
            title = "设置",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (selectedIndex != index) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedIndex) {
            0 -> MessagesScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            1 -> ApiGuideScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            2 -> SettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }

    if (showUpdateDialog) {
        val info = updateInfo
        if (info != null && info.hasUpdate) {
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("发现新版本 ${info.releaseTag} 🎉") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("更新说明：", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        MarkdownText(info.releaseNotes.ifBlank { "暂无更新说明。" })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUpdateDialog = false
                            viewModel.openUpdateUrl(info.apkUrl.ifBlank { info.htmlUrl })
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
    }
}
