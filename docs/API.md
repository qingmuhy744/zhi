# Zhi API 接口文档 (Bark 协议全兼容)

Zhi 服务端提供与 iOS Bark 完全兼容的 HTTP 推送接口，支持通过简单的 `GET` 或 `POST` 请求向安装了 Zhi App 的 Android 设备发送实时通知。

---

## 1. 快速开始 (推送接口)

### 1.1 最简 GET 请求
```http
GET /:device_key/:body
```
- **示例**:
  ```bash
  curl "http://localhost:8080/YOUR_DEVICE_KEY/这是一条测试消息"
  ```

### 1.2 带标题的 GET 请求
```http
GET /:device_key/:title/:body
```
- **示例**:
  ```bash
  curl "http://localhost:8080/YOUR_DEVICE_KEY/服务器告警/CPU使用率超过90%"
  ```

### 1.3 带分组的 GET 请求
```http
GET /:device_key/:group/:title/:body
```
- **示例**:
  ```bash
  curl "http://localhost:8080/YOUR_DEVICE_KEY/运维/数据库提醒/备份完成"
  ```

---

## 2. 标准 POST 请求 (推荐后端/脚本调用)

### 2.1 POST /push
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Request Body**:
```json
{
  "device_key": "YOUR_DEVICE_KEY",
  "title": "支付成功",
  "body": "您已成功购买专业版服务，订单号: 20261023",
  "group": "财务",
  "copy": "20261023",
  "url": "https://example.com/orders/20261023",
  "level": "timeSensitive",
  "badge": 1,
  "isArchive": 1
}
```

- **cURL 示例**:
```bash
curl -X POST "http://localhost:8080/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "YOUR_DEVICE_KEY",
    "title": "构建完成",
    "body": "Zhi App v1.0.0 构建成功",
    "group": "CI/CD",
    "copy": "git log -1",
    "level": "active"
  }'
```

### 2.2 POST /:device_key
支持直接在 URL 中指定 key，通过 JSON 或 Form-data 传递推送内容：
```bash
curl -X POST "http://localhost:8080/YOUR_DEVICE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "任务提醒",
    "body": "记得今晚 10:00 开会"
  }'
```

---

## 3. 请求参数说明

所有参数均可在 URL Query 参数（GET/POST）或 JSON Body（POST）中传递：

| 参数名 | 类型 | 说明 | 示例 |
| :--- | :--- | :--- | :--- |
| `device_key` | String | 设备唯一标识凭证（在 App 设置页查看） | `abcdef123456...` |
| `body` | String | **必填**。通知正文内容 | `服务器备份已完成` |
| `title` | String | 通知标题。缺省显示应用名称 `Zhi` | `运维通知` |
| `group` / `category` | String | 通知分组。相同分组在 App 历史中支持分类筛选与渠道归类 | `生产环境`、`生活提醒` |
| `copy` | String | 指定复制文本。若 App 开启「自动复制」，收到通知时会自动写入剪贴板；通知栏亦提供「复制内容」快捷按钮 | `668899` (验证码) |
| `url` | String | 点击通知后自动跳转的网址或第三方 App URL Scheme | `https://github.com`、`weixin://` |
| `level` | String | 通知时效级别：<br>• `active`: 常规通知 (默认)<br>• `timeSensitive`: 紧急/高时效通知，带悬浮横幅与红色标识<br>• `passive`: 静默低优先级通知 | `timeSensitive` |
| `sound` | String | 通知铃声名称 | `default` |
| `badge` | Int | 桌面角标数字 | `1` |
| `isArchive` | Int | 是否归档到 App 本地历史数据库：<br>• `1`: 归档 (默认)<br>• `0`: 仅通知，不存历史 | `1` |

---

## 4. 服务端管理与注册接口

### 4.1 设备注册 /:register
由 Zhi 客户端自动调用，将用户的 `device_key` 与 `fcm_token` 绑定：
- **Method**: `POST /register`
- **Request Body**:
```json
{
  "device_key": "your_device_key",
  "fcm_token": "fcm_token_string",
  "device_name": "Pixel 8 Pro"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "Device registered successfully",
  "data": {
    "key": "your_device_key",
    "fcm_token": "fcm_token_string",
    "device_name": "Pixel 8 Pro"
  },
  "timestamp": 1792767015
}
```

### 4.2 服务健康检查 /ping
- **Method**: `GET /ping`
- **Response**:
```json
{
  "code": 200,
  "message": "pong",
  "timestamp": 1792767009
}
```

### 4.3 服务运行信息 /info
- **Method**: `GET /info`
- **Response**:
```json
{
  "code": 200,
  "message": "Zhi Push Server is running",
  "data": {
    "version": "1.0.0",
    "name": "Zhi Push Server",
    "fcm_configured": true,
    "device_count": 5
  },
  "timestamp": 1792767009
}
```

---

## 5. 常见调用代码示例

### Python
```python
import requests

def send_push(key, title, body, group=None, copy=None, url=None):
    payload = {
        "device_key": key,
        "title": title,
        "body": body
    }
    if group: payload["group"] = group
    if copy: payload["copy"] = copy
    if url: payload["url"] = url

    r = requests.post("https://push.yourdomain.com/push", json=payload)
    return r.json()

send_push("YOUR_DEVICE_KEY", "爬虫完成", "共抓取 1200 条数据", group="数据抓取")
```

### Node.js / TypeScript
```typescript
import axios from 'axios';

async function push(deviceKey: string, title: string, body: string) {
  const res = await axios.post('https://push.yourdomain.com/push', {
    device_key: deviceKey,
    title,
    body,
    level: 'timeSensitive'
  });
  console.log(res.data);
}
```

### Bash Shell
```bash
#!/usr/bin/env bash
ZHI_KEY="YOUR_DEVICE_KEY"
ZHI_SERVER="https://push.yourdomain.com"

# 抓取 IP 并推送
MY_IP=$(curl -s ifconfig.me)
curl -s -X POST "$ZHI_SERVER/push" \
  -H "Content-Type: application/json" \
  -d "{\"device_key\": \"$ZHI_KEY\", \"title\": \"公网IP变动\", \"body\": \"当前IP: $MY_IP\", \"copy\": \"$MY_IP\"}"
```
