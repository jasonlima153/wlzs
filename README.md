# 网络调试精灵 - 去广告自动构建

通过 GitHub Actions 自动构建去广告版的「网络调试精灵」APK。

## 构建流程

```
原始APK → apktool反编译 → 删除广告SDK资源 → 禁用广告组件 → 修改广告初始化代码 → 重新编译 → 签名 → 输出
```

## 已移除的广告SDK

| SDK | 包名前缀 |
|-----|---------|
| TopOn (Anythink) 聚合平台 | `com.anythink.*` |
| 穿山甲 (ByteDance) | `com.bytedance.sdk.openadsdk.*` |
| 百度广告 | `com.baidu.mobads.*` |
| 腾讯优量汇 (GDT) | `com.qq.e.ads.*` |
| 快手广告 | `com.kwad.sdk.*` |
| 华为广告 (HiAd) | `com.huawei.openalliance.ad.*` |
| vivo 广告 | `com.vivo.mobilead.*` |
| OPPO 广告 (OPOS) | `com.opos.mobad.*` |
| 贝子广告 | `com.beizi.ad.*` |
| SmartDigiMKT | `com.smartdigimkt.*` |

## 使用方法

### 方式一: 通过 GitHub Actions 自动构建 (推荐)

1. Fork 或 Clone 此仓库
2. 将原始APK文件放入 `apk/` 目录，命名为 `netdebugger.apk`
3. 提交并推送到 `main` 分支
4. GitHub Actions 会自动触发构建
5. 构建完成后，在 Actions 页面下载产物，或从 Releases 页面下载

### 方式二: 手动触发构建

1. 进入仓库的 Actions 标签页
2. 选择 "构建去广告APK" 工作流
3. 点击 "Run workflow" 手动触发
4. 等待构建完成后下载产物

### 方式三: 本地构建

```bash
# 将原始APK放入 apk/ 目录
cp your_apk_file.apk apk/netdebugger.apk

# 执行构建脚本 (需要 JDK 17+)
chmod +x scripts/remove_ads.sh
# 本地运行时需修改 WORK_DIR 变量
bash scripts/remove_ads.sh
```

## 输出文件

构建完成后会生成:
- `output/netdebugger-ad-free-signed.apk` - 已签名的去广告APK

## 安装说明

1. **卸载原版**网络调试精灵 (签名不同，无法覆盖安装)
2. 将去广告版APK传输到手机
3. 安装APK (可能需要开启「允许安装未知来源应用」)

## 免责声明

此项目仅供学习和研究使用，请支持原版开发者。
