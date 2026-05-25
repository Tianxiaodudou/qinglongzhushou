package com.qinglong.app.data.model

import com.google.gson.annotations.SerializedName

// ===================== Generic API Response =====================

data class ApiResponse<T>(
    val code: Int,
    val data: T?,
    val message: String
)

// 分页响应：/api/crons 返回 {code:200, data: {data:[Task], total:83}}
data class PagedData<T>(
    val data: List<T>?,
    val total: Int?
)

// ===================== Auth =====================

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val code: Int,
    val data: LoginData?,
    val message: String
)

data class LoginData(
    val token: String  // "Bearer eyJhbGciOiJI..."
)

// ===================== Server Config =====================

data class ServerConfig(
    val id: String,
    val name: String,
    val protocol: String,   // "http" | "https"
    val domain: String,
    val port: Int,
    val username: String,
    val isDefault: Boolean = false
)

// ===================== Task =====================

data class Task(
    val id: Int,
    val name: String,
    val command: String,
    val schedule: String,         // cron 表达式
    val status: Number?,          // 0=running, 1=idle, 2=disabled
    val pid: Number?,             // null=未运行, 有值=正在运行
    val isDisabled: Int?,         // 0=启用, 1=禁用
    val isSystem: Int?,
    val isPinned: Int?,
    val labels: List<String>?,
    val last_running_time: Number?,     // 耗时(秒)
    val last_execution_time: Number?,   // 上次执行时间(Unix秒)
    val sub_id: Int?,
    val log_path: String?,
    val log_name: String?,
    val extra_schedules: Any?,
    val task_before: Any?,
    val task_after: Any?,
    val allow_multiple_instances: Int?,
    val createdAt: String?,
    val updatedAt: String?
) {
    // 计算属性：是否运行中（status=0 且有 pid）
    val isRunning: Boolean get() = status?.toDouble() == 0.0 && pid != null
    // 计算属性：是否队列中（status=0.5）
    val isQueued: Boolean get() = status?.toDouble() == 0.5
    // 计算属性：是否空闲中（status=1 且 isDisabled!=1）
    val isIdle: Boolean get() = status?.toDouble() == 1.0 && isDisabled != 1
    // 计算属性：是否已禁用（isDisabled=1 且 status=1/idle）
    val isDisabledFlag: Boolean get() = isDisabled == 1 && (status == null || status.toDouble() == 1.0)
    // 兼容旧属性
    @Deprecated("Use isRunning instead")
    val isActive: Boolean get() = isRunning
    @Deprecated("Use isDisabledFlag instead")
    val isInactive: Boolean get() = isDisabledFlag
    // 兼容旧字段名
    val lastRunTime: Long? get() {
        val raw = last_execution_time?.toLong()
        return if (raw != null && raw > 0) raw else null
    }
    val lastRunningTime: Long? get() = last_running_time?.toLong()
}

// ===================== Subscription =====================

data class Subscription(
    val id: Int,                  // 数字ID（API返回 Int）
    val name: String,             // 订阅名称
    val type: String,             // "public-repo" 等
    val url: String,              // 仓库地址
    val schedule: String?,        // cron 表达式
    @SerializedName("is_disabled") val isDisabled: Int?,  // 0=启用, 1=禁用
    val status: Int?,             // 状态
    val pid: Int?,                // 进程ID
    val alias: String?,           // 别名
    val whitelist: String?,       // 白名单
    val blacklist: String?,       // 黑名单
    val extensions: String?,      // 扩展名
    val branch: String?,          // 分支
    val schedule_type: String?,   // 调度类型
    val autoAddCron: Int?,        // 自动添加定时
    val autoDelCron: Int?,        // 自动删除定时
    val log_path: String?,        // 日志路径
    val createdAt: String?,       // 创建时间
    val updatedAt: String?        // 更新时间
)

// ===================== Log =====================

/**
 * 历史日志文件信息
 * GET /api/crons/{id}/logs 返回的单个日志文件
 */
data class CronLogFile(
    val filename: String,
    val directory: String,
    val time: Double
) {
    /** 完整的日志文件路径 */
    val fullPath: String get() = "$directory/$filename"
}

data class TaskLog(
    val id: String,
    val taskId: String,
    val taskName: String,
    val content: String,
    val execCode: Int,
    val startTime: Long,
    val endTime: Long
)

// ===================== Env Variable =====================

data class EnvVariable(
    @SerializedName("_id") val id: String,
    val name: String,
    val value: String,
    val remarks: String?,
    val timestamp: Long
)

// ===================== System Status =====================

data class SystemStatus(
    val cpu: CpuInfo,
    val memory: MemoryInfo,
    val disk: DiskInfo,
    val network: NetworkInfo,
    val process: List<ProcessInfo>,
    val diskIo: DiskIoInfo,
    val netIo: NetIoInfo,
    val bootTime: Long,
    val uptime: Long
)

data class CpuInfo(
    val count: Int,
    val used: Float,
    val cores: List<Float>
)

data class MemoryInfo(
    val total: Long,
    val used: Long,
    val free: Long,
    val available: Long,
    val usedPercent: Float
)

data class DiskInfo(
    val total: Long,
    val used: Long,
    val free: Long,
    val usedPercent: Float
)

data class NetworkInfo(
    val inbound: Long,
    val outbound: Long
)

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val cpu: Float,
    val memory: Float
)

data class DiskIoInfo(
    val readCount: Long,
    val writeCount: Long,
    val readBytes: Long,
    val writeBytes: Long
)

data class NetIoInfo(
    val bytesSent: Long,
    val bytesRecv: Long,
    val packetsSent: Long,
    val packetsRecv: Long
)

// ===================== Cron Views =====================

data class CronViewFilter(
    val property: String,   // name, command, status, isDisabled, labels, sub_id
    val operation: String,  // Reg, NotReg, In, Nin
    val value: String
)

data class CronViewSort(
    val property: String,
    val type: String        // "ascending" | "descending"
)

data class ViewItem(
    val id: Int,
    val name: String,
    val type: Int,          // 1=系统内置, 2=用户自定义
    val filters: List<CronViewFilter>?,
    val sorts: List<CronViewSort>?,
    val filterRelation: String?,  // "and" | "or"
    val position: Long?,
    val isDisabled: Int?
)

data class ViewsResponse(
    val code: Int,
    val data: List<ViewItem>?,
    val message: String
)
