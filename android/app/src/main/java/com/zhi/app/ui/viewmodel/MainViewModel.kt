package com.zhi.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhi.app.data.local.ZhiDatabase
import com.zhi.app.data.model.PushMessage
import com.zhi.app.data.preferences.ServerMode
import com.zhi.app.data.preferences.ThemeMode
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

    val currentVersionName = "0.1.1"

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    // Preferences Flows
    val deviceKey: StateFlow<String> = prefs.deviceKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val serverMode: StateFlow<ServerMode> = prefs.serverModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ServerMode.DEFAULT)

    val themeMode: StateFlow<ThemeMode> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

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

    val subscribedTopics: StateFlow<Set<String>> = prefs.subscribedTopicsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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

    fun clearMessagesByGroup(group: String) {
        viewModelScope.launch {
            dao.clearMessagesByGroup(group)
        }
    }

    fun retainRecentMessages(keepCount: Int) {
        viewModelScope.launch {
            dao.retainRecentMessages(keepCount)
        }
    }

    fun archiveFromSystemNotificationIntent(
        title: String,
        body: String,
        group: String,
        copy: String,
        url: String,
        image: String,
        level: String,
        collapseKey: String,
        ciphertext: String?,
        iv: String?
    ) {
        viewModelScope.launch {
            var finalTitle = title
            var finalBody = body
            var finalGroup = group
            var finalCopy = copy
            var finalUrl = url
            var finalImage = image
            var finalLevel = level
            if (!ciphertext.isNullOrEmpty()) {
                val key = aesKey.value
                if (key.isNotEmpty()) {
                    try {
                        val decrypted = com.zhi.app.util.CryptoUtils.decrypt(ciphertext, key, iv)
                        try {
                            val json = org.json.JSONObject(decrypted)
                            finalTitle = json.optString("title", finalTitle)
                            finalBody = json.optString("body", finalBody)
                            finalGroup = json.optString("group", finalGroup)
                            finalCopy = json.optString("copy", finalCopy)
                            finalUrl = json.optString("url", finalUrl)
                            finalImage = json.optString("image", finalImage)
                            finalLevel = json.optString("level", finalLevel)
                        } catch (_: Exception) {
                            finalBody = decrypted
                        }
                    } catch (_: Exception) {
                        finalTitle = "端到端消息解密失败"
                        finalBody = "密钥不匹配或数据损坏。"
                    }
                }
            }
            val msg = PushMessage(
                title = finalTitle,
                body = finalBody,
                group = finalGroup,
                copy = finalCopy,
                url = finalUrl,
                image = finalImage,
                level = finalLevel,
                collapseKey = collapseKey,
                isArchive = true,
                receivedAt = System.currentTimeMillis()
            )
            dao.insertMessage(msg)
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val allMsgs = dao.getAllMessagesList()
            val root = org.json.JSONObject()
            root.put("version", currentVersionName)
            root.put("exportedAt", System.currentTimeMillis())

            val settingsObj = org.json.JSONObject()
            settingsObj.put("deviceKey", deviceKey.value)
            settingsObj.put("serverMode", serverMode.value.name)
            settingsObj.put("customServerUrl", customServerUrl.value)
            settingsObj.put("themeMode", themeMode.value.name)
            settingsObj.put("autoCopy", autoCopy.value)
            settingsObj.put("soundEnabled", soundEnabled.value)
            settingsObj.put("vibrationEnabled", vibrationEnabled.value)
            settingsObj.put("e2eeEnabled", e2eeEnabled.value)
            settingsObj.put("aesKey", aesKey.value)
            val topicsArr = org.json.JSONArray()
            subscribedTopics.value.forEach { topicsArr.put(it) }
            settingsObj.put("subscribedTopics", topicsArr)
            root.put("settings", settingsObj)

            val msgsArr = org.json.JSONArray()
            allMsgs.forEach { m ->
                val obj = org.json.JSONObject()
                obj.put("title", m.title)
                obj.put("body", m.body)
                obj.put("group", m.group)
                obj.put("icon", m.icon)
                obj.put("image", m.image)
                obj.put("sound", m.sound)
                obj.put("url", m.url)
                obj.put("copy", m.copy)
                obj.put("badge", m.badge)
                obj.put("level", m.level)
                obj.put("collapseKey", m.collapseKey)
                obj.put("isArchive", m.isArchive)
                obj.put("receivedAt", m.receivedAt)
                msgsArr.put(obj)
            }
            root.put("messages", msgsArr)
            onResult(root.toString(2))
        }
    }

    fun importBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = org.json.JSONObject(jsonString)
                val settingsObj = root.optJSONObject("settings")
                if (settingsObj != null) {
                    val dk = settingsObj.optString("deviceKey")
                    if (dk.isNotEmpty()) prefs.setDeviceKey(dk)
                    val sm = settingsObj.optString("serverMode")
                    if (sm.isNotEmpty()) {
                        runCatching { ServerMode.valueOf(sm) }.getOrNull()?.let { prefs.setServerMode(it) }
                    }
                    val cUrl = settingsObj.optString("customServerUrl")
                    if (cUrl.isNotEmpty()) prefs.setCustomServerUrl(cUrl)
                    val tm = settingsObj.optString("themeMode")
                    if (tm.isNotEmpty()) {
                        runCatching { ThemeMode.valueOf(tm) }.getOrNull()?.let { prefs.setThemeMode(it) }
                    }
                    if (settingsObj.has("autoCopy")) prefs.setAutoCopy(settingsObj.optBoolean("autoCopy", true))
                    if (settingsObj.has("soundEnabled")) prefs.setSoundEnabled(settingsObj.optBoolean("soundEnabled", true))
                    if (settingsObj.has("vibrationEnabled")) prefs.setVibrationEnabled(settingsObj.optBoolean("vibrationEnabled", true))
                    if (settingsObj.has("e2eeEnabled")) prefs.setE2EEEnabled(settingsObj.optBoolean("e2eeEnabled", false))
                    val ak = settingsObj.optString("aesKey")
                    if (ak.isNotEmpty()) prefs.setAesKey(ak)
                    val topicsArr = settingsObj.optJSONArray("subscribedTopics")
                    if (topicsArr != null) {
                        for (i in 0 until topicsArr.length()) {
                            val t = topicsArr.optString(i)
                            if (t.length >= 8) prefs.subscribeTopic(t)
                        }
                    }
                }

                val msgsArr = root.optJSONArray("messages")
                var importedCount = 0
                if (msgsArr != null && msgsArr.length() > 0) {
                    val list = mutableListOf<PushMessage>()
                    for (i in 0 until msgsArr.length()) {
                        val o = msgsArr.optJSONObject(i) ?: continue
                        list.add(
                            PushMessage(
                                title = o.optString("title", "Zhi"),
                                body = o.optString("body", ""),
                                group = o.optString("group", ""),
                                icon = o.optString("icon", ""),
                                image = o.optString("image", ""),
                                sound = o.optString("sound", ""),
                                url = o.optString("url", ""),
                                copy = o.optString("copy", ""),
                                badge = o.optInt("badge", 0),
                                level = o.optString("level", "active"),
                                collapseKey = o.optString("collapseKey", ""),
                                isArchive = o.optBoolean("isArchive", true),
                                receivedAt = o.optLong("receivedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    dao.insertMessages(list)
                    importedCount = list.size
                }
                syncToServer()
                onResult(true, "成功恢复配置并导入 $importedCount 条消息")
            } catch (e: Exception) {
                onResult(false, "导入解析失败: ${e.message}")
            }
        }
    }

    fun setServerMode(mode: ServerMode) {
        viewModelScope.launch {
            prefs.setServerMode(mode)
            syncToServer()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
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

    fun generateTopicSecret(): String {
        return prefs.generateTopicSecret()
    }

    fun subscribeTopic(topic: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = prefs.subscribeTopic(topic)
            if (result.isSuccess) {
                onComplete?.invoke(true, "订阅群发频道成功: $topic")
            } else {
                onComplete?.invoke(false, result.exceptionOrNull()?.message ?: "订阅失败")
            }
        }
    }

    fun unsubscribeTopic(topic: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = prefs.unsubscribeTopic(topic)
            if (result.isSuccess) {
                onComplete?.invoke(true, "已退订群发频道: $topic")
            } else {
                onComplete?.invoke(false, result.exceptionOrNull()?.message ?: "退订失败")
            }
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
