#!/usr/bin/env python3
"""
Zhi (吱) E2EE 加密推送脚本 (Python 3)
使用 AES-256-GCM 对称加密，公网网关零知识，手机本地秒级解密。
依赖: pip install cryptography requests
"""

import argparse
import base64
import hashlib
import json
import os
import sys
import requests

try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError:
    print("错误: 需要安装 cryptography 库: pip install cryptography", file=sys.stderr)
    sys.exit(1)


def derive_key(passphrase: str) -> bytes:
    """使用 SHA-256 将用户密码/密钥派生为 32 字节 (256-bit) 密钥"""
    return hashlib.sha256(passphrase.encode("utf-8")).digest()


def encrypt_payload(data: dict, aes_key: str) -> str:
    """使用 AES-256-GCM 加密 JSON 载荷，格式为 Base64(12字节IV + 密文+Tag)"""
    key_bytes = derive_key(aes_key)
    aesgcm = AESGCM(key_bytes)
    
    # 生成 12 字节随机 IV
    iv = os.urandom(12)
    plaintext_bytes = json.dumps(data, ensure_ascii=False).encode("utf-8")
    
    # 加密
    ciphertext = aesgcm.encrypt(iv, plaintext_bytes, None)
    
    # 拼接 IV + 密文，并转为 Base64
    combined = iv + ciphertext
    return base64.b64encode(combined).decode("utf-8")


def main():
    parser = argparse.ArgumentParser(description="Zhi E2EE 密文推送工具")
    parser.add_argument("--server", default="https://push.qingmuhy.com", help="Zhi 服务端地址 (默认官方公共网关)")
    parser.add_argument("--key", required=True, help="设备 Device Key")
    parser.add_argument("--aes-key", required=True, help="与手机端 App 相同的 AES 加密密钥")
    parser.add_argument("--title", default="端到端加密通知", help="通知标题")
    parser.add_argument("--body", required=True, help="通知正文")
    parser.add_argument("--group", default="私密通道", help="通知分组")
    parser.add_argument("--copy", default="", help="快捷/自动复制内容")
    parser.add_argument("--url", default="", help="跳转链接")
    parser.add_argument("--level", default="active", choices=["active", "timeSensitive", "passive"], help="时效级别")

    args = parser.parse_args()

    payload = {
        "title": args.title,
        "body": args.body,
        "group": args.group,
        "copy": args.copy,
        "url": args.url,
        "level": args.level
    }

    # 加密为密文
    ciphertext = encrypt_payload(payload, args.aes_key)

    # 发送请求到服务端（服务端只看到密文）
    post_url = f"{args.server.rstrip('/')}/push"
    body_data = {
        "device_key": args.key,
        "ciphertext": ciphertext
    }

    print(f"[*] 正在向 {post_url} 发送加密推送...")
    print(f"[*] 密文预览 (长度 {len(ciphertext)}): {ciphertext[:32]}...")

    resp = requests.post(post_url, json=body_data, timeout=10)
    print(f"[+] 服务器响应 ({resp.status_code}): {resp.text}")


if __name__ == "__main__":
    main()
