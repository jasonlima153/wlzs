#!/bin/bash
# ============================================================
# 网络调试精灵 (cn.zengfs.netdebugger) 广告移除脚本
# 工作流程: apktool反编译 → 删除广告文件/组件/代码 → 重新打包签名
# ============================================================
set -e

WORK_DIR="/home/runner/work/wlzs/wlzs"
APK_SOURCE="${WORK_DIR}/apk/netdebugger.apk"
DECODED_DIR="${WORK_DIR}/decoded"
OUTPUT_DIR="${WORK_DIR}/output"
KEYSTORE_FILE="${WORK_DIR}/release.keystore"

echo "=========================================="
echo "  网络调试精灵 去广告构建脚本"
echo "=========================================="

# 验证 APK 是真实文件而非 LFS 指针
echo "[0/7] 验证 APK 文件..."
FILE_TYPE=$(file "$APK_SOURCE")
echo "  文件类型: $FILE_TYPE"
if echo "$FILE_TYPE" | grep -q "Git LFS"; then
  echo "  错误: APK 文件是 Git LFS 指针，未正确检出!"
  echo "  请确保 workflow 中使用了 lfs: true"
  exit 1
fi
if echo "$FILE_TYPE" | grep -q "ASCII text"; then
  echo "  错误: APK 文件是文本文件（可能是 LFS 指针），不是真实的 APK!"
  head -5 "$APK_SOURCE"
  exit 1
fi
echo "  APK 大小: $(du -h "$APK_SOURCE" | cut -f1)"
echo "  APK 验证通过"

# 1. 安装工具
echo "[1/7] 安装工具 (apktool, build-tools)..."

# apktool
mkdir -p "$HOME/apktool"
curl -sL "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar" -o "$HOME/apktool/apktool.jar"
curl -sL "https://bitbucket.org/AaronFeng753/waifu2x-extension-gui/downloads/apktool" -o "$HOME/apktool/apktool" 2>/dev/null || \
curl -sL "https://raw.githubusercontent.com/nicour/Apktool/master/scripts/linux/apktool" -o "$HOME/apktool/apktool" 2>/dev/null || true
if [ -f "$HOME/apktool/apktool" ]; then
  chmod +x "$HOME/apktool/apktool"
  export PATH="$HOME/apktool:$PATH"
else
  # 直接用 java -jar 调用
  echo "  apktool wrapper 下载失败, 将直接用 java -jar 调用"
fi

# Android build-tools (zipalign + apksigner)
mkdir -p "$HOME/build-tools"
BUILD_TOOLS_URL="https://dl.google.com/android/repository/build-tools_r34-linux.zip"
echo "  下载 Android build-tools..."
curl -sL "$BUILD_TOOLS_URL" -o /tmp/build-tools.zip
unzip -qo /tmp/build-tools.zip -d "$HOME/build-tools/"
export PATH="$HOME/build-tools/android-sdk/build-tools/34.0.0:$PATH"

# 验证工具
echo "  验证工具..."
which zipalign && echo "  zipalign: OK" || echo "  zipalign: 未找到"
which apksigner && echo "  apksigner: OK" || echo "  apksigner: 未找到"

# 2. 反编译 APK (完整反编译，包含 smali)
echo "[2/7] 反编译 APK (apktool 完整反编译)..."
rm -rf "$DECODED_DIR"

APKTOOL_CMD="java -jar $HOME/apktool/apktool.jar"
$APKTOOL_CMD d "$APK_SOURCE" -o "$DECODED_DIR" -f 2>&1

echo "  反编译完成, 目录结构:"
ls -la "$DECODED_DIR/" | head -20

# 检查 smali 目录
SMALI_DIRS=$(find "$DECODED_DIR" -maxdepth 1 -type d -name "smali*" | sort)
echo "  Smali 目录:"
for d in $SMALI_DIRS; do
  SMALI_COUNT=$(find "$d" -name "*.smali" | wc -l)
  echo "    $d ($SMALI_COUNT 个 smali 文件)"
done

# 3. 移除广告资源文件
echo "[3/7] 移除广告资源文件..."

# 百度广告插件
rm -f "$DECODED_DIR/assets/bdxadsdk.jar" && echo "  删除 bdxadsdk.jar"
# 腾讯优量汇插件
rm -rf "$DECODED_DIR/assets/gdt_plugin" && echo "  删除 gdt_plugin/"
# 穿山甲 GroMore 配置
rm -f "$DECODED_DIR/assets/gromore_config_5176912.json" && echo "  删除 gromore_config"
# 快手广告配置
rm -f "$DECODED_DIR/assets/ksad_common_encrypt_image.png" "$DECODED_DIR/assets/ksad_idc.json" && echo "  删除 ksad 配置"
# 穿山甲资源
rm -f "$DECODED_DIR/assets/na.czl" && echo "  删除 na.czl"
# OPPO 广告 UI 资源
rm -rf "$DECODED_DIR/assets/opos_module_biz_ui" && echo "  删除 opos 广告UI"
# 华为广告深色主题资源
rm -rf "$DECODED_DIR/assets/dark" && echo "  删除 hiad 深色主题"
# vivo 广告资源
rm -f "$DECODED_DIR/assets/vivo_module_biz_ui_"* "$DECODED_DIR/assets/vivo_module_web_"* && echo "  删除 vivo 广告资源"
# 百度 API SDK (嵌入式 APK)
rm -f "$DECODED_DIR/baidu-api-sdk-android.apk" "$DECODED_DIR/baidu-api-sdk-android.unaligned.apk" && echo "  删除百度API SDK"
# openmeasure (广告测量 SDK)
rm -rf "$DECODED_DIR/assets/openmeasure" && echo "  删除 openmeasure"
# 抖音相关
rm -rf "$DECODED_DIR/assets/lottie_json" 2>/dev/null

echo "  广告资源文件已清理"

# 4. 移除广告原生库 (SO 文件)
echo "[4/7] 移除广告原生库..."
AD_SO_FILES=(
  "libads-ac.so"
  "libads-c.so"
  "libpanglearmor.so"
  "libpangleflipped.so"
  "libopluslog.so"
  "libvivoantisdk.so"
)
for so in "${AD_SO_FILES[@]}"; do
  find "$DECODED_DIR/lib" -name "$so" -type f 2>/dev/null | while read f; do
    rm -f "$f" && echo "  删除 $f"
  done
done
echo "  广告原生库已清理"

# 5. 修改 AndroidManifest.xml - 禁用广告组件
echo "[5/7] 修改 AndroidManifest.xml 禁用广告组件..."

MANIFEST="$DECODED_DIR/AndroidManifest.xml"

# 所有需要禁用的广告组件名前缀 (Activity / Service / Receiver / Provider 通用)
AD_COMPONENT_PREFIXES=(
  "com.bytedance.sdk.openadsdk"
  "com.byted.live.lite"
  "com.bytedance.android.openliveplugin"
  "com.bytedance.sdk.open.douyin"
  "com.baidu.mobads"
  "com.baidu.oauth"
  "com.qq.e.ads"
  "com.qq.e.comm"
  "com.kwad.sdk"
  "com.kwad.auth"
  "com.huawei.openalliance.ad"
  "com.vivo.mobilead"
  "com.vivo.adloadsdk"
  "com.vivo.ic.dm"
  "com.opos.mobad"
  "com.opos.cmn"
  "com.opos.cmn.an"
  "com.beizi.ad"
  "com.beizi.fusion"
  "com.anythink.core"
  "com.anythink.core.api"
  "com.smartdigimkt"
  "com.smartdigimkt.sdk"
  "com.ss.android.socialbase"
  "com.ss.android.downloadlib"
  "com.byazt"
  "com.alipay.sdk"
  "com.sina.weibo"
  "com.tencent.connect"
  "com.tencent.tauth"
  "com.oplus.tblplayer"
)

# 使用 Python3 精确处理 AndroidManifest.xml (安全可靠)
python3 - "$MANIFEST" << 'PYEOF'
import re, sys

manifest_path = sys.argv[1]
with open(manifest_path, 'r', encoding='utf-8') as f:
    content = f.read()

ad_prefixes = [
    "com.bytedance.sdk.openadsdk",
    "com.byted.live.lite",
    "com.bytedance.android.openliveplugin",
    "com.bytedance.sdk.open.douyin",
    "com.baidu.mobads",
    "com.baidu.oauth",
    "com.qq.e.ads",
    "com.qq.e.comm",
    "com.kwad.sdk",
    "com.kwad.auth",
    "com.huawei.openalliance.ad",
    "com.vivo.mobilead",
    "com.vivo.adloadsdk",
    "com.vivo.ic.dm",
    "com.opos.mobad",
    "com.opos.cmn",
    "com.opos.cmn.an",
    "com.beizi.ad",
    "com.beizi.fusion",
    "com.anythink.core",
    "com.anythink.core.api",
    "com.smartdigimkt",
    "com.smartdigimkt.sdk",
    "com.ss.android.socialbase",
    "com.ss.android.downloadlib",
    "com.byazt",
    "com.alipay.sdk",
    "com.sina.weibo",
    "com.tencent.connect",
    "com.tencent.tauth",
    "com.oplus.tblplayer",
]

components = ["activity", "service", "receiver", "provider"]
modified_count = 0

for comp in components:
    # 匹配 <component ... android:name="xxx" ... > 标签
    pattern = r'(<' + comp + r'(\s[^>]*?)android:name="([^"]+)"([^>]*?)(/?>))'
    
    def replace_component(m):
        global modified_count
        full = m.group(0)
        pre_name = m.group(2)
        name = m.group(3)
        post_name = m.group(4)
        ending = m.group(5)
        
        # 检查是否是广告组件
        is_ad = False
        for prefix in ad_prefixes:
            if name.startswith(prefix):
                is_ad = True
                break
        
        if is_ad:
            modified_count += 1
            # 如果已有 enabled="false" 则跳过
            if 'android:enabled="false"' in full or "android:enabled='false'" in full:
                return full
            # 先移除已有的 enabled 属性 (避免 duplicate attribute 错误)
            cleaned_pre = re.sub(r'\s*android:enabled="[^"]*"', '', pre_name)
            cleaned_post = re.sub(r'\s*android:enabled="[^"]*"', '', post_name)
            # 在关闭 > 之前添加 android:enabled="false"
            if '/>' in ending:
                new_ending = ' android:enabled="false"/>'
            else:
                new_ending = ' android:enabled="false">'
            return '<' + comp + cleaned_pre + 'android:name="' + name + '"' + cleaned_post + new_ending
        return full
    
    content = re.sub(pattern, replace_component, content)

# 移除广告相关权限
ad_permissions = [
    'cn.zengfs.netdebugger.com.huawei.openalliance.ad.app.ins',
    'cn.zengfs.netdebugger.heytap.msp.mobad.BROADCAST_PERMISSION',
    'cn.zengfs.netdebugger.openadsdk.permission.TT_PANGOLIN',
    'com.asus.msa.SupplementaryDID.ACCESS',
    'com.google.android.gms.permission.AD_ID',
    'freemme.permission.msa',
    'oplus.permission.OPLUS_COMPONENT_SAFE',
]

for perm in ad_permissions:
    content = content.replace('<uses-permission android:name="' + perm + '"/>\n', '')
    content = content.replace("<uses-permission android:name=\"" + perm + "\"/>\n", "")

with open(manifest_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"  共修改/禁用 {modified_count} 个广告组件")
PYEOF

echo "  广告组件禁用完成"

# 6. 修改 Smali 代码 - 禁用广告 SDK 核心初始化入口
echo "[6/7] 修改 Smali 代码禁用广告 SDK (仅入口文件)..."

python3 - "$DECODED_DIR" << 'PYEOF'
import os, re, glob, sys

decoded_dir = sys.argv[1]
smali_dirs = sorted(glob.glob(os.path.join(decoded_dir, "smali*")))
total_modified = 0

# 策略: 只修改广告聚合平台和各SDK的核心初始化入口
# 不广撒网修改所有init方法，避免破坏非广告功能
target_files = {
    # TopOn Anythink 聚合平台入口
    "ATMediationManager": ["initSDK", "init"],
    "ATSDK": ["init", "initSDK"],
    "ATAdManager": ["init"],
    # 穿山甲
    "TTAdSdk": ["init"],
    "TTAdManager": ["init"],
    "PangleAdSdk": ["init"],
    # 百度广告
    "MobadsSDKSetup": ["init"],
    "BaiduMobads": ["initSDK"],
    # 腾讯优量汇
    "GDTAD": ["init"],
    "GDTSDK": ["init"],
    # 快手广告
    "KSAdSDK": ["init", "initSDK"],
    "KwadSDK": ["init"],
    # 华为广告
    "HiAd": ["init"],
    "PPSAdSdk": ["init"],
    # vivo广告
    "VivoAdSdk": ["init"],
    # OPPO广告
    "OppoAdSdk": ["init"],
    # 贝子广告
    "BeiZiAd": ["init"],
    # SmartDigiMKT
    "SDMAdManager": ["init"],
}

# 在 smali 中搜索这些类文件并修改
for smali_dir in smali_dirs:
    for class_name, init_methods in target_files.items():
        # 搜索这个类文件
        found_files = []
        for root, dirs, files in os.walk(smali_dir):
            for fname in files:
                if fname == f"{class_name}.smali":
                    found_files.append(os.path.join(root, fname))
        
        for fpath in found_files:
            try:
                with open(fpath, 'r', encoding='utf-8') as f:
                    content = f.read()
            except:
                continue
            
            original = content
            lines = content.split('\n')
            new_lines = []
            
            for line in lines:
                stripped = line.strip()
                new_lines.append(line)
                
                # 检查是否匹配目标方法
                for method_name in init_methods:
                    if re.match(rf'\.method\s+(?:public|private|protected|static)\s+.*\b{method_name}\b', stripped):
                        # 在方法签名后，第一行 .locals 或 .param 后插入 return-void
                        pass  # 后续通过行号处理
                        break
            
            # 更可靠的方式: 逐行处理
            lines = content.split('\n')
            new_lines = []
            i = 0
            while i < len(lines):
                stripped = lines[i].strip()
                
                # 检测目标方法定义
                matched_method = False
                for method_name in init_methods:
                    if re.match(rf'\.method\s+(?:public|private|protected|static)\s+.*\b{method_name}\b', stripped):
                        matched_method = True
                        break
                
                if matched_method:
                    new_lines.append(lines[i])
                    i += 1
                    # 找到 .locals 行，在后面插入 return-void
                    while i < len(lines):
                        cur_stripped = lines[i].strip()
                        new_lines.append(lines[i])
                        if cur_stripped.startswith('.locals') or cur_stripped.startswith('.param'):
                            # 获取缩进
                            indent = lines[i][:len(lines[i]) - len(cur_stripped)]
                            # 对于 void 方法直接 return-void
                            if '.method' in stripped and ')V' in stripped.split('.method')[1]:
                                new_lines.append(f"{indent}return-void")
                            # 对于非 void 方法, 修改 return 语句后面处理
                            elif '.method' not in stripped or 'V)' not in stripped:
                                # 非void方法: 注释整个方法体
                                pass  # 保持原样，不破坏
                            else:
                                new_lines.append(f"{indent}return-void")
                            i += 1
                            break
                        i += 1
                else:
                    new_lines.append(lines[i])
                    i += 1
            
            new_content = '\n'.join(new_lines)
            if new_content != original:
                with open(fpath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                total_modified += 1
                print(f"  修改: {fpath.split('smali')[-1]}")

# 额外策略: 直接将关键广告初始化 smali 文件替换为空实现
# 这比修改方法更安全
ad_init_files = [
    # Anythink 核心
    "com/anythink/core/api/ATInitSDK.smali",
    "com/anythink/core/adapter/common/ATAdManagerImpl.smali",
    # 穿山甲
    "com/bytedance/sdk/openadsdk/TTAdSdk.smali",
    "com/bytedance/sdk/openadsdk/core/init/s.smali",
]

for smali_dir in smali_dirs:
    for rel_path in ad_init_files:
        fpath = os.path.join(smali_dir, rel_path)
        if os.path.exists(fpath):
            print(f"  找到核心文件: {rel_path}")
            # 不替换文件，只找到不修改 (避免编译问题)

print(f"  共修改 {total_modified} 个 smali 入口文件")
PYEOF

echo "  Smali 代码修改完成"

# 7. 重新编译、对齐、签名
echo "[7/7] 重新编译、对齐、签名..."

# apktool 重新编译
echo "  apktool 编译..."
mkdir -p "$OUTPUT_DIR"
$APKTOOL_CMD b "$DECODED_DIR" -o "${OUTPUT_DIR}/unsigned.apk" 2>&1 | tee /tmp/apktool_build.log

BUILD_EXIT=${PIPESTATUS[0]}
echo "  apktool 退出码: $BUILD_EXIT"

if [ ! -f "${OUTPUT_DIR}/unsigned.apk" ]; then
  echo "  错误: apktool 编译失败! 完整日志:"
  cat /tmp/apktool_build.log
  exit 1
fi
echo "  编译成功: $(du -h ${OUTPUT_DIR}/unsigned.apk | cut -f1)"

# zipalign 对齐
echo "  zipalign 对齐..."
if command -v zipalign &> /dev/null; then
  zipalign -f 4 "${OUTPUT_DIR}/unsigned.apk" "${OUTPUT_DIR}/aligned.apk" 2>&1
else
  # 直接使用未对齐的文件
  echo "  zipalign 未找到, 跳过对齐步骤 (不影响签名)"
  cp "${OUTPUT_DIR}/unsigned.apk" "${OUTPUT_DIR}/aligned.apk"
fi

# 生成签名密钥
echo "  生成签名密钥..."
keytool -genkeypair -v \
  -keystore "$KEYSTORE_FILE" \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass netdebug123 \
  -keypass netdebug123 \
  -dname "CN=WLZS, OU=Dev, O=WLZS, L=CN, ST=CN, C=CN" 2>&1 | tail -3

# apksigner 签名
echo "  apksigner 签名..."
if command -v apksigner &> /dev/null; then
  apksigner sign \
    --ks "$KEYSTORE_FILE" \
    --ks-key-alias release \
    --ks-pass pass:netdebug123 \
    --key-pass pass:netdebug123 \
    --out "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk" \
    "${OUTPUT_DIR}/aligned.apk" 2>&1
else
  # 备选: 用 jarsigner
  echo "  apksigner 未找到, 使用 jarsigner..."
  jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore "$KEYSTORE_FILE" \
    -storepass netdebug123 -keypass netdebug123 \
    "${OUTPUT_DIR}/aligned.apk" release 2>&1 | tail -5
  cp "${OUTPUT_DIR}/aligned.apk" "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk"
fi

# 验证签名
echo "  验证签名..."
if command -v apksigner &> /dev/null; then
  apksigner verify --print-certs "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk" 2>&1
else
  jarsigner -verify "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk" 2>&1
fi

# 清理临时文件
rm -f "${OUTPUT_DIR}/unsigned.apk" "${OUTPUT_DIR}/aligned.apk"

echo ""
echo "=========================================="
echo "  构建完成!"
echo "=========================================="

# 显示最终 APK 信息
FINAL_APK="${OUTPUT_DIR}/netdebugger-ad-free-signed.apk"
if [ -f "$FINAL_APK" ]; then
  SIZE=$(du -h "$FINAL_APK" | cut -f1)
  echo "  文件: netdebugger-ad-free-signed.apk"
  echo "  大小: $SIZE"
  echo "  SHA256: $(sha256sum "$FINAL_APK" | cut -d' ' -f1)"
else
  echo "  错误: 最终 APK 文件不存在!"
  exit 1
fi
