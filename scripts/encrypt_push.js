#!/usr/bin/env node
/**
 * Zhi (吱) E2EE 加密推送脚本 (Node.js 16+)
 * 零第三方依赖，纯 Node.js 内置 crypto 与 http 模块
 */

const crypto = require('crypto');
const http = require('http');
const https = require('https');

function deriveKey(passphrase) {
  return crypto.createHash('sha256').update(passphrase, 'utf8').digest();
}

function encryptGCM(payloadObj, aesKey) {
  const key = deriveKey(aesKey);
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);

  const plaintext = JSON.stringify(payloadObj);
  const encrypted = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();

  // 格式: Base64(12字节IV + 密文 + 16字节Tag)
  const combined = Buffer.concat([iv, encrypted, tag]);
  return combined.toString('base64');
}

async function sendPush({ server, deviceKey, aesKey, title, body, group, copy, url, level, display = 1 }) {
  const payload = {
    title: title || '加密消息',
    body: body || '测试内容',
    group: group || '私密',
    copy: copy || '',
    url: url || '',
    level: level || 'active',
    display
  };

  const ciphertext = encryptGCM(payload, aesKey);
  const postData = JSON.stringify({
    device_key: deviceKey,
    ciphertext: ciphertext,
    // Keep display outside the ciphertext so the gateway can suppress
    // an optional FCM system-notification fallback without decrypting.
    display
  });

  const parsedUrl = new URL(`${server.replace(/\/+$/, '')}/push`);
  const isHttps = parsedUrl.protocol === 'https:';
  const client = isHttps ? https : http;

  const options = {
    hostname: parsedUrl.hostname,
    port: parsedUrl.port || (isHttps ? 443 : 80),
    path: parsedUrl.pathname,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(postData)
    }
  };

  console.log(`[*] 正在向 ${parsedUrl.href} 发送加密推送...`);
  console.log(`[*] 密文预览: ${ciphertext.slice(0, 32)}...`);

  return new Promise((resolve, reject) => {
    const req = client.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        console.log(`[+] 服务器响应 (${res.statusCode}): ${data}`);
        resolve(data);
      });
    });
    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

// 命令行直接调用支持: node encrypt_push.js <server> <deviceKey> <aesKey> <title> <body> [display]
if (require.main === module) {
  const args = process.argv.slice(2);
  if (args.length < 5) {
    console.log("用法: node encrypt_push.js <serverUrl> <deviceKey> <aesKey> <title> <body> [display]");
    console.log("示例: node encrypt_push.js https://push.qingmuhy.com my_key my_password '告警' '数据库离线'");
    process.exit(1);
  }

  sendPush({
    server: args[0],
    deviceKey: args[1],
    aesKey: args[2],
    title: args[3],
    body: args[4],
    display: args[5] === undefined ? 1 : Number(args[5])
  }).catch(err => {
    console.error("[-] 推送失败:", err);
    process.exit(1);
  });
}

module.exports = { encryptGCM, sendPush };
