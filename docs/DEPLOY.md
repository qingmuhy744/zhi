# Zhi 服务端自建与部署指南 (Self-Hosted Guide)

Zhi 服务端基于 Go 语言编写，采用纯 Go 内嵌式 SQLite，编译为单一无外部依赖的二进制文件（仅约 18MB），单容器内存占用小于 20MB，支持快速自建与低成本运行。

---

## 方式一：Docker Compose 一键部署 (推荐)

### 1. 拿到服务端目录

在要部署的机器上进入仓库的 `server/` 目录。镜像的构建上下文就是这个目录，`compose.yaml`、`Dockerfile` 和配置模板都在这里。

```bash
cd /path/to/zhi/server
```

### 2. 使用仓库里的 `compose.yaml`

在 `server/` 目录启动。文件没有顶层 `version`，内容就是：

```yaml
services:
  zhi-server:
    build: .
    container_name: zhi-server
    restart: unless-stopped
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
      - ./config.yaml:/app/config.yaml
      - ./firebase-service-account.json:/app/firebase-service-account.json
    environment:
      - PORT=8080
      - DATABASE_PATH=/app/data/zhi.db
      - FCM_SERVICE_ACCOUNT_FILE=/app/firebase-service-account.json
```

`PORT`、`DATABASE_PATH`、`FCM_SERVICE_ACCOUNT_FILE` 会覆盖 `config.yaml` 里的同名项。镜像入口是 `/app/zhi-server -config config.yaml`。

前面有 Nginx 或 Caddy 时，把端口改成 `127.0.0.1:8080:8080`，避免 8080 直接暴露在公网。

### 3. 先放好两个挂载文件

Compose 会绑定宿主机上的 `config.yaml` 和 `firebase-service-account.json`。文件不存在时，Docker 会建成目录，容器起不来。启动前执行：

```bash
cp config.example.yaml config.yaml
mkdir -p data
```

按需要改 `config.yaml`：

```yaml
port: "8080"
database_path: "data/zhi.db"
fcm_service_account: "firebase-service-account.json"
default_server_url: "https://push.yourdomain.com"
```

容器里实际使用的数据库路径和密钥路径以环境变量为准，分别是 `/app/data/zhi.db` 和 `/app/firebase-service-account.json`。

### 4. 放置 Firebase 密钥

把 Firebase 控制台下载的 Service Account 保存为 `server/firebase-service-account.json`。没有这个文件时，服务会以 MOCK 模式启动，接口能通，但不会真正发给 FCM。配置步骤见 [FIREBASE_SETUP.md](FIREBASE_SETUP.md)。

### 5. 启动服务

```bash
docker compose up -d --build
docker compose logs -f
```

---

## 方式二：二进制直接运行 (Systemd 服务)

### 1. 编译或上传二进制
```bash
cd server
CGO_ENABLED=0 go build -ldflags="-s -w" -o zhi-server .
```
将生成的文件上传至服务器 `/usr/local/bin/zhi-server`。

### 2. 创建 Systemd 服务文件 `/etc/systemd/system/zhi.service`
```ini
[Unit]
Description=Zhi Push Notification Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/var/lib/zhi
ExecStart=/usr/local/bin/zhi-server -config /var/lib/zhi/config.yaml
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### 3. 启动并开机自启
```bash
mkdir -p /var/lib/zhi/data
cp config.example.yaml /var/lib/zhi/config.yaml
cp firebase-service-account.json /var/lib/zhi/

systemctl daemon-reload
systemctl enable --now zhi
systemctl status zhi
```

---

## 方式三：配置 Nginx / Caddy 反向代理与 HTTPS (SSL)

客户端与第三方 Webhook 通常强制要求 `https://` 协议，建议使用 Caddy 或 Nginx 配置免费证书：

### Caddy 极简配置 (`/etc/caddy/Caddyfile`)
```caddy
push.yourdomain.com {
    reverse_proxy localhost:8080
}
```
Caddy 将自动申请并维护 Let's Encrypt SSL 免费证书。

### Nginx 配置
```nginx
server {
    listen 80;
    server_name push.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name push.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/push.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/push.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 客户端接入自建服务器

1. 打开手机端 **Zhi App**。
2. 进入底部导航 **「设置」** 页面。
3. 在 **「推送服务器」** 分类中，切换到 **「自建私有服务器 (Self-Hosted)」**。
4. 在输入框输入自建服务器公网域名，例如 `https://push.yourdomain.com`。
5. 点击 **「测试连接」**，验证返回 `连接成功 (200 OK)`。
6. 点击 **「保存并同步」**，App 将自动把当前的 `fcm_token` 注册至您的私有服务器中。
7. 返回「接口」页面，即可使用属于您自己的专属私有推送 API！
