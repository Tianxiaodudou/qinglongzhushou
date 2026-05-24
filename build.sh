#!/bin/bash
# 本地编译青龙助手 APK
# 与 GitHub Actions CI workflow 完全一致
# 用法: ./build.sh

set -e

WORKSPACE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_ENV="$WORKSPACE_DIR/build_env"
APK_OUTPUT="$WORKSPACE_DIR/apk_output"
PROJECT_DIR="$BUILD_ENV/qinglong"

export JAVA_HOME="$BUILD_ENV/jdk-17.0.19+10"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$BUILD_ENV/android-sdk"
export GRADLE_USER_HOME="$BUILD_ENV/.gradle"

cd "$PROJECT_DIR"

# 确保 gradlew 有执行权限
chmod +x gradlew

# 生成唯一码：大写+小写+数字，4位
BUILD_CODE=$(cat /dev/urandom | tr -dc 'A-Za-z0-9' | head -c4)

# 备份当前代码状态
BACKUP_DIR="$WORKSPACE_DIR/build_backups/${BUILD_CODE}"
mkdir -p "$BACKUP_DIR"

echo "📸 代码快照: $BUILD_CODE"
echo "   备份目录: $BACKUP_DIR"

# 记录 git 状态
cd "$PROJECT_DIR"
echo "分支: $(git branch --show-current)" > "$BACKUP_DIR/git_info.txt"
echo "提交: $(git rev-parse HEAD)" >> "$BACKUP_DIR/git_info.txt"
echo "说明: $(git log --oneline -1)" >> "$BACKUP_DIR/git_info.txt"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$BACKUP_DIR/git_info.txt"
echo "唯一码: $BUILD_CODE" >> "$BACKUP_DIR/git_info.txt"

# 备份源码（排除 build/ 和 .gradle/）
git diff HEAD > "$BACKUP_DIR/uncommitted.diff" 2>/dev/null || true
rsync -a --exclude='build/' --exclude='.gradle/' --exclude='*.apk' "$PROJECT_DIR/" "$BACKUP_DIR/source/"

echo ""
echo "🔨 开始编译 (与 CI 一致: --stacktrace --no-daemon --no-build-cache)..."
./gradlew assembleDebug --stacktrace --no-daemon --no-build-cache

# APK 命名: {APP_NAME}-v{APP_VERSION}-{BUILD_CODE}.apk
APP_NAME="QingLong"
APP_VERSION="1.1.0"
APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DST="$APK_OUTPUT/${APP_NAME}-v${APP_VERSION}-${BUILD_CODE}.apk"

mkdir -p "$APK_OUTPUT"
cp "$APK_SRC" "$APK_DST"

# 把 APK 也复制到备份目录
cp "$APK_SRC" "$BACKUP_DIR/$(basename "$APK_DST")"

echo ""
echo "✅ 编译成功！"
echo "   唯一码:  $BUILD_CODE"
echo "   APK:    $APK_DST"
echo "   快照:   $BACKUP_DIR"
ls -lh "$APK_DST"
