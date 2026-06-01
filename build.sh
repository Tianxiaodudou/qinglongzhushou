#!/bin/bash
# 编译青龙助手 APK
# 用法: ./build.sh

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

# 生成唯一码：大写+小写+数字，4位
BUILD_CODE=$(cat /dev/urandom | tr -dc 'A-Za-z0-9' | head -c4)

echo ""
echo "🔨 开始编译..."
# 编译，不掩盖退出码
./gradlew assembleDebug --no-daemon --no-build-cache 2>&1

echo ""
echo "✅ 编译通过！"

# 编译成功后，创建备份目录
BACKUP_DIR="$BACKUPS_DIR/${BUILD_CODE}"
mkdir -p "$BACKUP_DIR"

echo "📸 代码快照: $BUILD_CODE"
echo "   备份目录: $BACKUP_DIR"

# 1. 记录编译环境信息
echo "编译时间: $(date '+%Y-%m-%d %H:%M:%S')" > "$BACKUP_DIR/git_info.txt"
echo "唯一码: $BUILD_CODE" >> "$BACKUP_DIR/git_info.txt"
echo "JDK: $(java -version 2>&1 | head -1)" >> "$BACKUP_DIR/git_info.txt"
echo "SDK: Android SDK build-tools $(ls $ANDROID_HOME/build-tools/ 2>/dev/null | head -1), platform android-$(ls $ANDROID_HOME/platforms/ 2>/dev/null | grep -oP 'android-\K\d+')" >> "$BACKUP_DIR/git_info.txt"
echo "Gradle: $(cat gradle/wrapper/gradle-wrapper.properties | grep distributionUrl | grep -oP 'gradle-\K[^-]+')" >> "$BACKUP_DIR/git_info.txt"

# 2. 生成 CHANGELOG.md（对比上一个版本）
LAST_BACKUP=$(ls -t "$BACKUPS_DIR/" 2>/dev/null | grep -v "^${BUILD_CODE}$" | head -1)
if [ -n "$LAST_BACKUP" ]; then
    LAST_CODE="$LAST_BACKUP"
else
    LAST_CODE="无"
fi

cat > "$BACKUP_DIR/CHANGELOG.md" << EOF
# ${BUILD_CODE} 版本变更日志

**基于版本**: ${LAST_CODE}

**编译时间**: $(date '+%Y-%m-%d %H:%M:%S')

**唯一码**: ${BUILD_CODE}

## 变更内容

（请手动填写本次编译的变更内容）

### 新增
- 

### 修复
- 

### 优化
- 
EOF
echo "   CHANGELOG.md: 已生成（请手动填写变更内容）"

# 3. 备份源码（排除 build/ 和 .gradle/）
rsync -a --exclude='build/' --exclude='.gradle/' --exclude='*.apk' "$PROJECT_DIR/" "$BACKUP_DIR/source/"
echo "   source/: 已备份"

# 4. 生成 uncommitted.diff（如果有 git）
if git rev-parse --git-dir > /dev/null 2>&1; then
    git diff > "$BACKUP_DIR/uncommitted.diff" 2>/dev/null || true
    echo "   uncommitted.diff: 已生成"
else
    echo "   uncommitted.diff: 跳过（非 git 仓库）"
fi

# 5. 复制 APK
APP_NAME="QingLong"
APP_VERSION="1.1.0"
APK_SRC="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DST="$APK_OUTPUT/${APP_NAME}-v${APP_VERSION}-${BUILD_CODE}.apk"

mkdir -p "$APK_OUTPUT"
cp "$APK_SRC" "$APK_DST"
cp "$APK_SRC" "$BACKUP_DIR/$(basename "$APK_DST")"

echo ""
echo "✅ 编译成功！"
echo "   唯一码:  $BUILD_CODE"
echo "   APK:    $APK_DST"
echo "   快照:   $BACKUP_DIR"
echo ""
echo "⚠️  请编辑 $BACKUP_DIR/CHANGELOG.md 填写本次变更内容"
ls -lh "$APK_DST"
