# 编译版本管理

## 版本号规则

从 v1.2.0 起，不再使用随机唯一码标识编译版本，改用**语义化版本号 + 时间戳**。

### 版本格式

```
{Major}.{Minor}.{Patch}
```

- **Major**：主版本号，重大功能重构时递增
- **Minor**：次版本号，新功能时递增
- **Patch**：补丁号，问题修复时递增

### APK 命名规则

```
{APP_NAME}-v{APP_VERSION}.apk
```

示例：`QingLong-v1.2.0.apk`

### 快照备份规则

编译时的代码快照保存到 `build_backups/v{APP_VERSION}_{YYYYMMDD_HHMMSS}/` 目录。

### 手动更新版本号

1. 修改 `app/build.gradle.kts` 中的 `versionName` 和版本函数
2. 版本号会自动同步到 `versionCode`
