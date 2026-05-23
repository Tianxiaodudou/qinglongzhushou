# 🔒 LOCKED — 登录页面禁止修改

这两个文件是登录页面的完整实现，目前功能完善、体验良好：

- `LoginScreen.kt` — 登录界面 UI
- `LoginViewModel.kt` — 登录业务逻辑

**规则：**
1. 除非旅行者（用户）**明确点名修改登录页面**，否则绝对不碰这两个文件的任何一行
2. squash/覆盖、批量回退等操作必须跳过这两个文件
3. 就算是 CI 编译失败，也只能改 build.gradle / proguard / workflow，不能动登录页面

**当前版本亮点：**
- 多服务器保存/切换（JSON 数组去重存储）
- "管理已保存的服务器"弹窗：点击选择填充、⋮菜单编辑(四项+密码)/删除
- 键盘适配（imePadding）
- 密码明文小眼睛
- APP 重启自动登录获取新 token
- 覆盖安装数据迁移

**关联文件（同样保护）：**
- `Repositories.kt` 中的 `AuthRepository` 类（getServers/saveServer/updateServer/deleteServer/savePassword 等方法）

修改时间：2026-05-24
