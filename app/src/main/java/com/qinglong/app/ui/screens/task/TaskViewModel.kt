package com.qinglong.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.Task
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
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
    val selectedTaskIds: Set<String> = emptySet()
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

            val filter = when (_uiState.value.filterTab) {
                TaskFilterTab.ALL -> null
                TaskFilterTab.RUNNING -> "running"
                TaskFilterTab.STOPPED -> "stopped"
            }

            when (val result = taskRepository.getTasks(
                search = _uiState.value.searchQuery.takeIf { it.isNotBlank() },
                filter = filter
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, tasks = result.data) }
                }
                is Result.Error -> {
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

    fun toggleTaskSelection(taskId: String) {
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

    fun runTask(taskId: String) {
        viewModelScope.launch {
            when (taskRepository.runTask(taskId)) {
                is Result.Success -> loadTasks()
                is Result.Error -> { /* TODO: show error toast */ }
            }
        }
    }

    fun stopTask(taskId: String) {
        viewModelScope.launch {
            when (taskRepository.stopTask(taskId)) {
                is Result.Success -> loadTasks()
                is Result.Error -> { /* TODO: show error toast */ }
            }
        }
    }

    fun enableTask(taskId: String) {
        viewModelScope.launch {
            when (taskRepository.enableTasks(taskId)) {
                is Result.Success -> loadTasks()
                is Result.Error -> { }
            }
        }
    }

    fun disableTask(taskId: String) {
        viewModelScope.launch {
            when (taskRepository.disableTasks(taskId)) {
                is Result.Success -> loadTasks()
                is Result.Error -> { }
            }
        }
    }

    fun batchEnable() {
        val ids = _uiState.value.selectedTaskIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            when (taskRepository.enableTasks(ids.joinToString(","))) {
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
            when (taskRepository.disableTasks(ids.joinToString(","))) {
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
                taskRepository.deleteTask(id)
            }
            _uiState.update { it.copy(isBatchMode = false, selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }
}
