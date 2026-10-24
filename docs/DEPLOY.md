# Zhi 服务端自建与部署指南 (Self-Hosted Guide)

Zhi 服务端基于 Go 语言编写，采用纯 Go 内嵌式 SQLite，编译为单一无外部依赖的二进制文件（仅约 18MB），单容器内存占用小于 20MB，支持快速自建与低成本运行。

---

## 方式一：Docker Compose 拉取镜像 (推荐)

部署机器不编译。打 `v*` tag 时，发布工作流在 CI 通过后把 `server/` 打成多架构镜像，推到 GHCR。当前版本是 `ghcr.io/qingmuhy744/zhi-server:v0.1.3`，同一版本还有不带 `v` 的 `0.1.3`，没有预发布号的标签同时是 `latest`。镜像里只有程序，没有数据库，也没有 Firebase 密钥。

GHCR 上第一次出现的包默认是私有的，匿名 `docker pull` 会返回 401。到包的设置页把它改成 Public，这一次之后的新版本会保持公开：<https://github.com/users/qingmuhy744/packages/container/package/zhi-server/settings>。

### 1. 准备一个目录

把下面三样放在同一个目录里，用相对路径挂载，整个目录可以一起备份。不要把密钥放进 git 仓库。

```text
zhi/
├── compose.yaml
├── config.yaml
├── firebase-service-account.json
└── data/
```

`compose.yaml` 没有顶层 `version`，内容就是：

```yaml
services:
  zhi-server:
    image: ghcr.io/qingmuhy744/zhi-server:v0.1.3
    pull_policy: always
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

仓库里的 `server/compose.yaml` 与这里相同。`PORT`、`DATABASE_PATH`、`FCM_SERVICE_ACCOUNT_FILE` 会覆盖 `config.yaml` 里的同名项。镜像入口是 `/app/zhi-server -config config.yaml`。

前面有 Nginx 或 Caddy 时，把端口改成 `127.0.0.1:8080:8080`，避免 8080 直接暴露在公网。

### 2. 先放好两个挂载文件

Compose 会绑定宿主机上的 `config.yaml` 和 `firebase-service-account.json`。文件不存在时，Docker 会建成目录，容器起不来。`config.yaml` 可以从仓库的 `server/config.example.yaml` 复制：

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

### 3. 放置 Firebase 密钥

把 Firebase 控制台下载的 Service Account 保存为同一目录下的 `firebase-service-account.json`。没有这个文件时，服务会以 MOCK 模式启动，接口能通，但不会真正发给 FCM。配置步骤见 [FIREBASE_SETUP.md](FIREBASE_SETUP.md)。

### 4. 拉取并启动

```bash
docker compose pull
docker compose up -d
docker compose logs -f
```

升级时改 `compose.yaml` 里的镜像标签，再执行同样的两条命令。数据库在 `./data`，密钥和配置在旁边，升级不会替换它们。

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
