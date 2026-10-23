# 吱 Zhi (Squeak) 🐭⚡

> Android 版类 Bark 极简推送工具。  
> iOS 大佬有犬吠（Bark），下水道鼠鼠只能「吱」一声。  
> 不敢高声语，恐惊天上人；收到请吱声，荒原见自由。

[![Release](https://img.shields.io/github/v/release/qingmuhy744/zhi?logo=github&color=5865F2)](https://github.com/qingmuhy744/zhi/releases/latest)
[![CI](https://img.shields.io/github/actions/workflow/status/qingmuhy744/zhi/ci.yml?branch=main&label=CI&logo=githubactions&logoColor=white)](https://github.com/qingmuhy744/zhi/actions)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Package](https://img.shields.io/badge/Package-com.zhi.app-blue.svg)](android/app)
[![API](https://img.shields.io/badge/API-Bark%20Compatible-007AFF.svg?logo=apple&logoColor=white)](docs/API.md)
[![Security](https://img.shields.io/badge/Security-E2EE%20(AES--GCM)-e05d44.svg?logo=lock&logoColor=white)](docs/API.md#2-方案-c端到端加密-e2ee-防视奸)
[![Daemon](https://img.shields.io/badge/Daemon-Zero%20Background-2ea44f.svg?logo=googleplay&logoColor=white)](docs/FIREBASE_SETUP.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Design-Material%203-7C4DFF.svg?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Server](https://img.shields.io/badge/Server-Go%201.23%2B-00ADD8.svg?logo=go&logoColor=white)](server/)
[![Push Engine](https://img.shields.io/badge/Engine-FCM%20HTTP%20v1-FFCA28.svg?logo=firebase&logoColor=black)](docs/FIREBASE_SETUP.md)
[![Memory](https://img.shields.io/badge/Memory-%3C20MB%20RAM-informational.svg?logo=docker&logoColor=white)](server/)
[![Vibe](https://img.shields.io/badge/Vibe-Rock%20%26%20Wild-ff69b4.svg)](https://github.com/qingmuhy744/wildstring)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🍎 苹果人体面，🤖 安卓力工梭哈

关于名字的来处，这里有一则来自赛博工地的真实寓言：

- **苹果人（iOS）**：优雅、得体、闭环花园。喝着冰美式，每年心平气和地给库克交 99 刀保护费，享受着纯净高贵的 APNs 统一推送通道。他们的通知工具叫 **Bark** —— 昂首挺胸、声如洪钟、宛若纯种杜宾的戒备犬吠，浑身上下都透着一股不差钱的中产阶级体面。
- **安卓人（Android）**：野蛮生长、灰头土脸、与天斗与地斗。每天在各大手机厂商的杀后台策略、魔改 Doze、自启白名单、神仙打架的权限泥潭里摸爬滚打。我们不是什么数字游民，大部分时候更像是穿梭在信息管道和机房轰鸣声里的**赛博力工 / 地下城老鼠人（鼠鼠）**。

> 🛡️ **赛博叠甲声明**：  
> 本文所有比喻纯属安卓底层力工的自我调侃与打工人自嘲，对极为优秀的 [Bark](https://github.com/Finb/Bark) 及其作者 Finb 大佬致以最崇高的开源敬意（Salute 🫡）！绝对没有任何冒犯之意——毕竟是 Bark 开发者自己先起名叫“犬吠”的哈（笑），我们不过是作为安卓下水道的鼠鼠顺着动物声谱接了一记“吱声”🐭。

**“不敢高声语，恐惊天上人。”**

既然当了安卓力工，就不配像看门名犬那样动辄高门大嗓地汪汪叫；但在服务器崩盘、CI 构件爆红、监控指标异常、或者深夜某个搬砖脚本跑通的瞬间，在黑暗的信息管道里小心翼翼地「**吱**」一声，就足够心领神会。

> **“收到请吱声。”**  
> 这是打工人最朴素的接头暗号，也是属于底层搬砖力工的极客浪漫。

**关于开发者状态**：  
白天在赛博工地上努力搬砖打灰、拧螺丝换窝窝头；晚上脱下安全帽，把键盘敲出火星子，**准备力工梭哈 ing**！如果哪天这玩意成了，鼠鼠也能从下水道探出头，向着荒原「吱吱」大笑两声。

与作者的另一款项目 [野弦 WildString](https://github.com/qingmuhy744/wildstring) 一样，「吱」同样带着一股荒原摇滚的粗粝与反骨：**野弦在地上嘶吼放歌，吱在地下负责通风报信。**

---

## ✨ 核心特性

- 🐭 **双重生存哲学（方案 B 与 方案 C）**：
  - **方案 B：野生明文透传（默认）** —— `curl "https://.../key/开饭了"`，一行命令直接起飞，粗暴直接，拒绝任何繁文缛节。适合自己的私有自建服务器或日常无密告警。
  - **方案 C：端到端加密 E2EE（防视奸）** —— 鼠鼠也有秘密。通过客户端与发送端对称密钥（AES），公网中转网关只负责运送乱码，连服务器都不知道你到底发了什么。零知识转发，谁也别想窥探下水道的私语。
- ⚡ **无后台常驻的真正 FCM**：基于 Google Firebase Cloud Messaging HTTP v1 协议，利用系统级推送通道。无需后台死皮赖脸保活，不偷跑电量，告警却能在毫秒级划破寂静。
- 🌐 **双服务器模式，来去自由**：
  - **默认公共服务 (`https://push.qingmuhy.com`)**：内置预配官方公共网关，开箱即用，免去服务器搭建与域名备案的折腾；配合方案 C（E2EE 端到端加密），公网网关零知识盲转发密文，安全与便利兼得。
  - **自建私有服务器**：App 设置里随时切成自己的私有域名（如 `https://push.yourdomain.com` 或内网 IP），数据与通道全由自己掌控，彻底告别商业服务关停跑路的焦虑。
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

无需学习任何新协议，直接照搬你的 Bark 脚本（以公共网关 `https://push.qingmuhy.com` 为例，自建服务请替换为自建地址）：

### 1. 方案 B：极简明文透传 (GET / POST)
```bash
# 1. 最简消息
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/下班了，快跑！"

# 2. 带标题
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/生产告警/主库CPU突破95%"

# 3. 携带分组、自动复制与时效级别
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/验证码/您的验证码是8899?group=安全&copy=8899&level=timeSensitive"

# 4. 标准 POST JSON
curl -X POST "https://push.qingmuhy.com/push" \
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

### 2. 方案 C：端到端加密 E2EE (零知识防视奸)
在手机 App 设置开启「端到端加密」并设置好 AES Key 后，公网网关盲目转发密文，手机本地秒级解密：
```bash
# 1. 使用配套 Python 工具加密推送
python3 scripts/encrypt_push.py \
  --server "https://push.qingmuhy.com" \
  --key "YOUR_DEVICE_KEY" \
  --aes-key "YOUR_AES_KEY" \
  --title "机密告警" \
  --body "财务数据库异动"

# 2. 使用原生 Node.js 工具 (零外部依赖)
node scripts/encrypt_push.js "https://push.qingmuhy.com" "YOUR_DEVICE_KEY" "YOUR_AES_KEY" "机密告警" "财务数据库异动"
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
3. 打开手机 **Zhi App -> 设置 -> 推送服务器**，选「**自建私有服务器**」，填入你的公网域名，点击测试并同步。
4. 此时你的手机和服务器之间形成完全闭环的私有专属通道。

---

## 🖤 致敬与共鸣

- 鸣谢 [Bark](https://github.com/Finb/Bark) 开拓的优雅推送协议规范，对 Finb 大佬致以最崇高的开源敬意（Salute 🫡！声明无任何冒犯，纯属动物声谱的趣味共鸣）。
- 联动 [野弦 WildString](https://github.com/qingmuhy744/wildstring) —— 荒原自留地，音乐不设限，自由有回响。

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 授权，自由使用，随意魔改。
