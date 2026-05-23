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
    // 计算属性：是否正在运行（有pid且status为0或1）
    val isActive: Boolean get() = pid != null && (status == null || status.toInt() != 2)
    // 计算属性：是否已禁用
    val isInactive: Boolean get() = isDisabled == 1 || status?.toInt() == 2
    // 计算属性：是否空闲中
    val isIdle: Boolean get() = pid == null && (status == null || status.toInt() == 1) && isDisabled != 1
    // 计算属性：是否队列中
    val isQueued: Boolean get() = status?.toInt() == 0 && pid == null
    // 兼容旧字段名
    val lastRunTime: Long? get() = last_execution_time?.toLong()
    val lastRunningTime: Long? get() = last_running_time?.toLong()
}

// ===================== Subscription =====================

data class Subscription(
    @SerializedName("_id") val id: String,
    val name: String,
    val type: String,             // "jd" | "tx" | "tb"
    val url: String,
    val schedule: String,
    val isDisabled: Boolean,
    val lastRunTime: Long?,
    val lastExecCode: Int?,
    val createdAt: Long,
    val updatedAt: Long
)

// ===================== Log =====================

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
