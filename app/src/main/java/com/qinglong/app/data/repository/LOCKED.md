# 🔒 LOCKED — Repositories.kt 中 AuthRepository 部分禁止回退

`AuthRepository`（在 `Repositories.kt` 中）包含多服务器管理的全部数据层逻辑。这个文件可以被修改其他部分，但禁止被 squash/覆盖回旧版单服务器模式。

**当前 AuthRepository 方法（必须保留）：**
- getServers() → JSON 数组反序列化
- saveServer(config, password)
- updateServer(old, new) → 原地更新 + 密码迁移
- deleteServer(serverId)
- savePassword(serverId, password)
- getPassword(serverId)
- getPort() / getHost() → 自动登录用
- loadServerConfig()
- logout() → 只删 token
- clearAuth() → 完全清除

**禁止回退到：**
- 单服务器独立 key 存储（无 JSON 数组）
- 无 getServers()/updateServer()/deleteServer()/savePassword()

修改时间：2026-05-24
