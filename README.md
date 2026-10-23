# 吱 Zhi (Squeak) 🐭⚡

> Android 版类 Bark 极简推送工具。  
> iOS 大佬有犬吠（Bark），下水道鼠鼠只能「吱」一声。  
> 不敢高声语，恐惊天上人；收到请吱声，荒原见自由。

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Package](https://img.shields.io/badge/Package-com.zhi.app-blue.svg)](android/app)
[![Server](https://img.shields.io/badge/Server-Go%201.23-00ADD8.svg?logo=go)](server/)
[![Push Engine](https://img.shields.io/badge/Engine-FCM%20HTTP%20v1-FFCA28.svg?logo=firebase)](docs/FIREBASE_SETUP.md)
[![Vibe](https://img.shields.io/badge/Vibe-Rock%20%26%20Wild-ff69b4.svg)](https://github.com/qingmuhy744/wildstring)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 名字的来处

iOS 那边的大佬们用的是 **Bark** —— 昂首挺胸、声如洪钟的犬吠。

而在安卓这片狂野戈壁、在机房轰鸣的地下机房、在深夜不见天日的工位夹缝里，我们大部分开发者更像是穿梭在信息管道间的**互联网老鼠人（鼠鼠）**。

**“不敢高声语，恐惊天上人。”**

既然不配像看门恶犬那样动辄高门大嗓地汪汪叫，那在服务器崩盘、构建结束、告警触发、或者某个深夜脚本跑通的瞬间，在黑暗的管道里轻轻地、小心翼翼地**「吱」**一声，就够了。

> **“收到请吱声。”**  
> 这是打工人最朴素的接头暗号，也是属于地下管道鼠鼠们的极客浪漫。

与作者的另一款项目 [野弦 WildString](https://github.com/qingmuhy744/wildstring) 一样，「吱」同样带着一点荒原摇滚的粗粝与反骨：**野弦在地上嘶吼放歌，吱在地下负责通风报信。**

---

## ✨ 核心特性

- 🐭 **双重生存哲学（方案 B 与 方案 C）**：
  - **方案 B：野生明文透传（默认）** —— `curl "https://.../key/开饭了"`，一行命令直接起飞，粗暴直接，拒绝任何繁文缛节。适合自己的私有自建服务器或日常无密告警。
  - **方案 C：端到端加密 E2EE（防视奸）** —— 鼠鼠也有秘密。通过客户端与发送端对称密钥（AES），公网中转网关只负责运送乱码，连服务器都不知道你到底发了什么。零知识转发，谁也别想窥探下水道的私语。
- ⚡ **无后台常驻的真正 FCM**：基于 Google Firebase Cloud Messaging HTTP v1 协议，利用系统级推送通道。无需后台死皮赖脸保活，不偷跑电量，告警却能在毫秒级划破寂静。
- 🌐 **双服务器模式，来去自由**：
  - **默认公共服务**：内置预配作者维护的 FCM 路由网关，开箱即用，免去折腾。
  - **自建私有服务器**：App 设置里随时切成自己的域名（例如 `https://push.yourdomain.com`），数据全在自己手里，彻底告别服务关停跑路的焦虑。
- 📦 **轻到不能再轻的服务端**：纯 Go 编写 + 纯 Go 内嵌式 SQLite，编译出来的单一可执行文件只有 **18MB**，容器内存占用 **< 20MB**。哪怕是 512MB 内存的远古小鸡也能轻松拉起。
- 🎨 **现代 Android 原生体验**：
  - 包名：`com.zhi.app`
  - 技术栈：Kotlin + Jetpack Compose + Material 3 + Room 数据库 + Coroutines / Flow。
  - **分组筛选与搜索**：支持运维、日常、安全等自定义 Group 过滤与全文模糊检索。
  - **时效分级 (level)**：`active`（常规消息）、`timeSensitive`（紧急强提醒/悬浮横幅）、`passive`（静默打扰）。
  - **自动与便捷复制 (copy)**：收到含 `copy` 字段的通知（如验证码、Git Commit、命令），可自动落入剪贴板，通知栏亦附带独立复制按钮。
  - **一键跳转 (url)**：通知附带链接时，点击直接拉起浏览器或第三方 App Scheme。

---

## 📂 项目结构

```
zhi/
├── android/                         # Android 客户端工程 (com.zhi.app)
│   ├── app/
│   │   ├── src/main/java/com/zhi/app/
│   │   │   ├── ZhiApp.kt            # Application 类 (初始化通知渠道)
│   │   │   ├── MainActivity.kt      # 权限请求与 Compose 宿主
│   │   │   ├── data/                # Room 本地持久化 & DataStore 首选项
│   │   │   ├── service/             # FCM 接收服务 & 通知栏 Action 广播
│   │   │   └── ui/                  # 消息中心、API 指南、设置页面 (Material 3)
│   │   ├── google-services.json     # Firebase 客户端凭证模板
│   │   └── build.gradle.kts
│   └── gradlew                      # Gradle 8.13 包装器
├── server/                          # 轻量 Go 服务端
│   ├── internal/api/                # Gin 路由 (Bark 兼容端点)
│   ├── internal/push/               # FCM HTTP v1 OAuth2 推送器
│   ├── internal/storage/            # 纯 Go SQLite 存储设备映射
│   ├── Dockerfile                   # Alpine 极小镜像构建
│   ├── docker-compose.yml           # 一键 Docker 部署配置
│   ├── config.example.yaml          # 配置文件模板
│   └── zhi-server                   # 已编译完成的单文件二进制
├── docs/
│   ├── API.md                       # Bark 兼容接口说明与多语言调用示例
│   ├── DEPLOY.md                    # Docker / 二进制 / Nginx HTTPS 自建部署指南
│   └── FIREBASE_SETUP.md            # Firebase 项目创建与双端凭证配置教程
└── README.md                        # 你现在看到的这份文档
```

---

## 📡 API 调用速查 (Bark 全兼容)

无需学习任何新协议，直接照搬你的 Bark 脚本：

### 1. 极简 GET 调用
```bash
# 1. 最简消息
curl "http://localhost:8080/YOUR_DEVICE_KEY/下班了，快跑！"

# 2. 带标题
curl "http://localhost:8080/YOUR_DEVICE_KEY/生产告警/主库CPU突破95%"

# 3. 携带分组、自动复制与时效级别
curl "http://localhost:8080/YOUR_DEVICE_KEY/验证码/您的验证码是8899?group=安全&copy=8899&level=timeSensitive"
```

### 2. 标准 POST JSON 调用
```bash
curl -X POST "http://localhost:8080/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "YOUR_DEVICE_KEY",
    "title": "任务完成",
    "body": "跨国大文件搬迁完成，共耗时 42 秒",
    "group": "自动化",
    "copy": "scp done",
    "url": "https://example.com/log",
    "level": "timeSensitive"
  }'
```

完整参数列表与 Python/Node.js/Bash 封装详见 [docs/API.md](docs/API.md)。

---

## 🚀 极速上手

### 1. 启动服务端

#### 本地二进制直接跑
```bash
cd server
./zhi-server -config config.example.yaml
```
服务将在 `http://localhost:8080` 启动。未配置 Google 密钥时自动处于 **MOCK 测试模式**，不影响接口调试。

#### Docker Compose 部署
```bash
cd server
docker compose up -d
```
详细自建与 Nginx/Caddy 反代配置参见 [docs/DEPLOY.md](docs/DEPLOY.md)。

---

### 2. Android 客户端打包与安装

在终端执行：
```bash
cd android
./gradlew assembleDebug
```
产物位置：
```
android/app/build/outputs/apk/debug/app-debug.apk
```
用 USB 数据线连上手机直接安装：
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠 自建私有化与独立 FCM

如果你不想让任何第三方网关经手你的告警：
1. 按照 [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) 在 Firebase 控制台免费创建自己的项目，拿到 `google-services.json`（给客户端）与 `firebase-service-account.json`（给服务端）。
2. 在自己的 VPS 上启动 `zhi-server`。
3. 打开手机 **Zhi App -> 设置 -> 推送服务器**，选 **「自建私有服务器」**，填入你的公网域名，点击测试并同步。
4. 此时你的手机和服务器之间形成完全闭环的私有专属通道。

---

## 🖤 致敬与共鸣

- 鸣谢 [Bark](https://github.com/Finb/Bark) 开拓的优雅推送协议规范。
- 联动 [野弦 WildString](https://github.com/qingmuhy744/wildstring) —— 荒原自留地，音乐不设限，自由有回响。

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 授权，自由使用，随意魔改。
