package com.qinglong.app.ui.screens.task

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.CronLogFile
import com.qinglong.app.data.model.Task
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示 Toast 消息
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 拦截系统返回键：搜索状态时关闭搜索，多选状态时退出多选
    if (uiState.showSearch) {
        BackHandler {
            viewModel.toggleSearch()
        }
    }
    if (uiState.isBatchMode) {
        BackHandler {
            viewModel.toggleBatchMode()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // TopAppBar: [☰] + 定时任务 + [新建] + [刷新] + [搜索] + [批量]
        QingLongTopBar(
            title = "定时任务",
            onMenuClick = onMenuClick,
            actions = {
                // 新建任务按钮
                IconButton(onClick = { viewModel.showCreateDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "新建")
                }
                // 刷新按钮
                IconButton(onClick = { viewModel.refreshTasks() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
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
                onSearch = { viewModel.searchAllTasks(uiState.searchQuery) },
                searchMode = uiState.searchMode,
                onSearchModeChange = { viewModel.setSearchMode(it) }
            )
        }

        // Batch action bar
        AnimatedVisibility(visible = uiState.isBatchMode && uiState.selectedTaskIds.isNotEmpty()) {
            BatchActionBar(
                selectedCount = uiState.selectedTaskIds.size,
                onRunAll = { viewModel.batchRun() },
                onStopAll = { viewModel.batchStop() },
                onEnableAll = { viewModel.batchEnable() },
                onDisableAll = { viewModel.batchDisable() },
                onPinAll = { viewModel.batchPin() },
                onUnpinAll = { viewModel.batchUnpin() },
                onDeleteAll = { viewModel.batchDelete() },
                onSelectAll = { viewModel.selectAllTasks() },
                onInvertSelection = { viewModel.invertTaskSelection() }
            )
        }

        // Tab Row — 胶囊样式视图标签
        if (uiState.viewTabs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.viewTabs.forEachIndexed { index, tab ->
                    val isSelected = uiState.selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color.Black
                                else Color.LightGray.copy(alpha = 0.4f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { viewModel.selectTab(index) }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                Box(modifier = Modifier.fillMaxSize()) {
                    TaskContent(
                        uiState = uiState,
                        isSearching = isSearching,
                        viewModel = viewModel
                    )
                    // 加载覆盖层（切换标签/刷新时显示）
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation()
                        }
                    }
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
            onConfirm = { name, command, schedule, labels, allowMultipleInstances, logName, taskBefore, taskAfter ->
                viewModel.createNewTask(
                    name = name,
                    command = command,
                    schedule = schedule,
                    labels = labels,
                    allowMultipleInstances = allowMultipleInstances,
                    logName = logName,
                    taskBefore = taskBefore,
                    taskAfter = taskAfter
                )
            }
        )
    }

    // 编辑任务弹窗
    if (uiState.showEditDialog && uiState.editingTask != null) {
        TaskEditDialog(
            task = uiState.editingTask!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, command, schedule, labels, allowMultipleInstances, logName, taskBefore, taskAfter ->
                viewModel.saveTask(
                    id = uiState.editingTask!!.id,
                    name = name,
                    command = command,
                    schedule = schedule,
                    labels = labels,
                    allowMultipleInstances = allowMultipleInstances,
                    logName = logName,
                    taskBefore = taskBefore,
                    taskAfter = taskAfter
                )
            }
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
                title = "实时日志: ${uiState.logTask!!.name}",
                logContent = uiState.logContent,
                isLoading = uiState.isLoadingLog,
                isRunning = true,
                autoRefreshEnabled = uiState.autoRefreshEnabled,
                onRefresh = { viewModel.manualRefreshLog() },
                onToggleAutoRefresh = { viewModel.toggleAutoRefresh() },
                onDismiss = { viewModel.hideLogDialog() }
            )
        } else if (uiState.isShowingLogDetail && uiState.selectedLogFile != null) {
            // 查看某条历史日志详情
            TaskLogDetailDialog(
                title = uiState.selectedLogFile!!.filename,
                logContent = uiState.logContent,
                isLoading = uiState.isLoadingLog,
                onDismiss = { viewModel.hideLogDialog() },
                onBack = { viewModel.backToLogFileList() }
            )
        } else {
            // 非运行中的任务：显示历史日志文件列表
            TaskLogListDialog(
                task = uiState.logTask!!,
                logFiles = uiState.logFiles,
                isLoading = uiState.isLoadingLogFiles,
                isBatchMode = uiState.isLogBatchMode,
                selectedFiles = uiState.selectedLogFiles,
                onDismiss = { viewModel.hideLogDialog() },
                onSelectFile = { viewModel.selectLogFile(it) },
                onDeleteFile = { viewModel.showDeleteLogConfirm(it) },
                onToggleBatchMode = { viewModel.toggleLogBatchMode() },
                onToggleFile = { viewModel.toggleLogFileSelection(it) },
                onSelectAll = { viewModel.selectAllLogFiles() },
                onInvertSelection = { viewModel.invertLogFileSelection() },
                onDeleteSelected = { viewModel.deleteSelectedLogFiles() }
            )
        }
    }

    // 删除日志确认弹窗
    if (uiState.showDeleteLogConfirm && uiState.deletingLogFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteLogConfirm() },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除以下日志文件吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.deletingLogFile!!.filename,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteLogFile() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteLogConfirm() }) {
                    Text("取消")
                }
            }
        )
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

    // 错误提示弹窗（支持长按复制错误信息）
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideErrorDialog() },
            title = { Text("错误") },
            text = {
                SelectionContainer {
                    Text(uiState.errorDialogMessage)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideErrorDialog() }) {
                    Text("确定")
                }
            }
        )
    }
    // SnackbarHost 放在 Box 底部
    SnackbarHost(hostState = snackbarHostState)
}

// ===================== Task Content（提取自 else 分支） =====================

@Composable
private fun TaskContent(
    uiState: com.qinglong.app.ui.screens.task.TaskUiState,
    isSearching: Boolean,
    viewModel: com.qinglong.app.ui.screens.task.TaskViewModel
) {
    // 分页
    val pageSize = uiState.pageSize
    val totalTasks = uiState.tasks.size
    val totalPages = (totalTasks + pageSize - 1) / pageSize
    var currentPage by remember { mutableIntStateOf(0) }

    // 切换标签时重置页码
    LaunchedEffect(uiState.selectedTabIndex) {
        currentPage = 0
    }

    // 当前页任务列表
    val pageStart = currentPage * pageSize
    val pageEnd = (pageStart + pageSize).coerceAtMost(totalTasks)
    val pageTasks = remember(currentPage, uiState.tasks) {
        uiState.tasks.subList(pageStart, pageEnd)
    }

    // 页码指示器 + 任务列表
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 操作栏：搜索提示 / 上一页 / 页码 / 下一页 / 新建（均匀分布）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                // 搜索模式：搜索提示 + 上一页 + 页码 + 下一页 + 新建
                Text(
                    text = "共 ${uiState.tasks.size} 个结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 上一页按钮
            TextButton(
                onClick = {
                    if (currentPage > 0) currentPage--
                },
                enabled = currentPage > 0,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                @Suppress("DEPRECATION")
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "上一页",
                    modifier = Modifier.size(20.dp)
                )
                Text("上一页", style = MaterialTheme.typography.labelSmall)
            }

            // 页码指示器
            Text(
                text = "${currentPage + 1} / $totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 下一页按钮
            TextButton(
                onClick = {
                    if (currentPage < totalPages - 1) currentPage++
                },
                enabled = currentPage < totalPages - 1,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text("下一页", style = MaterialTheme.typography.labelSmall)
                @Suppress("DEPRECATION")
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "下一页",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 任务列表
        if (pageTasks.isEmpty()) {
            EmptyView("当前页无任务")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pageTasks.forEach { task ->
                    val subName = task.sub_id?.let { subId ->
                        uiState.subscriptions.find { it.id == subId }?.name
                    }
                    TaskCard(
                        task = task,
                        isBatchMode = uiState.isBatchMode,
                        isSelected = task.id in uiState.selectedTaskIds,
                        subName = subName,
                        nextRunTimeText = uiState.nextRunTimeCache[task.id],
                        onToggleSelect = { viewModel.toggleTaskSelection(task.id) },
                        onRun = { viewModel.runTask(task.id) },
                        onStop = { viewModel.stopTask(task.id) },
                        onEdit = { viewModel.showEditDialog(task) },
                        onDelete = { viewModel.showDeleteConfirm(task) },
                        onViewLog = { viewModel.showLogDialog(task) },
                        onEnable = { viewModel.enableTask(task.id) },
                        onDisable = { viewModel.disableTask(task.id) },
                        onPin = { viewModel.pinTask(task.id) },
                        onUnpin = { viewModel.unpinTask(task.id) }
                    )
                }
            }
        }
    }
}

// ===================== Task Card =====================

@Composable
fun TaskCard(
    task: Task,
    isBatchMode: Boolean,
    isSelected: Boolean,
    subName: String? = null,
    nextRunTimeText: String? = null,
    onToggleSelect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewLog: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit
) {
    // === 直接从 task 读取字段 ===
    val taskName = task.name
    val isRunning = task.isRunning
    val isDisabled = task.isDisabledFlag
    val isQueued = task.isQueued
    val isPinned = task.isPinned == 1
    val lastRunTime = task.lastRunTime

    // === 一次性计算配置 ===
    val cfg = remember(isRunning, isDisabled, isQueued, isPinned, lastRunTime, nextRunTimeText) {
        val btnIcon = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow
        val btnDesc = when { isRunning -> "停止"; isDisabled -> "启用"; else -> "运行" }
        val btnColor = when { isRunning -> cRed; isDisabled -> cBlue; else -> cGreen }
        val tagText = when { isDisabled -> "已禁用"; isRunning -> "运行中"; isQueued -> "队列中"; else -> "空闲中" }
        val tagColor = when { isDisabled -> cRed; isRunning -> cGreen; isQueued -> cGray; else -> cGray }
        CardCfg(tagText, tagColor, btnIcon, btnDesc, btnColor)
    }

    // === 信息框内容 ===
    val infoRows = remember(lastRunTime, nextRunTimeText, subName) {
        buildList {
            val lastRun = if (lastRunTime != null) "上次运行: ${com.qinglong.app.util.CronParser.formatTimestamp(lastRunTime)}" else "上次运行: 暂无"
            val nextRun = nextRunTimeText?.let { "预计下次: $it" } ?: "预计下次: 无法计算"
            add(InfoRow(Icons.Default.History, "上次运行", lastRun, color = cBlue))
            add(InfoRow(Icons.Default.Schedule, "预计下次", nextRun, color = cGreen))
            if (subName != null) {
                add(InfoRow(Icons.Default.Folder, "所属订阅", subName, color = cOrange))
            }
        }
    }

    CommonCard(
        name = taskName,
        cfg = cfg,
        isBatchMode = isBatchMode,
        isSelected = isSelected,
        isDisabled = isDisabled,
        infoRows = infoRows,
        onToggleSelect = onToggleSelect,
        onToggleEnable = { if (isDisabled) onEnable() else onDisable() },
        enableSwitchStyle = false, // 圆角矩形按钮样式
        bottomButtons = {
            // 运行/停止
            RunStopButton(
                isRunning = isRunning,
                btnIcon = cfg.btnIcon,
                btnDesc = cfg.btnDesc,
                btnColor = cfg.btnColor,
                onRun = onRun,
                onStop = onStop
            )
            // 日志
            LogButton(onViewLog = onViewLog, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            // 置顶
            PinButton(
                isPinned = isPinned,
                onPin = onPin,
                onUnpin = onUnpin
            )
            // 编辑
            EditButton(onClick = onEdit)
            // 删除
            DeleteButton(onClick = onDelete)
        }
    )
}

// ===================== Log Dialogs =====================

/**
 * 历史日志文件列表弹窗（非运行中的任务）
 * 支持多选批量删除
 */

// ===================== 日志按钮组件 =====================

/**
 * 任务卡片底部的日志按钮
 */
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
                                subOptions.forEachIndexed { index, (_, label) ->
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
