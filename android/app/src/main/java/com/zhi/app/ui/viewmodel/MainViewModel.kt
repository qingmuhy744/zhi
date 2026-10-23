package com.zhi.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhi.app.data.local.ZhiDatabase
import com.zhi.app.data.model.PushMessage
import com.zhi.app.data.preferences.ServerMode
import com.zhi.app.data.preferences.UserPreferences
import com.zhi.app.data.remote.ServerApiClient
import com.zhi.app.update.AppUpdateManager
import com.zhi.app.update.UpdateInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZhiDatabase.getInstance(application)
    private val dao = db.messageDao()
    private val prefs = UserPreferences(application)
    private val apiClient = ServerApiClient()
    private val updateManager = AppUpdateManager(application)

    val currentVersionName = "0.1.0"

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    // Preferences Flows
    val deviceKey: StateFlow<String> = prefs.deviceKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val serverMode: StateFlow<ServerMode> = prefs.serverModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServerMode.DEFAULT)

    val customServerUrl: StateFlow<String> = prefs.customServerUrlFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val effectiveServerUrl: StateFlow<String> = prefs.effectiveServerUrlFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences.DEFAULT_SERVER_URL)

    val fcmToken: StateFlow<String> = prefs.fcmTokenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isRegistered: StateFlow<Boolean> = prefs.isRegisteredFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoCopy: StateFlow<Boolean> = prefs.autoCopyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val soundEnabled: StateFlow<Boolean> = prefs.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val vibrationEnabled: StateFlow<Boolean> = prefs.vibrationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val e2eeEnabled: StateFlow<Boolean> = prefs.e2eeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val aesKey: StateFlow<String> = prefs.aesKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // UI state
    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMessage = MutableStateFlow<PushMessage?>(null)
    val selectedMessage: StateFlow<PushMessage?> = _selectedMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val groups: StateFlow<List<String>> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered messages list
    val messages: StateFlow<List<PushMessage>> = combine(
        _selectedGroup,
        _searchQuery
    ) { group, query ->
        Pair(group, query)
    }.flatMapLatest { (group, query) ->
        when {
            query.isNotEmpty() -> dao.searchMessages(query)
            group != null -> dao.getMessagesByGroup(group)
            else -> dao.getAllMessages()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Ensure device key is generated on launch
        viewModelScope.launch {
            prefs.getOrCreateDeviceKey()
        }
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openMessageDetail(message: PushMessage) {
        _selectedMessage.value = message
    }

    fun closeMessageDetail() {
        _selectedMessage.value = null
    }

    fun deleteMessage(message: PushMessage) {
        viewModelScope.launch {
            dao.deleteMessageById(message.id)
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            dao.clearAllMessages()
        }
    }

    fun setServerMode(mode: ServerMode) {
        viewModelScope.launch {
            prefs.setServerMode(mode)
            syncToServer()
        }
    }

    fun setCustomServerUrl(url: String) {
        viewModelScope.launch {
            prefs.setCustomServerUrl(url)
            syncToServer()
        }
    }

    fun regenerateDeviceKey() {
        viewModelScope.launch {
            prefs.regenerateDeviceKey()
            syncToServer()
        }
    }

    fun setAutoCopy(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoCopy(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setSoundEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setVibrationEnabled(enabled)
        }
    }

    fun setE2EEEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setE2EEEnabled(enabled)
        }
    }

    fun setAesKey(key: String) {
        viewModelScope.launch {
            prefs.setAesKey(key)
        }
    }

    fun generateNewAesKey() {
        viewModelScope.launch {
            val newKey = com.zhi.app.util.CryptoUtils.generateRandomKey(32)
            prefs.setAesKey(newKey)
            prefs.setE2EEEnabled(true)
        }
    }

    fun checkForAppUpdate(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = updateManager.checkForUpdate(currentVersionName)
            _isCheckingUpdate.value = false
            if (result.isSuccess) {
                val info = result.getOrNull()
                _updateInfo.value = info
                if (info?.hasUpdate == true) {
                    onResult?.invoke(true, "发现新版本: ${info.releaseTag}")
                } else {
                    onResult?.invoke(false, "已是最新版本 (v$currentVersionName)")
                }
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "检查更新失败")
            }
        }
    }

    fun openUpdateUrl(url: String) {
        updateManager.openDownloadPage(url)
    }

    fun syncToServer(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val key = deviceKey.value
            val token = fcmToken.value
            val serverUrl = effectiveServerUrl.value

            if (key.isEmpty()) {
                _isSyncing.value = false
                onComplete?.invoke(false, "设备 Key 为空")
                return@launch
            }

            if (token.isEmpty()) {
                _isSyncing.value = false
                onComplete?.invoke(false, "FCM Token 尚未获取，请检查 Google Play 服务网络连接")
                return@launch
            }

            val result = apiClient.registerDevice(serverUrl, key, token)
            _isSyncing.value = false

            if (result.isSuccess) {
                prefs.setRegistered(true)
                onComplete?.invoke(true, "同步成功")
            } else {
                prefs.setRegistered(false)
                onComplete?.invoke(false, result.exceptionOrNull()?.message ?: "同步失败")
            }
        }
    }

    fun testServerPing(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = apiClient.ping(url)
            if (result.isSuccess) {
                onResult(true, "连接成功 (200 OK)")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "连接失败")
            }
        }
    }

    fun sendLocalTestPush(title: String, body: String, group: String, copy: String, url: String) {
        viewModelScope.launch {
            val message = PushMessage(
                title = title.ifEmpty { "本地测试推送" },
                body = body.ifEmpty { "这是一条本地模拟推送消息，用来测试存储与列表显示。" },
                group = group,
                copy = copy,
                url = url,
                level = "active",
                receivedAt = System.currentTimeMillis()
            )
            dao.insertMessage(message)
        }
    }

    fun sendServerTestPush(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val serverUrl = effectiveServerUrl.value
            val key = deviceKey.value
            val result = apiClient.sendTestPush(
                serverUrl = serverUrl,
                deviceKey = key,
                title = "Zhi 服务端测试",
                body = "恭喜！您已成功通过 Zhi 服务端和 FCM 发送了推送通知！",
                group = "系统测试"
            )
            if (result.isSuccess) {
                onResult(true, "请求已发送到服务器")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "请求失败")
            }
        }
    }
}
