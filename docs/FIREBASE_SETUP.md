# Firebase 项目配置指南 (FCM Setup Guide)

Zhi 基于 Google Firebase Cloud Messaging (FCM HTTP v1) 协议实现无后台保活、极低功耗的高可靠实时推送。

本文指导您如何创建 Firebase 项目并获取客户端和服务端所需的凭证。

---

## 步骤 1：创建 Firebase 项目

1. 打开 [Firebase 控制台](https://console.firebase.google.com/) 并登录您的 Google 账号。
2. 点击 **「添加项目」** (Add project)。
3. 输入项目名称（如 `zhi-push`），点击继续完成项目创建。

---

## 步骤 2：获取客户端配置 `google-services.json`

1. 在 Firebase 项目概览页面，点击 **Android 图标** 添加 Android 应用。
2. 填写应用信息：
   - **Android 软件包名称**：必须填写 `com.zhi.app`。
   - **应用昵称**：`Zhi`（可选）。
3. 点击 **「注册应用」**。
4. 在第二步 **「下载配置文件」** 中，下载 `google-services.json`。
5. 将下载的 `google-services.json` 覆盖到工程目录：
   ```
   android/app/google-services.json
   ```
6. 执行编译打包：
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

---

## 步骤 3：获取服务端凭证 `firebase-service-account.json`

用于 Zhi 服务端调用 FCM HTTP v1 接口下发通知：

1. 在 Firebase 控制台，点击左上角齿轮图标 ⚙️ -> **「项目设置」** (Project settings)。
2. 切换到 **「服务账号」** (Service accounts) 选项卡。
3. 确保开发语言选中 **Node.js** 或 **Java** / **Go**，点击底部的 **「生成新的私钥」** (Generate new private key)。
4. 在弹出的对话框中确认生成，浏览器将下载一个 `.json` 文件。
5. 将该文件重命名为 `firebase-service-account.json` 并放置在服务端根目录：
   ```
   server/firebase-service-account.json
   ```
   或者通过环境变量指定文件路径：
   ```bash
   export FCM_SERVICE_ACCOUNT_FILE=/path/to/firebase-service-account.json
   ```

---

## 步骤 4：验证 FCM 推送链路

1. 启动 Zhi 服务端：
   ```bash
   cd server
   ./zhi-server -config config.example.yaml
   ```
   终端会输出：
   ```
   [INFO] FCM Pusher initialized successfully for project: your-project-id
   [INFO] Zhi Server is ready and listening on :8080
   ```
2. 安装并打开 Android Zhi 客户端，首次启动将自动获取 FCM Token 并向服务器发送注册请求。
3. 在客户端 **「接口」** 页面点击 **「向服务端发推送」**，或在电脑执行：
   ```bash
   curl "http://localhost:8080/YOUR_DEVICE_KEY/测试标题/测试正文"
   ```
4. 手机将立即弹出系统通知！
