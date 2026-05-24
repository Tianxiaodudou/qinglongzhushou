# 编译唯一码规则

## 概述

每次在本机执行 `build_env/build.sh` 编译时，会自动生成一个 **4位唯一码**，用于标识本次编译的代码快照版本。

## 唯一码格式

- **长度：** 固定 4 位
- **字符范围：** 大写字母（A-Z）、小写字母（a-z）、数字（0-9）
- **示例：** `VRc5`、`aB3k`、`X9mL`

## APK 命名规则

```
{APP_NAME}-v{APP_VERSION}-{唯一码}.apk
```

示例：`QingLong-v1.1.0-VRc5.apk`

## 快照机制

每次编译时，`build.sh` 自动完成：

1. **生成唯一码** — 从 `/dev/urandom` 随机生成 4 位字符
2. **备份代码快照** — 保存到 `build_backups/{唯一码}/` 目录，包含：
   - `git_info.txt` — 分支名、commit SHA、commit 说明、编译时间
   - `uncommitted.diff` — 若有未提交的改动，保存 diff
   - `source/` — 完整源码副本（排除 build/ 和 .gradle/）
   - APK 文件副本
3. **APK 输出** — 存入 `apk_output/`，文件名带唯一码

## 使用场景

用户与 AI 交流时可直接引用唯一码，AI 通过查询对应快照定位当时的代码状态。

例如：
> "VRc5 那个版本的任务页面有个 bug..."
> "对比 aB3k 和 X9mL 两个版本的区别"

## 注意事项

- 唯一码与 APK 一一对应，不可重复使用
- 备份目录 `build_backups/` 仅存在于本机，不提交到 Git 仓库
- 本文件（BUILD_CODE.md）提交到仓库，供开发者参考
