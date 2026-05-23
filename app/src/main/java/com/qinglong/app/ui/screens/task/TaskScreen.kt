package com.qinglong.app.ui.screens.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                                onViewLog = { }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun TaskCard(
    task: Task,
    isSelected: Boolean,
    isBatchMode: Boolean,
    onToggleSelect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onViewLog: () -> Unit
) {
    // 预计算状态，避免在 Compose 内多次访问计算属性
    val isActive = remember(task.id, task.pid, task.status) { task.isActive }
    val statusText = remember(task.id, task.pid, task.status, task.isDisabled) {
        when {
            task.isActive -> "运行中"
            task.isQueued -> "队列中"
            task.isInactive -> "已禁用"
            task.isIdle -> "空闲中"
            else -> "待运行"
        }
    }
    val statusColor = remember(task.id, task.pid, task.status, task.isDisabled) {
        when {
            task.isActive -> StatusRunning
            task.isQueued -> StatusWarning
            task.isInactive -> StatusStopped
            task.isIdle -> StatusIdle
            else -> StatusWarning
        }
    }
    val lastRunTimeStr = remember(task.lastRunTime) {
        task.lastRunTime?.let { formatTimestamp(it) }
    }

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
                    // Status chip
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (lastRunTimeStr != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastRunTimeStr,
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
                    if (isActive) {
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

// ===================== Helper Functions =====================

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 604800 -> "${diff / 86400}天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp * 1000))
        }
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
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "清除")
                }
            }
        }
    )
}
