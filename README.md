# Zhi (知 / 吱) 📱🔔

> Android 版 Bark 兼容轻量推送服务。通过极简 HTTP 请求即可向 Android 手机发送实时推送，默认预设官方 FCM，同时全面支持用户自建私有服务器。

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Package](https://img.shields.io/badge/Package-com.zhi.app-blue.svg)](android/app)
[![Server](https://img.shields.io/badge/Server-Go%201.23-00ADD8.svg?logo=go)](server/)
[![Push Engine](https://img.shields.io/badge/Engine-FCM%20HTTP%20v1-FFCA28.svg?logo=firebase)](docs/FIREBASE_SETUP.md)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## ✨ 核心特性

- 🚀 **全面兼容 iOS Bark 协议**：支持 Bark 风格的短链接路由（`/:key/:body`、`/:key/:title/:body`、`/:key/:category/:title/:body`）及标准 `POST /push` JSON 接口。
- ⚡ **Google FCM HTTP v1**：基于现代 Firebase 官方最新推送协议，无后台守护进程常驻，功耗极低，支持高优先级实时穿透。
- 🌐 **双模式灵活切换**：
  - **默认模式**：客户端预配置默认官方 FCM 路由服务，开箱即用。
  - **自建模式 (Self-Hosted)**：用户可在 App 设置中一键切换为自己的私有服务器域名，数据完全由自己掌控。
- 📦 **超轻量服务端**：Go 语言编写，纯 Go 内嵌式 SQLite，静态编译为仅 18MB 单文件，支持 Docker 与 Docker Compose 极速部署。
- 🎨 **现代 Android 原生体验**：
  - 包名：`com.zhi.app`
  - 技术栈：Kotlin + Jetpack Compose + Material 3 + Room 本地持久化 + Coroutines / Flow。
  - **消息中心**：历史记录检索、分组筛选折叠、富文本卡片、一键复制、一键跳转 URL、清空管理。
  - **时效级别 (level)**：支持 `active`（普通）、`timeSensitive`（紧急强提醒/悬浮横幅）、`passive`（静默通知）。
  - **自动与便捷复制 (copy)**：收到含 `copy` 字段的通知时支持后台自动写入剪贴板，通知栏附带快捷「复制」按钮。
  - **在线调试**：App 内置接口调用示例与一键测试按钮，开箱即测。

---

## 📂 项目结构

```
zhi/
├── android/                         # Android 客户端工程 (com.zhi.app)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/zhi/app/
│   │   │   │   ├── ZhiApp.kt                # Application 类，初始化通知渠道
│   │   │   │   ├── MainActivity.kt          # 权限申请与 Compose 主入口
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/               # Room 数据库 (PushMessage / Dao)
│   │   │   │   │   ├── model/               # 数据模型
│   │   │   │   │   ├── preferences/         # DataStore 首选项 (DeviceKey / Server URL)
│   │   │   │   │   └── remote/              # OkHttp 客户端 (注册与健康检查)
│   │   │   │   ├── service/                 # FCM 接收服务与通知栏动作广播
│   │   │   │   └── ui/                      # Jetpack Compose UI (消息/接口/设置)
│   │   │   ├── res/                         # 图标、配色与字符串资源
│   │   │   └── AndroidManifest.xml
│   │   ├── google-services.json             # Firebase 客户端配置文件
│   │   └── build.gradle.kts
│   ├── gradle/
│   ├── gradlew                              # Gradle 编译脚本
│   └── settings.gradle.kts
├── server/                          # Zhi 推送服务端 (Go)
│   ├── internal/
│   │   ├── api/                             # Gin HTTP 路由 (Bark 兼容端点)
│   │   ├── config/                          # YAML / 环境变量配置加载
│   │   ├── model/                           # 消息与设备数据结构
│   │   ├── push/                            # FCM HTTP v1 消息构造与 OAuth2 发送器
│   │   └── storage/                         # 纯 Go SQLite 存储
│   ├── Dockerfile                           # 多阶段超小 Docker 镜像
│   ├── docker-compose.yml                   # 一键编排文件
│   ├── config.example.yaml                  # 配置文件模板
│   ├── firebase-service-account.example.json# 服务端 Service Account 样例
│   └── main.go                              # 服务端启动入口
└── docs/                            # 详细文档
    ├── API.md                               # 完整 API 与 cURL/Python/Node 调用示例
    ├── DEPLOY.md                            # Docker / 二进制 / Nginx 自建服务器部署指南
    └── FIREBASE_SETUP.md                    # Firebase 创建与双端凭证配置指南
```

---

## 🚀 快速上手

### 1. 服务端运行

#### 本地直接运行
```bash
cd server
./zhi-server -config config.example.yaml
```
服务将在 `http://localhost:8080` 启动。若未放置 Firebase 密钥，将自动运行于 **MOCK 测试模式**，不影响接口调试。

#### 生产 Docker Compose 部署
```bash
cd server
docker compose up -d
```
配置与自建指南详见 [docs/DEPLOY.md](docs/DEPLOY.md)。

---

### 2. Android 客户端打包与安装

在终端执行编译：
```bash
cd android
./gradlew assembleDebug
```
编译产物位于：
```
android/app/build/outputs/apk/debug/app-debug.apk
```
可通过 ADB 直接安装到手机：
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📡 API 调用速查

### GET 请求示例
```bash
# 1. 基础调用
curl "http://localhost:8080/YOUR_DEVICE_KEY/您有一条新推送"

# 2. 带标题调用
curl "http://localhost:8080/YOUR_DEVICE_KEY/服务告警/磁盘空间不足10%"

# 3. 携带分组、自动复制与时效级别
curl "http://localhost:8080/YOUR_DEVICE_KEY/验证码/您的验证码为8899?group=安全&copy=8899&level=timeSensitive"
```

### POST JSON 请求示例
```bash
curl -X POST "http://localhost:8080/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "YOUR_DEVICE_KEY",
    "title": "任务完成",
    "body": "后台数据同步已完毕",
    "group": "日常任务",
    "url": "https://example.com/status",
    "level": "active"
  }'
```

更多参数说明和 Python/Node.js/Shell 代码样例，请参见 [docs/API.md](docs/API.md)。

---

## 🛠 自建服务器与自定义 FCM

当您想完全私有化部署自己的推送服务器时：
1. 参照 [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) 在 Firebase 控制台创建属于您自己的项目，并下载 `google-services.json`（放置于 `android/app/`）与 `firebase-service-account.json`（放置于 `server/`）。
2. 在服务器上通过 Docker 或二进制启动 `zhi-server`。
3. 打开手机 **Zhi App -> 设置 -> 推送服务器**，选中 **「自建私有服务器」**，填入您的公网域名（例如 `https://push.yourdomain.com`），点击 **「测试连接」** 并保存同步。
4. 客户端将自动把 FCM Token 同步给您的私有服务器，所有推送链路均为您私有独享！

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。
