package com.qinglong.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.Task
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import com.qinglong.app.util.LiveLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    val filterTab: TaskFilterTab = TaskFilterTab.ALL,
    val isBatchMode: Boolean = false,
    val selectedTaskIds: Set<Int> = emptySet()
)

enum class TaskFilterTab { ALL, RUNNING, STOPPED }

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val query = _uiState.value.searchQuery
            val filter = when (_uiState.value.filterTab) {
                TaskFilterTab.ALL -> null
                TaskFilterTab.RUNNING -> "running"
                TaskFilterTab.STOPPED -> "stopped"
            }
            LiveLogger.i("Task", "加载任务列表: search=$query, filter=$filter")

            when (val result = taskRepository.getTasks(
                search = query.takeIf { it.isNotBlank() },
                filter = filter
            )) {
                is Result.Success -> {
                    LiveLogger.i("Task", "加载成功: ${result.data.size} 个任务")
                    _uiState.update { it.copy(isLoading = false, tasks = result.data) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "加载失败: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                showSearch = !it.showSearch,
                searchQuery = if (it.showSearch) "" else it.searchQuery
            )
        }
        if (!_uiState.value.showSearch) loadTasks()
    }

    fun toggleBatchMode() {
        _uiState.update {
            it.copy(
                isBatchMode = !it.isBatchMode,
                selectedTaskIds = emptySet()
            )
        }
    }

    fun toggleTaskSelection(taskId: Int) {
        _uiState.update {
            val newSet = it.selectedTaskIds.toMutableSet()
            if (taskId in newSet) newSet.remove(taskId) else newSet.add(taskId)
            it.copy(selectedTaskIds = newSet)
        }
    }

    fun setFilterTab(tab: TaskFilterTab) {
        _uiState.update { it.copy(filterTab = tab) }
        loadTasks()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadTasks()
    }

    fun runTask(taskId: Int) {
        viewModelScope.launch {
            when (taskRepository.runTask(taskId.toString())) {
                is Result.Success -> loadTasks()
                is Result.Error -> { /* TODO: show error toast */ }
            }
        }
    }

    fun stopTask(taskId: Int) {
        viewModelScope.launch {
            when (taskRepository.stopTask(taskId.toString())) {
                is Result.Success -> loadTasks()
                is Result.Error -> { /* TODO: show error toast */ }
            }
        }
    }

    fun enableTask(taskId: Int) {
        viewModelScope.launch {
            when (taskRepository.enableTasks(listOf(taskId.toString()))) {
                is Result.Success -> loadTasks()
                is Result.Error -> { }
            }
        }
    }

    fun disableTask(taskId: Int) {
        viewModelScope.launch {
            when (taskRepository.disableTasks(listOf(taskId.toString()))) {
                is Result.Success -> loadTasks()
                is Result.Error -> { }
            }
        }
    }

    fun batchEnable() {
        val ids = _uiState.value.selectedTaskIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when (taskRepository.enableTasks(ids.map { it.toString() })) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBatchMode = false, selectedTaskIds = emptySet()) }
                    loadTasks()
                }
                is Result.Error -> { }
            }
        }
    }

    fun batchDisable() {
        val ids = _uiState.value.selectedTaskIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when (taskRepository.disableTasks(ids.map { it.toString() })) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBatchMode = false, selectedTaskIds = emptySet()) }
                    loadTasks()
                }
                is Result.Error -> { }
            }
        }
    }

    fun batchDelete() {
        val ids = _uiState.value.selectedTaskIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            for (id in ids) {
                taskRepository.deleteTask(id.toString())
            }
            _uiState.update { it.copy(isBatchMode = false, selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }
}
