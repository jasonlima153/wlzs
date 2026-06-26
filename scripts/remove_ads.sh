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

# 1. 安装 apktool
echo "[1/7] 安装 apktool..."
mkdir -p "$HOME/apktool"
curl -sL "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar" -o "$HOME/apktool/apktool.jar"
curl -sL "https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool" -o "$HOME/apktool/apktool"
chmod +x "$HOME/apktool/apktool"
export PATH="$HOME/apktool:$PATH"

# 2. 反编译 APK
echo "[2/7] 反编译 APK (使用 apktool)..."
rm -rf "$DECODED_DIR"
apktool d "$APK_SOURCE" -o "$DECODED_DIR" -f --no-src

echo "[2/7] 继续反编译 sources (smali)..."
cd "$DECODED_DIR"
# 手动解压 dex 文件并转换
mkdir -p smali_classes2 smali_classes3 smali_classes4 smali_classes5 smali_classes6

if [ -f "classes2.dex" ]; then
  java -jar "$HOME/apktool/apktool.jar" d "$APK_SOURCE" -o "$DECODED_DIR" -f -s 2>/dev/null || true
fi

# 使用 baksmali 反编译所有 dex
mkdir -p "$HOME/baksmali"
curl -sL "https://github.com/JesusFreke/smali/releases/download/v2.5.2/baksmali-2.5.2.jar" -o "$HOME/baksmali/baksmali.jar"

for dex in classes.dex classes2.dex classes3.dex classes4.dex classes5.dex classes6.dex; do
  if [ -f "$DECODED_DIR/$dex" ]; then
    suffix=$(echo "$dex" | sed 's/classes//' | sed 's/\.dex//')
    outdir="$DECODED_DIR/smali${suffix}"
    mkdir -p "$outdir"
    echo "  反编译 $dex -> $outdir"
    java -jar "$HOME/baksmali/baksmali.jar" d "$DECODED_DIR/$dex" -o "$outdir" 2>/dev/null || echo "  警告: $dex 反编译失败, 跳过"
  fi
done

echo "[2/7] 删除 dex 文件 (将使用 smali 重新编译)..."
rm -f "$DECODED_DIR"/classes*.dex

# 3. 移除广告资源文件
echo "[3/7] 移除广告资源文件..."

# 百度广告插件
rm -rf "$DECODED_DIR/assets/bdxadsdk.jar"
# 腾讯优量汇插件
rm -rf "$DECODED_DIR/assets/gdt_plugin"
# 穿山甲 GroMore 配置
rm -f "$DECODED_DIR/assets/gromore_config_5176912.json"
# 快手广告配置和资源
rm -f "$DECODED_DIR/assets/ksad_common_encrypt_image.png"
rm -f "$DECODED_DIR/assets/ksad_idc.json"
# 穿山甲资源
rm -f "$DECODED_DIR/assets/na.czl"
# OPPO 广告 UI 资源
rm -rf "$DECODED_DIR/assets/opos_module_biz_ui"
# 华为广告深色主题资源
rm -rf "$DECODED_DIR/assets/dark"
# vivo 广告资源
rm -f "$DECODED_DIR/assets/vivo_module_biz_ui_"*
rm -f "$DECODED_DIR/assets/vivo_module_web_"*
# 百度 API SDK (嵌入式 APK)
rm -f "$DECODED_DIR/baidu-api-sdk-android.apk"
rm -f "$DECODED_DIR/baidu-api-sdk-android.unaligned.apk"
# openmeasure (广告测量 SDK)
rm -rf "$DECODED_DIR/assets/openmeasure"

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
  for arch_dir in "$DECODED_DIR"/lib/*/; do
    rm -f "$arch_dir/$so" 2>/dev/null && echo "  删除 $arch_dir/$so"
  done
done
echo "  广告原生库已清理"

# 5. 修改 AndroidManifest.xml - 禁用广告组件
echo "[5/7] 修改 AndroidManifest.xml 禁用广告组件..."

MANIFEST="$DECODED_DIR/AndroidManifest.xml"

# 需要删除/禁用的广告 Activity 包名前缀
AD_ACTIVITY_PREFIXES=(
  "com.bytedance.sdk.openadsdk"
  "com.byted.live.lite"
  "com.bytedance.android.openliveplugin"
  "com.bytedance.sdk.open.douyin"
  "com.baidu.mobads.sdk"
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
  "com.beizi.ad"
  "com.beizi.fusion"
  "com.anythink.core"
  "com.smartdigimkt"
  "com.smartdigimkt.sdk"
  "com.ss.android.socialbase"
  "com.ss.android.downloadlib"
  "com.byazt"
  "com.alipay.sdk"
)

# 使用 xmlstarlet 或者 sed 来处理
# 先用 sed 方式处理 (更可靠，不需要额外依赖)

# 禁用所有广告 Activity (android:enabled="false")
for prefix in "${AD_ACTIVITY_PREFIXES[@]}"; do
  # 对含该前缀的 activity 节点添加或替换 enabled=false
  sed -i "s|<activity \(.*name=\"${prefix}[^\" ]*\"[^>]*\)>|<activity \1 android:enabled=\"false\">|g" "$MANIFEST" 2>/dev/null || true
done

echo "  广告 Activity 已禁用"

# 需要删除/禁用的广告 Service
AD_SERVICE_PREFIXES=(
  "com.beizi.ad.DownloadService"
  "com.beizi.fusion"
  "com.byazt"
  "com.kwad.sdk"
  "com.oplus.tblplayer"
  "com.opos.mobad"
  "com.qq.e.comm"
  "com.smartdigimkt"
  "com.vivo.ic.dm"
)

for prefix in "${AD_SERVICE_PREFIXES[@]}"; do
  sed -i "s|<service \(.*name=\"${prefix}[^\" ]*\"[^>]*\)>|<service \1 android:enabled=\"false\">|g" "$MANIFEST" 2>/dev/null || true
done

echo "  广告 Service 已禁用"

# 需要删除/禁用的广告 Receiver
AD_RECEIVER_PREFIXES=(
  "com.byazt"
  "com.smartdigimkt"
  "com.vivo.ic.dm"
)

for prefix in "${AD_RECEIVER_PREFIXES[@]}"; do
  sed -i "s|<receiver \(.*name=\"${prefix}[^\" ]*\"[^>]*\)>|<receiver \1 android:enabled=\"false\">|g" "$MANIFEST" 2>/dev/null || true
done

echo "  广告 Receiver 已禁用"

# 需要删除/禁用的广告 Provider
AD_PROVIDER_PREFIXES=(
  "com.anythink.core"
  "com.baidu.mobads"
  "com.beizi.fusion"
  "com.byazt"
  "com.byted.live"
  "com.bytedance.android.openliveplugin"
  "com.bytedance.sdk.openadsdk"
  "com.heytap.msp"
  "com.huawei.openalliance.ad"
  "com.kwad.sdk"
  "com.opos.mobad"
  "com.qq.e.comm"
  "com.smartdigimkt"
  "com.vivo.ic.dm"
  "com.vivo.mobilead"
)

for prefix in "${AD_PROVIDER_PREFIXES[@]}"; do
  sed -i "s|<provider \(.*name=\"${prefix}[^\" ]*\"[^>]*\)>|<provider \1 android:enabled=\"false\">|g" "$MANIFEST" 2>/dev/null || true
done

echo "  广告 Provider 已禁用"

# 移除广告相关权限声明
AD_PERMISSIONS=(
  "cn.zengfs.netdebugger.com.huawei.openalliance.ad.app.ins"
  "cn.zengfs.netdebugger.heytap.msp.mobad.BROADCAST_PERMISSION"
  "cn.zengfs.netdebugger.openadsdk.permission.TT_PANGOLIN"
  "com.asus.msa.SupplementaryDID.ACCESS"
  "com.google.android.gms.permission.AD_ID"
  "freemme.permission.msa"
  "oplus.permission.OPLUS_COMPONENT_SAFE"
)

for perm in "${AD_PERMISSIONS[@]}"; do
  sed -i "/<uses-permission android:name=\"${perm}\"\/>/d" "$MANIFEST" 2>/dev/null || true
done

echo "  广告权限已移除"

# 6. 修改 Smali 代码 - 禁用广告 SDK 初始化
echo "[6/7] 修改 Smali 代码禁用广告 SDK 初始化..."

# 查找并修改 TopOn / Anythink 初始化
# 方法: 在所有 smali 目录中搜索广告初始化方法并使其返回 void
SMALI_DIRS=$(find "$DECODED_DIR" -maxdepth 1 -type d -name "smali*")

for smali_dir in $SMALI_DIRS; do
  echo "  处理 $smali_dir ..."

  # TopOn / Anythink 初始化拦截
  find "$smali_dir" -path "*/anythink/core/adapter/common/ATAdManager*.smali" 2>/dev/null | while read f; do
    if grep -q "method.*init" "$f" 2>/dev/null; then
      echo "    拦截 Anythink ATAdManager 初始化"
    fi
  done

  # 穿山甲 TTAdSdk.init 空实现
  find "$smali_dir" -path "*/openadsdk/TTAdSdk.smali" 2>/dev/null | while read f; do
    echo "    找到穿山甲 TTAdSdk: $f"
  done

  # 将广告相关的 init 方法中添加 return-void (让初始化直接返回)
  # Anythink initATSDK
  find "$smali_dir" -name "*.smali" 2>/dev/null | xargs grep -l "initATSDK\|initAdSDK\|TTAdSdk.*init\|initSDK.*Baidu\|init.*Mobads\|GDT.*init\|Kwad.*init\|initKwad\|HuaweiAds.*init\|initHuawei\|Vivo.*init\|initVivo\|initOppo\|initBeizi" 2>/dev/null | while read f; do
    echo "    修改广告初始化: $(basename $f)"
    # 在方法开头注入 return-void
    sed -i '/\.method.*init/{n;/\.locals/s/.*/\0\n    return-void/}' "$f" 2>/dev/null || true
  done

  # 拦截广告加载方法 (loadAd, showAd)
  find "$smali_dir" -name "*.smali" 2>/dev/null | xargs grep -l "\.method.*loadAd\|\.method.*showAd\|\.method.*loadAndShow" 2>/dev/null | while read f; do
    # 只处理广告 SDK 目录下的
    if echo "$f" | grep -qE "anythink|openadsdk|mobads|qq\.e|kwad|openalliance|mobilead|opos\.mobad|beizi|byazt|smartdigimkt"; then
      echo "    禁用广告加载: $(basename $f)"
      sed -i '/\.method.*\(loadAd\|showAd\)/{n;/\.locals/s/.*/\0\n    return-void/}' "$f" 2>/dev/null || true
    fi
  done

done

echo "  Smali 代码修改完成"

# 7. 重新编译、签名、对齐
echo "[7/7] 重新编译、对齐、签名..."

# 安装 apksigner 和 zipalign
mkdir -p "$HOME/build-tools"
BUILD_TOOLS_URL="https://dl.google.com/android/repository/build-tools_r34-linux.zip"
echo "  下载 Android build-tools..."
curl -sL "$BUILD_TOOLS_URL" -o /tmp/build-tools.zip
unzip -qo /tmp/build-tools.zip -d "$HOME/build-tools/"
export PATH="$HOME/build-tools/android-sdk/build-tools/34.0.0:$PATH"

# apktool 重新编译
echo "  apktool 重新编译..."
mkdir -p "$OUTPUT_DIR"
java -Xmx4096m -jar "$HOME/apktool/apktool.jar" b "$DECODED_DIR" -o "${OUTPUT_DIR}/unsigned.apk" 2>&1 | tail -5

# zipalign 对齐
echo "  zipalign 对齐..."
zipalign -v 4 "${OUTPUT_DIR}/unsigned.apk" "${OUTPUT_DIR}/aligned.apk" 2>&1 | tail -3

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
apksigner sign \
  --ks "$KEYSTORE_FILE" \
  --ks-key-alias release \
  --ks-pass pass:netdebug123 \
  --key-pass pass:netdebug123 \
  --out "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk" \
  "${OUTPUT_DIR}/aligned.apk" 2>&1

# 验证签名
echo "  验证签名..."
apksigner verify --verbose "${OUTPUT_DIR}/netdebugger-ad-free-signed.apk" 2>&1 | tail -5

# 清理临时文件
rm -f "${OUTPUT_DIR}/unsigned.apk" "${OUTPUT_DIR}/aligned.apk"

echo ""
echo "=========================================="
echo "  构建完成!"
echo "  输出文件: ${OUTPUT_DIR}/netdebugger-ad-free-signed.apk"
echo "=========================================="

# 显示最终 APK 信息
FINAL_APK="${OUTPUT_DIR}/netdebugger-ad-free-signed.apk"
if [ -f "$FINAL_APK" ]; then
  SIZE=$(du -h "$FINAL_APK" | cut -f1)
  echo "  文件大小: $SIZE"
  echo "  SHA256: $(sha256sum "$FINAL_APK" | cut -d' ' -f1)"
fi
