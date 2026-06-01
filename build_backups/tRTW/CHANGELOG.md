# tRTW 版本变更日志

**应用名称**: 青龙助手 (QingLongAssistant)
**包名**: com.qinglong.app
**版本号**: v1.1.0 (Build tRTW)
**编译时间**: 2026-05-31 17:58

**基于版本**: HSFH

## 变更内容

### 新增
- **2FA 登录支持**：两步验证（Two-Factor Authentication）完整集成
  - 登录时自动检测面板是否开启了 2FA
  - 检测到后弹出验证码输入框，支持 6 位 TOTP 验证码
  - 调用 PUT /api/user/two-factor/login 完成认证
  - 支持取消 2FA 流程返回普通登录
  - 自动登录遇到 2FA 时提示用户手动登录

### 改动
- 重构登录流程以支持 2FA 挑战-响应模式

### 修复
- 无

### 优化
- 无
