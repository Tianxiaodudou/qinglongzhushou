# 青龙助手

青龙面板 Android 客户端，基于 Jetpack Compose + Kotlin + Clean Architecture。

## 功能模块

| 模块 | 说明 |
|------|------|
| 定时任务 | 默认首页，展示所有定时任务 |
| 订阅管理 | 管理京东、淘宝等订阅 |
| 日志管理 | 查看任务执行日志 |
| 环境变量 | 管理青龙环境变量 |
| 系统状态 | 查看系统资源使用情况 |
| 面板设置 | 青龙面板端的配置 |
| 应用设置 | APP 端的配置 |

## 技术栈

| 技术 | 说明 |
|------|------|
| UI | Jetpack Compose + Material Design 3 |
| 架构 | Clean Architecture + MVVM |
| DI | Hilt |
| 网络 | Retrofit + OkHttp + Kotlin Coroutines |
| 本地存储 | EncryptedSharedPreferences |
| 认证 | Bearer Token (JWT) |

## 登录说明

- HTTP / HTTPS 协议切换
- 域名与端口分开输入
- 认证凭证：Bearer Token (JWT)
- Token 存储于 EncryptedSharedPreferences

## 项目结构

```
app/src/main/java/com/qinglong/app/
├── data/
│   ├── api/          # Retrofit 接口
│   ├── model/        # DTO / Domain Model
│   └── repository/   # 数据仓库
├── di/               # Hilt 模块
├── ui/
│   ├── theme/        # Compose 主题
│   ├── components/   # 通用组件
│   ├── screens/      # 各页面
│   └── navigation/   # 导航
├── util/             # 工具类
└── navigation/       # 导航配置
```

## UI 规范

- **TopAppBar**：固定左侧导航按钮 + 页面标题 + 右侧操作按钮
- **Drawer**：左侧弹出导航栏（定时任务 / 订阅管理 / 日志管理等）
- **深色主题**：Material Design 3 深色模式，青绿品牌色 #00D9A6
