#!/bin/bash
# 编译青龙助手 APK
# 用法: ./build.sh
# 每次编译自动递增补丁号 + 生成更新日志

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
WORKSPACE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_ENV="$WORKSPACE_DIR/build_env"
APK_OUTPUT="$WORKSPACE_DIR/apk_output"
BACKUPS_DIR="$WORKSPACE_DIR/build_backups"

export JAVA_HOME="$BUILD_ENV/amazon-corretto-17.0.19.10.1-linux-x64"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$BUILD_ENV/android-sdk"
export GRADLE_USER_HOME="$BUILD_ENV/.gradle"

cd "$PROJECT_DIR"

# 确保 gradlew 有执行权限
chmod +x gradlew

# ===== 版本号自动递增 =====
APP_VERSION=$(grep 'versionName' app/build.gradle.kts | grep -oP '"\K[^"]+' | head -1)
if [ -z "$APP_VERSION" ]; then
    echo "❌ 无法读取版本号，请检查 app/build.gradle.kts"
    exit 1
fi

echo "📦 当前版本: v${APP_VERSION}"

# 递增版本号：补丁位 0~10，满 10 进一（1.4.10 → 1.5.0）
MAJOR=$(echo "$APP_VERSION" | cut -d. -f1)
MINOR=$(echo "$APP_VERSION" | cut -d. -f2)
PATCH=$(echo "$APP_VERSION" | cut -d. -f3)
NEW_PATCH=$((PATCH + 1))
NEW_MINOR=$MINOR
if [ $NEW_PATCH -gt 10 ]; then
  NEW_PATCH=0
  NEW_MINOR=$((MINOR + 1))
fi
NEW_VERSION="${MAJOR}.${NEW_MINOR}.${NEW_PATCH}"

echo "🔄 递增版本: v${APP_VERSION} → v${NEW_VERSION}"

# 更新 app/build.gradle.kts
sed -i "s/versionName = \"${APP_VERSION}\"/versionName = \"${NEW_VERSION}\"/" app/build.gradle.kts
sed -i "s/val parts = \"${APP_VERSION}\"/val parts = \"${NEW_VERSION}\"/" app/build.gradle.kts

# 更新 LoginScreen.kt
sed -i "s/青龙助手 v${APP_VERSION}/青龙助手 v${NEW_VERSION}/" app/src/main/java/com/qinglong/app/ui/screens/login/LoginScreen.kt

# 更新 AppSettingsViewModel.kt
sed -i "s/appVersion: String = \"${APP_VERSION}\"/appVersion: String = \"${NEW_VERSION}\"/" app/src/main/java/com/qinglong/app/ui/screens/appsettings/AppSettingsViewModel.kt

APP_VERSION="$NEW_VERSION"

# ===== 生成更新日志（基于 git log） =====
CHANGELOG_ENTRIES=""
if git rev-parse --git-dir > /dev/null 2>&1; then
    # 获取自上一个 tag 以来的提交
    LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    if [ -n "$LAST_TAG" ]; then
        CHANGELOG_ENTRIES=$(git log "${LAST_TAG}..HEAD" --oneline --no-decorate 2>/dev/null || echo "")
    fi
    # 如果有未提交的 diff，也加进去
    UNCOMMITTED=$(git diff --stat 2>/dev/null | tail -1 || echo "")
fi
# 如果没有 git 信息，使用默认内容
if [ -z "$CHANGELOG_ENTRIES" ]; then
    CHANGELOG_ENTRIES="- 版本号自动递增至 v${APP_VERSION}"
fi

echo ""
echo "🔨 编译 v${APP_VERSION}..."
# 编译，不掩盖退出码
./gradlew assembleDebug --no-daemon --no-build-cache 2>&1

echo ""
echo "✅ 编译通过！"

# 编译成功后，创建备份目录
BUILD_TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_DIR="$BACKUPS_DIR/v${APP_VERSION}_${BUILD_TIMESTAMP}"
mkdir -p "$BACKUP_DIR"

echo "📸 备份目录: $BACKUP_DIR"

# 1. 记录编译环境信息
cat > "$BACKUP_DIR/git_info.txt" << EOF
编译时间: $(date '+%Y-%m-%d %H:%M:%S')
APP 版本: v${APP_VERSION}
JDK: $(java -version 2>&1 | head -1)
SDK: Android SDK build-tools $(ls $ANDROID_HOME/build-tools/ 2>/dev/null | head -1), platform android-$(ls $ANDROID_HOME/platforms/ 2>/dev/null | grep -oP 'android-\K\d+')
Gradle: $(cat gradle/wrapper/gradle-wrapper.properties | grep distributionUrl | grep -oP 'gradle-\K[^-]+')
EOF

# 2. 生成 CHANGELOG.md
{
  echo "# v${APP_VERSION} 版本变更日志"
  echo ""
  echo "**编译时间**: $(date '+%Y-%m-%d %H:%M:%S')"
  echo ""
  echo "## 变更内容"
  echo ""
  echo "${CHANGELOG_ENTRIES}"
} > "$BACKUP_DIR/CHANGELOG.md"
echo "   CHANGELOG.md: 已生成"
echo ""
echo "📋 === 更新日志 ==="
cat "$BACKUP_DIR/CHANGELOG.md"
echo ""

# 3. 备份源码
rsync -a --exclude='build/' --exclude='.gradle/' --exclude='*.apk' "$PROJECT_DIR/" "$BACKUP_DIR/source/"
echo "   source/: 已备份"

# 4. 生成 uncommitted.diff
if git rev-parse --git-dir > /dev/null 2>&1; then
    git diff > "$BACKUP_DIR/uncommitted.diff" 2>/dev/null || true
    echo "   uncommitted.diff: 已生成"
fi

# 5. 复制 APK
APP_NAME="QingLong"
APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DST="$APK_OUTPUT/${APP_NAME}-v${APP_VERSION}.apk"

mkdir -p "$APK_OUTPUT"
cp "$APK_SRC" "$APK_DST"
cp "$APK_SRC" "$BACKUP_DIR/${APP_NAME}-v${APP_VERSION}.apk"

echo ""
echo "✅ 编译成功！"
echo "   APK:    $APK_DST"
echo "   快照:   $BACKUP_DIR"
echo ""
ls -lh "$APK_DST"
