# Zhi API 接口文档 (Bark 协议全兼容)

Zhi 服务端提供与 iOS Bark 完全兼容的 HTTP 推送接口，支持通过简单的 `GET` 或 `POST` 请求向安装了 Zhi App 的 Android 设备发送实时通知。

---

## 1. 快速开始 (推送接口)

直接打开网关根路径返回一段纯文本，大约 300 字节，没有 HTML 和 JSON：

```bash
curl https://push.qingmuhy.com/
```

正文里有 GitHub、README、接口文档和部署文档的地址，以及 `GET /:device_key/:body` 和 `POST /push` 两行调用形式。

> **服务网关地址**：
> - **官方公共网关（开箱即用，免搭建）**：`https://push.qingmuhy.com`
> - **自建私有服务**：`http://localhost:8080`（或您反向代理的独立域名）

### 1.1 最简 GET 请求
```http
GET /:device_key/:body
```
- **示例（公共网关）**:
  ```bash
  curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/这是一条测试消息"
  ```
- **示例（自建服务）**:
  ```bash
  curl "http://localhost:8080/YOUR_DEVICE_KEY/这是一条测试消息"
  ```

### 1.2 带标题的 GET 请求
```http
GET /:device_key/:title/:body
```
- **示例**:
  ```bash
  curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/服务器告警/CPU使用率超过90%"
  ```

### 1.3 带分组的 GET 请求
```http
GET /:device_key/:group/:title/:body
```

服务端还提供 `GET /metrics` 返回 FCM 请求总数、当前并发、等待并发槽的次数、超时数、耗时汇总和按状态分类的计数。它是运维指标接口，建议只允许本机或监控网络访问。
- **示例**:
  ```bash
  curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/运维/数据库提醒/备份完成"
  ```

### 1.4 进阶参数 GET 示例
```bash
# 折叠覆盖：相同 collapse_key 更新同一条通知
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/构建进度/当前 85%?group=monitor&collapse_key=build_task"

# 通知按钮。简写固定为 GET；竖线分隔，最多 3 个
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/上线审批/是否部署 v0.1.1?actions=同意部署:https://example.com/approve|拒绝:https://example.com/reject"

# 频道广播。路径 key 以 topic_ 或 group_ 开头，且不少于 8 位
curl "https://push.qingmuhy.com/topic_your_secret_key/团队告警/数据库连接池已达上限"

# 大图只传 URL，手机自己下载。ttl 是 FCM 缓存秒数，不是 Zhi 服务器的存储时间
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/门禁抓拍/检测到异常移动?image=https://example.com/snap.jpg&ttl=300"

# 系统通知兜底。后台由系统弹窗，点开后 App 再解密并写入历史
curl "https://push.qingmuhy.com/YOUR_DEVICE_KEY/兜底告警/请点开查看?system_notify=1"
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
curl -X POST "https://push.qingmuhy.com/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "YOUR_DEVICE_KEY",
    "title": "构建完成",
    "body": "Zhi App v1.0.0 构建成功",
    "group": "CI/CD",
    "copy": "git log -1",
    "image": "https://example.com/status.png",
    "collapse_key": "sync_job",
    "actions": "[{\"title\":\"重试\",\"url\":\"https://example.com/retry\",\"method\":\"POST\"}]",
    "ttl": 3600,
    "level": "active"
  }'
```

带 `topic` 时不再查设备表，直接向该频道发送。`topic` 与 `device_key` 同时存在时，以 `topic` 为准。频道名不少于 8 位。

```bash
curl -X POST "https://push.qingmuhy.com/push" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "topic_your_secret_key",
    "title": "全员通知",
    "body": "今晚十点机房维护"
  }'
```

### 2.2 POST /:device_key
支持直接在 URL 中指定 key，通过 JSON 或 Form-data 传递推送内容：
```bash
curl -X POST "https://push.qingmuhy.com/YOUR_DEVICE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "任务提醒",
    "body": "记得今晚 10:00 开会"
  }'
```

---

## 3. 端到端加密调用 (方案 C - E2EE 零知识转发)

当借用公网 FCM 网关 (`https://push.qingmuhy.com`) 但传输高度敏感数据（如手机短信验证码、服务器内部报警、财务异动）时，可在手机 App 设置中开启「**端到端加密**」并设定 AES 密钥。发送端在本地加密为密文，公网网关看不到任何标题与正文。

### 3.1 使用 Python 发送加密推送 (`scripts/encrypt_push.py`)
```bash
python3 scripts/encrypt_push.py \
  --server "https://push.qingmuhy.com" \
  --key "YOUR_DEVICE_KEY" \
  --aes-key "YOUR_AES_KEY" \
  --title "机密告警" \
  --body "检测到生产环境主库高负载" \
  --group "安全" \
  --copy "top -n 1" \
  --level "timeSensitive"
```

### 3.2 使用 Node.js 发送加密推送 (`scripts/encrypt_push.js`)
零第三方 npm 依赖，内置 crypto 极速运行：
```bash
node scripts/encrypt_push.js "https://push.qingmuhy.com" "YOUR_DEVICE_KEY" "YOUR_AES_KEY" "告警标题" "私密告警正文"
```

### 3.3 cURL POST 密文直接调用
```bash
curl -X POST "https://push.qingmuhy.com/push" \
  -H "Content-Type: application/json" \
  -d '{
    "device_key": "YOUR_DEVICE_KEY",
    "ciphertext": "DSL/IJ3DseXmvtCvyT+kyHbEFXKO9n8O..."
  }'
```

---

## 4. 请求参数说明

所有参数均可在 URL Query 参数（GET/POST）或 JSON Body（POST）中传递：

| 参数名 | 类型 | 说明 | 示例 |
| :--- | :--- | :--- | :--- |
| `device_key` | String | 设备凭证，在 App 设置页查看。路径上的 key 若以 `topic_` 或 `group_` 开头，会改按频道发送 | `abcdef123456...` |
| `topic` | String | 频道名，至少 8 位。有该字段时不查设备表。App 生成的默认形态是 `topic_` 加 18 位小写字母和数字 | `topic_x9k2m1ab` |
| `body` | String | 通知正文。与 `title`、`ciphertext` 至少要有一个 | `服务器备份已完成` |
| `title` | String | 通知标题。缺省显示 `Zhi` | `运维通知` |
| `group` / `category` | String | 分组。历史列表可按它筛选。`level` 不是 `timeSensitive` 且分组非空时，通知进入「分组: 名字」渠道 | `生产环境` |
| `image` | String | 图片 URL。手机下载后用 Big Picture 展示，失败则只显示文本。服务端只转发这个字符串，不接收图片文件 | `https://example.com/snap.jpg` |
| `collapse_key` | String | 折叠 key。客户端用它的哈希作为通知 ID，同 key 的新通知盖住旧的；同时传给 FCM 的 `collapse_key` | `ci_build_42` |
| `actions` | String | 最多 3 个按钮，URL 必须是 `http` 或 `https`。简写 `标题:https://...\|标题:https://...` 固定 GET。JSON：`[{"title":"同意","url":"https://...","method":"POST"}]` | 见左 |
| `ttl` | Int | 交给 FCM 的存活秒数，例如 `300`。超时仍离线则不再投递。`0` 表示当前不在线就丢弃。不传则用 FCM 默认缓存（最长约 4 周） | `300` |
| `system_notify` | Int | 不传或 `0`：只发高优先级 Data。`1`：额外附带系统通知块。加密消息的系统弹窗正文固定为「收到一条端到端加密消息」，点开后才在本地解密入库 | `0` |
| `copy` | String | 复制文本。通知上会多一个复制按钮；App 开启自动复制时还会写入剪贴板 | `668899` |
| `url` | String | 点击通知或「打开链接」时打开的地址 | `https://github.com` |
| `level` | String | `active` 走常规或分组渠道。`timeSensitive` 走「紧急重要通知」渠道。`passive` 只把兼容优先级标低；Android 8 及以上仍按渠道重要性响铃，没有单独的静默渠道 | `timeSensitive` |
| `isArchive` | Int | `1` 写入本地历史（默认），`0` 只通知不入库 | `1` |
| `ciphertext` | String | 方案 C 的 AES-GCM 密文。有密文时，公网网关看不到标题和正文 | 见第 3 节 |
| `iv` | String | 可选。自定义 IV，与 `ciphertext` 一起交给客户端解密 |  |
| `sound` | String | 会写入历史记录。系统通知铃声跟随通知渠道，不按这个字段更换 | `default` |
| `badge` | Int | 会写入历史记录。当前版本不修改桌面角标 | `1` |
| `icon` | String | 会写入历史记录。通知小图标固定为应用图标 |  |

Data 载荷上限是 FCM 的 4KB。图片、长日志不要塞进正文，图片用 `image` URL。

### 4.1 什么时候走频道，什么时候走单设备

1. 请求里有 `topic`，或路径 key 以 `topic_` / `group_` 开头：调用 FCM Topic 发送。服务端不保存订阅名单，订阅发生在手机上的 Firebase。
2. 否则按 `device_key` 查库，把库里的 FCM Token 用于单播。
3. 同一个 `device_key` 再次注册会覆盖原来的 FCM Token。后登记的那台手机会收到之后的单播，先登记的那台不再收到。

频道名没有单独的发送密钥。知道名字的人就能向该频道发推送，所以要用 App 生成的长随机名，不要用 `server-error` 这类明文。

`system_notify=1` 时，FCM 通知块使用的渠道 ID 是 `zhi_urgent`、`zhi_group_分组名` 或 `zhi_default`。App 自己创建的常规和紧急渠道 ID 是 `zhi_channel_default`、`zhi_channel_urgent`，只有分组前缀 `zhi_group_` 相同。兜底弹窗因此不一定落进 App 里已经建好的渠道。默认的纯 Data 消息不受这个差异影响。

---

## 5. 服务端管理与注册接口

### 5.1 设备注册 /register
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

### 5.2 服务健康检查 /ping
- **Method**: `GET /ping`
- **Response**:
```json
{
  "code": 200,
  "message": "pong",
  "timestamp": 1792767009
}
```

### 5.3 服务运行信息 /info
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

## 6. 常见调用代码示例

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

    r = requests.post("https://push.qingmuhy.com/push", json=payload)
    return r.json()

send_push("YOUR_DEVICE_KEY", "爬虫完成", "共抓取 1200 条数据", group="数据抓取")
```

### Node.js / TypeScript
```typescript
import axios from 'axios';

async function push(deviceKey: string, title: string, body: string) {
  const res = await axios.post('https://push.qingmuhy.com/push', {
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
ZHI_SERVER="https://push.qingmuhy.com"

# 抓取 IP 并推送
MY_IP=$(curl -s ifconfig.me)
curl -s -X POST "$ZHI_SERVER/push" \
  -H "Content-Type: application/json" \
  -d "{\"device_key\": \"$ZHI_KEY\", \"title\": \"公网IP变动\", \"body\": \"当前IP: $MY_IP\", \"copy\": \"$MY_IP\"}"
```
