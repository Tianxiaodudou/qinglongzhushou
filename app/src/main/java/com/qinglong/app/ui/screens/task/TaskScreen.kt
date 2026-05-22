package com.qinglong.app.ui.screens.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.Task
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("全部", "运行中", "已停止")

    Column(modifier = Modifier.fillMaxSize()) {
        // TopAppBar: [☰] + 定时任务 + [搜索] + [更多]
        QingLongTopBar(
            title = "定时任务",
            onMenuClick = onMenuClick,
            actions = {
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(
                        if (uiState.showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }
                IconButton(onClick = { viewModel.toggleBatchMode() }) {
                    Icon(
                        if (uiState.isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                        contentDescription = "批量操作"
                    )
                }
            }
        )

        // Search Bar
        AnimatedVisibility(visible = uiState.showSearch) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onClear = { viewModel.setSearchQuery("") }
            )
        }

        // Batch action bar
        AnimatedVisibility(visible = uiState.isBatchMode) {
            BatchActionBar(
                selectedCount = uiState.selectedTaskIds.size,
                onEnableAll = { viewModel.batchEnable() },
                onDisableAll = { viewModel.batchDisable() },
                onDeleteAll = { viewModel.batchDelete() }
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = uiState.filterTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.filterTab.ordinal]),
                    color = QingLongGreen
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.filterTab.ordinal == index,
                    onClick = {
                        viewModel.setFilterTab(TaskFilterTab.entries[index])
                    },
                    text = {
                        Text(
                            text = title,
                            color = if (uiState.filterTab.ordinal == index)
                                QingLongGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error!!,
                    onRetry = viewModel::loadTasks
                )
                uiState.tasks.isEmpty() -> EmptyView("暂无任务")
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.tasks,
                            key = { it.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                isSelected = task.id in uiState.selectedTaskIds,
                                isBatchMode = uiState.isBatchMode,
                                onToggleSelect = { viewModel.toggleTaskSelection(task.id) },
                                onRun = { viewModel.runTask(task.id) },
                                onStop = { viewModel.stopTask(task.id) },
                                onViewLog = { /* TODO: navigate to log */ },
                                onToggle = {
                                    if (task.isDisabled) {
                                        viewModel.enableTask(task.id)
                                    } else {
                                        viewModel.disableTask(task.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    isSelected: Boolean,
    isBatchMode: Boolean,
    onToggleSelect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onViewLog: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                QingLongGreen.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in batch mode
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = QingLongGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskStatusChip(
                        isRunning = task.isRunning,
                        isDisabled = task.isDisabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (task.lastRunTime != null) {
                        Text(
                            text = formatTimestamp(task.lastRunTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons
            if (!isBatchMode) {
                Row {
                    IconButton(onClick = onViewLog, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = "日志",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.isRunning) {
                        IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.StopCircle,
                                contentDescription = "停止",
                                modifier = Modifier.size(18.dp),
                                tint = StatusFailed
                            )
                        }
                    } else {
                        IconButton(onClick = onRun, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "运行",
                                modifier = Modifier.size(18.dp),
                                tint = QingLongGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskStatusChip(
    isRunning: Boolean,
    isDisabled: Boolean
) {
    val (text, color) = when {
        isRunning -> "运行中" to StatusRunning
        isDisabled -> "已停止" to StatusStopped
        else -> "待运行" to StatusWarning
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("搜索任务名称...") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = QingLongGreen,
            cursorColor = QingLongGreen
        )
    )
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选 $selectedCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onEnableAll) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("启用")
            }
            TextButton(onClick = onDisableAll) {
                Icon(Icons.Default.Pause, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("禁用")
            }
            TextButton(onClick = onDeleteAll) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp),
                    tint = StatusFailed)
                Spacer(Modifier.width(4.dp))
                Text("删除", color = StatusFailed)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp * 1000))
}
