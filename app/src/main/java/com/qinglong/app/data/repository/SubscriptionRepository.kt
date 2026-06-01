package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.model.Subscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    /**
     * 获取订阅列表
     * 官方: GET /api/subscriptions?searchValue=xxx
     */
    suspend fun getSubscriptions(search: String? = null): Result<List<Subscription>> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getSubscriptions(search)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: emptyList())
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun createSubscription(body: Map<String, Any>): Result<Subscription> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.createSubscription(body)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun updateSubscription(body: Map<String, Any>): Result<Subscription> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.updateSubscription(body)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 删除订阅
     * 官方: DELETE /api/subscriptions (body: [id], query: ?force=true)
     */
    suspend fun deleteSubscription(id: Int, force: Boolean = false): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.deleteSubscriptions(listOf(id), force)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 运行订阅（单个）
     * 官方没有单个运行 API，统一用批量接口传单个 id
     * PUT /api/subscriptions/run (body: [id])
     */
    suspend fun runSubscription(id: Int): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.runSubscriptions(listOf(id))
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 停止订阅（单个）
     * PUT /api/subscriptions/stop (body: [id])
     */
    suspend fun stopSubscription(id: Int): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.stopSubscriptions(listOf(id))
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
            val resp = api.enableSubscriptions(ids)
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
            val resp = api.disableSubscriptions(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchRun(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.runSubscriptions(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchStop(ids: List<Int>): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.stopSubscriptions(ids)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun batchDelete(ids: List<Int>, force: Boolean = true): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.deleteSubscriptions(ids, force)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 获取订阅实时日志
     * 官方: GET /api/subscriptions/:id/log
     * 返回 data 是字符串
     */
    suspend fun getSubscriptionLog(subId: Int): Result<String> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getSubscriptionLog(subId)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: "")
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }
}
