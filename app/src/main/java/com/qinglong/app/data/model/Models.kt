package com.qinglong.app.data.model

import com.google.gson.annotations.SerializedName

// ===================== Generic API Response =====================

data class ApiResponse<T>(
    val code: Int,
    val data: T?,
    val message: String
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
    @SerializedName("_id") val id: String,
    val name: String,
    val command: String,
    val schedule: String,         // cron 表达式
    val isDisabled: Boolean,
    val lastRunTime: Long?,
    val lastRunningTime: Long?,
    val lastExecCode: Int?,        // 0=成功，其他=失败
    val isRunning: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

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
