# 🔒 LOCKED — 登录页面功能绝对禁止修改

## 受保护文件

| 文件 | 内容 |
|------|------|
| `LoginScreen.kt` | 登录界面 UI（493行） |
| `LoginViewModel.kt` | 登录业务逻辑（248行） |
| `../data/repository/Repositories.kt` | `AuthRepository` 类（多服务器JSON数组存储） |

## 绝对禁令

1. **绝不修改**登录页面的任何功能、UI、交互逻辑
2. **绝不删除**登录页面的任何现有功能
3. **绝不重构**登录页面代码
4. squash/覆盖、批量回退等操作**必须跳过**这些文件
5. 即使 CI 编译失败，只能改 build.gradle / proguard / workflow，**不能动登录页面**
6. 任何对 `AuthRepository` 中 `getServers/saveServer/updateServer/deleteServer/savePassword/migrateIfNeeded` 的修改均被禁止

## 当前功能清单（锁定状态）

- ✅ 服务端输入：HTTP/HTTPS 协议切换、域名、端口、用户名、密码
- ✅ 密码明文小眼睛切换
- ✅ 键盘适配（imePadding）
- ✅ 状态栏适配（statusBarsPadding）
- ✅ 管理已保存的服务器弹窗（多服务器 JSON 数组去重存储）
- ✅ 服务器编辑弹窗（域名、端口、用户名、密码四项编辑 + 删除确认）
- ✅ APP 重启自动登录获取新 token
- ✅ 覆盖安装数据迁移（旧独立 key → 新 JSON 数组）
- ✅ 登录后 API 实例动态创建（ApiManager）
- ✅ 渐变背景 + 图标 Logo
- ✅ 登录成功自动跳转任务页

## 例外

仅当旅行者（用户）**明确说"修改登录页面"**时，才可解锁。
除此之外，任何理由不得触碰上述文件。

---
锁定时间：2026-05-24
锁定人：派蒙（奉旅行者之命）
