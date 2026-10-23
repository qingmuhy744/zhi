# Security policy

## Supported versions

Security fixes target the latest stable release and the `main` branch. Older releases are not maintained separately; upgrade both the server and Android client when a fix is released.

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1.0 | :x:                |

## Reporting a vulnerability

Please use [GitHub private vulnerability reporting](https://github.com/qingmuhy744/zhi/security/advisories/new) for suspected vulnerabilities. Include affected versions, reproduction steps, expected and observed behavior, and the potential impact.

**Please redact**:
- Firebase service account private keys (`firebase-service-account.json`)
- Google Cloud / FCM API keys (`google-services.json`)
- Device Keys and End-to-End Encryption AES keys
- Private server domains and IPs

Reports are reviewed by the maintainer on a best-effort basis. Please keep unpatched vulnerabilities private while a fix is being investigated. The maintainer will coordinate disclosure and publish a security advisory when appropriate.

For leaked credentials, revoke or rotate them at Firebase / Google Cloud Console immediately; deleting them from the current file does not remove them from Git history or invalidate them.

## Security boundaries

- **方案 B (明文传输)**：适用于受信任内网或自建服务器私有链路。通过公共网关传输敏感数据存在中间人或审计顾虑。
- **方案 C (端到端加密 E2EE)**：利用本地对称密钥（AES-256-GCM / CBC）在客户端与发送端加解密。公网网关仅负责盲目转发密文，实现零知识中继。
- 公共服务器部署应开启 HTTPS，并配置反向代理防护与访问速率限制。
