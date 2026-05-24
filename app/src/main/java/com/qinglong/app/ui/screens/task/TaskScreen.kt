package com.qinglong.app.ui.screens.task

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.CronLogFile
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

    // 拦截系统返回键：搜索状态时关闭搜索回到视图标签
    if (uiState.showSearch) {
        BackHandler {
            viewModel.toggleSearch()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TopAppBar: [☰] + 定时任务 + [搜索] + [批量]
        QingLongTopBar(
            title = "定时任务",
            onMenuClick = onMenuClick,
            actions = {
                // 视图管理按钮
                IconButton(onClick = { viewModel.showViewManager() }) {
                    Icon(Icons.Default.ViewList, contentDescription = "视图管理")
                }
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
                onClear = { viewModel.setSearchQuery("") },
                onSearch = { viewModel.searchAllTasks(uiState.searchQuery) }
            )
        }

        // Batch action bar
        AnimatedVisibility(visible = uiState.isBatchMode && uiState.selectedTaskIds.isNotEmpty()) {
            BatchActionBar(
                selectedCount = uiState.selectedTaskIds.size,
                onEnableAll = { viewModel.batchEnable() },
                onDisableAll = { viewModel.batchDisable() },
                onDeleteAll = { viewModel.batchDelete() }
            )
        }

        // Tab Row — 动态从 API 视图标签加载
        if (uiState.viewTabs.isNotEmpty()) {
            if (uiState.viewTabs.size <= 4) {
                // 标签少时用 ScrollableTabRow 或普通 TabRow
                TabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    uiState.viewTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTabIndex == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(tab.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            } else {
                // 标签多时用 ScrollableTabRow
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    uiState.viewTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTabIndex == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(tab.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }

        // Content
        val isSearching = uiState.showSearch && uiState.searchQuery.isNotBlank()
        when {
            uiState.isLoading && uiState.tasks.isEmpty() -> LoadingView()
            uiState.error != null && uiState.tasks.isEmpty() -> ErrorView(
                message = uiState.error!!,
                onRetry = { viewModel.loadViews() }
            )
            uiState.tasks.isEmpty() && isSearching -> EmptyView("未搜索到「${uiState.searchQuery}」相关结果")
            uiState.tasks.isEmpty() -> EmptyView("暂无任务")
            else -> {
                // 搜索模式提示
                if (isSearching) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "搜索「${uiState.searchQuery}」的结果（共 ${uiState.tasks.size} 个）",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isBatchMode = uiState.isBatchMode,
                            isSelected = task.id in uiState.selectedTaskIds,
                            onToggleSelect = { viewModel.toggleTaskSelection(task.id) },
                            onRun = { viewModel.runTask(task.id) },
                            onStop = { viewModel.stopTask(task.id) },
                            onEdit = { viewModel.showEditDialog(task) },
                            onDelete = { viewModel.showDeleteConfirm(task) },
                            onViewLog = { viewModel.showLogDialog(task) },
                            onEnable = { viewModel.enableTask(task.id) },
                            onDisable = { viewModel.disableTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // ========== 弹窗 ==========

    // 新建任务弹窗
    if (uiState.showCreateDialog) {
        TaskCreateDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { /* TODO */ }
        )
    }

    // 编辑任务弹窗
    if (uiState.showEditDialog && uiState.editingTask != null) {
        TaskEditDialog(
            task = uiState.editingTask!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { /* TODO */ }
        )
    }

    // 删除确认
    if (uiState.showDeleteConfirm && uiState.deletingTask != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除任务「${uiState.deletingTask!!.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(uiState.deletingTask!!.id)
                    viewModel.hideDeleteConfirm()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) { Text("取消") }
            }
        )
    }

    // 日志弹窗
    if (uiState.showLogDialog && uiState.logTask != null) {
        if (uiState.logTask!!.isRunning) {
            // 运行中的任务：直接显示实时日志
            TaskLogDetailDialog(
                task = uiState.logTask!!,
                logContent = uiState.logContent,
                isLoading = uiState.isLoadingLog,
                onDismiss = { viewModel.hideLogDialog() }
            )
        } else if (uiState.isShowingLogDetail && uiState.selectedLogFile != null) {
            // 查看某条历史日志详情
            TaskLogDetailDialog(
                task = uiState.logTask!!,
                logContent = uiState.logContent,
                isLoading = uiState.isLoadingLog,
                title = uiState.selectedLogFile!!.filename,
                onDismiss = { viewModel.hideLogDialog() },
                onBack = { viewModel.backToLogFileList() }
            )
        } else {
            // 非运行中的任务：显示历史日志文件列表
            TaskLogListDialog(
                task = uiState.logTask!!,
                logFiles = uiState.logFiles,
                isLoading = uiState.isLoadingLogFiles,
                onDismiss = { viewModel.hideLogDialog() },
                onSelectFile = { viewModel.selectLogFile(it) }
            )
        }
    }

    // 视图管理弹窗
    if (uiState.showViewManager && uiState.editingView == null) {
        ViewManagerDialog(
            views = uiState.viewTabs,
            onDismiss = { viewModel.hideViewManager() },
            onCreate = { viewModel.startCreateView() },
            onEdit = { viewModel.startEditView(it) },
            onDelete = { viewModel.showViewDeleteConfirm(it) }
        )
    }

    // 视图编辑弹窗
    if (uiState.editingView != null) {
        ViewEditDialog(
            view = uiState.editingView!!,
            subscriptions = uiState.subscriptions,
            onDismiss = { viewModel.cancelEditView() },
            onSave = { name, property, operation, value ->
                viewModel.saveView(name, property, operation, value)
            }
        )
    }

    // 视图删除确认
    if (uiState.showViewDeleteConfirm && uiState.deletingView != null) {
        ViewDeleteConfirmDialog(
            view = uiState.deletingView!!,
            onDismiss = { viewModel.hideViewDeleteConfirm() },
            onConfirm = { viewModel.deleteView(uiState.deletingView!!) }
        )
    }

    // 错误提示弹窗
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideErrorDialog() },
            title = { Text("错误") },
            text = { Text(uiState.errorDialogMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.hideErrorDialog() }) {
                    Text("确定")
                }
            }
        )
    }
}

// ===================== Task Card =====================

@Composable
fun TaskCard(
    task: Task,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewLog: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit
) {
    // 根据状态决定卡片边框颜色
    val borderColor = when {
        task.isRunning -> Color(0xFF43A047) // 运行中 - 绿色边框
        task.isDisabledFlag -> Color(0xFFE53935) // 已禁用 - 红色边框
        task.isQueued -> Color(0xFFFFA726) // 队列中 - 橙色边框
        else -> MaterialTheme.colorScheme.outlineVariant // 空闲 - 灰色边框
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = if (task.isRunning || task.isDisabledFlag) 1.5.dp else 0.5.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.isRunning) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isRunning) {
                Color(0xFF43A047).copy(alpha = 0.04f)
            } else if (task.isDisabledFlag) {
                Color(0xFFE53935).copy(alpha = 0.04f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // === 第一行：名称 + 状态标签 + 运行/停止按钮 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：复选框（批量模式）+ 名称
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBatchMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusTag(task = task)
                }

                // 右侧：运行/停止/启用按钮
                if (!isBatchMode) {
                    if (task.isRunning) {
                        // 运行中 → 停止按钮（红色）
                        FilledIconButton(
                            onClick = onStop,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFE53935).copy(alpha = 0.15f),
                                contentColor = Color(0xFFE53935)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "停止", modifier = Modifier.size(20.dp))
                        }
                    } else if (task.isDisabledFlag) {
                        // 已禁用 → 启用按钮（蓝色）
                        FilledIconButton(
                            onClick = onEnable,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF1976D2).copy(alpha = 0.15f),
                                contentColor = Color(0xFF1976D2)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "启用", modifier = Modifier.size(20.dp))
                        }
                    } else {
                        // 空闲中 → 运行按钮（绿色）
                        FilledIconButton(
                            onClick = onRun,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF43A047).copy(alpha = 0.15f),
                                contentColor = Color(0xFF43A047)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "运行", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // === 第二行：命令/脚本 ===
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // === 第三行：定时规则 + 上次运行信息 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.schedule,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.lastRunningTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.lastRunningTime}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // === 标签 ===
            if (!task.labels.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    task.labels.take(3).forEach { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.labels.size > 3) {
                        Text(
                            text = "+${task.labels.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // === 分隔线 ===
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // === 底部操作栏：禁用开关 + 日志/编辑/删除按钮 ===
            if (!isBatchMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：禁用/启用开关
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (task.isDisabledFlag) Icons.Default.NotInterested else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (task.isDisabledFlag) Color(0xFFE53935) else Color(0xFF43A047)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (task.isDisabledFlag) "已禁用" else "已启用",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.isDisabledFlag) Color(0xFFE53935) else Color(0xFF43A047)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = !task.isDisabledFlag,
                            onCheckedChange = { enabled ->
                                if (enabled) onEnable() else onDisable()
                            },
                            modifier = Modifier.height(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF43A047),
                                checkedTrackColor = Color(0xFF43A047).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFFE53935),
                                uncheckedTrackColor = Color(0xFFE53935).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // 右侧：日志 + 编辑 + 删除
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // 日志按钮
                        IconButton(onClick = onViewLog, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Article,
                                contentDescription = "日志",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // 编辑按钮
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // 删除按钮
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFE53935).copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===================== Status Tag =====================

@Composable
fun StatusTag(task: Task) {
    val (text, color) = when {
        task.isDisabledFlag -> "已禁用" to Color(0xFFE53935)
        task.isRunning -> "运行中" to Color(0xFF43A047)
        task.isQueued -> "队列中" to Color(0xFF9E9E9E)
        task.isIdle -> "空闲中" to Color(0xFF9E9E9E)
        else -> "未知" to Color(0xFF9E9E9E)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ===================== Dialogs (占位) =====================

@Composable
fun TaskCreateDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建任务") },
        text = { Text("功能完善中...") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
fun TaskEditDialog(task: Task, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑: ${task.name}") },
        text = { Text("功能完善中...") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

// ===================== Log Dialogs =====================

/**
 * 历史日志文件列表弹窗（非运行中的任务）
 */
@Composable
fun TaskLogListDialog(
    task: Task,
    logFiles: List<CronLogFile>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectFile: (CronLogFile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日志: ${task.name}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("加载中...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    logFiles.isEmpty() -> {
                        Text(
                            text = "暂无历史日志",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logFiles) { file ->
                                val dateStr = try {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(file.time.toLong()))
                                } catch (e: Exception) {
                                    file.filename
                                }
                                Card(
                                    onClick = { onSelectFile(file) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = file.filename,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "查看",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

/**
 * 日志内容详情弹窗（运行中实时日志 / 历史日志详情）
 */
@Composable
fun TaskLogDetailDialog(
    task: Task,
    logContent: String,
    isLoading: Boolean,
    title: String? = null,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = title ?: (if (task.isRunning) "实时日志: ${task.name}" else "日志: ${task.name}"),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("加载中...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    logContent.startsWith("加载失败") -> {
                        Text(
                            text = logContent,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    logContent.isBlank() -> {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        SelectionContainer {
                            Text(
                                text = logContent,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

// ===================== View Management Dialogs =====================

@Composable
fun ViewManagerDialog(
    views: List<ViewTab>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (ViewTab) -> Unit,
    onDelete: (ViewTab) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("管理视图标签")
                TextButton(onClick = onCreate) { Text("+ 新建") }
            }
        },
        text = {
            if (views.isEmpty()) {
                Text("暂无自定义视图", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    views.filter { it.type == 2 }.forEach { view ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(view.name, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(onClick = { onEdit(view) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDelete(view) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    val sysCount = views.count { it.type == 1 }
                    if (sysCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "系统内置: $sysCount 个（不可编辑）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun ViewEditDialog(
    view: ViewTab,
    subscriptions: List<com.qinglong.app.data.model.Subscription>,
    onDismiss: () -> Unit,
    onSave: (name: String, property: String, operation: String, value: String) -> Unit
) {
    var name by remember { mutableStateOf(view.name) }

    // 将原始 filter 翻译为中文选项
    val rawProperty = view.filters?.firstOrNull()?.property ?: "name"
    val rawOperation = view.filters?.firstOrNull()?.operation ?: "Reg"
    val rawValue = view.filters?.firstOrNull()?.value ?: ""

    // 初始选中项
    val initialFilterType = when {
        rawProperty == "status" && rawOperation == "In" -> "任务状态"
        rawProperty == "isDisabled" && rawOperation == "In" -> "启用状态"
        rawProperty == "sub_id" && rawOperation == "In" -> "所属订阅"
        rawProperty == "name" && rawOperation == "Reg" -> "包含关键词"
        rawProperty == "name" && rawOperation == "NotReg" -> "不包含关键词"
        else -> "包含关键词"
    }
    var selectedFilterType by remember { mutableStateOf(initialFilterType) }

    val filterTypes = listOf("任务状态", "启用状态", "所属订阅", "包含关键词", "不包含关键词")

    // 状态选项
    val statusOptions = listOf("运行中" to "0", "队列中" to "0.5", "空闲中" to "1", "已禁用" to "2")
    val enableOptions = listOf("已启用" to "0", "已禁用" to "1")
    var selectedStatusValue by remember {
        mutableStateOf(
            if (rawProperty in listOf("status", "isDisabled")) {
                statusOptions.find { it.second == rawValue }?.first
                    ?: enableOptions.find { it.second == rawValue }?.first
                    ?: ""
            } else ""
        )
    }
    var keywordValue by remember { mutableStateOf(if (rawProperty == "name") rawValue else "") }

    // 订阅下拉
    val subOptions = remember(subscriptions) {
        listOf(null to "不限") + subscriptions.map { it to it.name }
    }
    var selectedSubIndex by remember {
        mutableStateOf(
            if (rawProperty == "sub_id" && rawValue.isNotBlank()) {
                val idx = subscriptions.indexOfFirst { it.id.toString() == rawValue }
                if (idx >= 0) idx + 1 else 0
            } else 0
        )
    }
    var subMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (view.id == 0) "新建视图" else "编辑视图") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 视图名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("视图名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 筛选条件
                Text("筛选条件（可选）", style = MaterialTheme.typography.labelMedium)

                // 筛选方式选择芯片（分两行显示）
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filterTypes.take(3).forEach { ft ->
                            FilterChip(
                                selected = selectedFilterType == ft,
                                onClick = { selectedFilterType = ft },
                                label = { Text(ft, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filterTypes.drop(3).forEach { ft ->
                            FilterChip(
                                selected = selectedFilterType == ft,
                                onClick = { selectedFilterType = ft },
                                label = { Text(ft, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // 根据筛选类型显示不同的值选择
                when (selectedFilterType) {
                    "任务状态" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            statusOptions.forEach { (label, _) ->
                                FilterChip(
                                    selected = selectedStatusValue == label,
                                    onClick = { selectedStatusValue = label },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    "启用状态" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            enableOptions.forEach { (label, _) ->
                                FilterChip(
                                    selected = selectedStatusValue == label,
                                    onClick = { selectedStatusValue = label },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    "包含关键词", "不包含关键词" -> {
                        OutlinedTextField(
                            value = keywordValue,
                            onValueChange = { keywordValue = it },
                            label = { Text(if (selectedFilterType == "包含关键词") "包含哪些关键词" else "排除哪些关键词") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "所属订阅" -> {
                        Box {
                            OutlinedTextField(
                                value = subOptions.getOrNull(selectedSubIndex)?.second ?: "不限",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("选择订阅") },
                                trailingIcon = {
                                    IconButton(onClick = { subMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = subMenuExpanded,
                                onDismissRequest = { subMenuExpanded = false }
                            ) {
                                subOptions.forEachIndexed { index, (sub, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedSubIndex = index
                                            subMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val (prop, op, val_) = when (selectedFilterType) {
                    "任务状态" -> {
                        val sv = statusOptions.find { it.first == selectedStatusValue }
                        Triple("status", "In", sv?.second ?: "")
                    }
                    "启用状态" -> {
                        val ev = enableOptions.find { it.first == selectedStatusValue }
                        Triple("isDisabled", "In", ev?.second ?: "")
                    }
                    "包含关键词" -> Triple("name", "Reg", keywordValue)
                    "不包含关键词" -> Triple("name", "NotReg", keywordValue)
                    "所属订阅" -> {
                        val sub = subOptions.getOrNull(selectedSubIndex)?.first
                        val subId = if (sub != null) sub.id.toString() else ""
                        Triple("sub_id", "In", subId)
                    }
                    else -> Triple("name", "Reg", keywordValue)
                }
                onSave(name, prop, op, val_)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ViewDeleteConfirmDialog(view: ViewTab, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除视图「${view.name}」吗？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
