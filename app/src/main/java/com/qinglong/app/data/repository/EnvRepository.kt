package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.model.ApiResponse
import com.qinglong.app.data.model.EnvVariable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    suspend fun getEnvVariables(search: String? = null, type: String? = null): Result<List<EnvVariable>> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getEnvVariables(search, type)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: emptyList())
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun createEnvVariable(body: Map<String, Any>): Result<EnvVariable> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.createEnvVariables(listOf(body))
            if (resp.isSuccessful && resp.body()?.code == 200 && resp.body()?.data?.isNotEmpty() == true) {
                Result.Success(resp.body()?.data!!.first())
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun updateEnvVariable(id: Int, body: Map<String, Any>): Result<EnvVariable> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val fullBody = body.toMutableMap()
            fullBody["id"] = id
            val resp = api.updateEnvVariable(fullBody)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun deleteEnvVariable(id: Int): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.deleteEnvVariables(listOf(id))
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun toggleStatus(id: Int, enable: Boolean): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = if (enable) {
                api.enableEnvVariables(listOf(id))
            } else {
                api.disableEnvVariables(listOf(id))
            }
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchPin(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.pinEnvVariables(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchUnpin(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.unpinEnvVariables(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchDelete(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.deleteEnvVariables(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchEnable(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.enableEnvVariables(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchDisable(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.disableEnvVariables(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }
}
