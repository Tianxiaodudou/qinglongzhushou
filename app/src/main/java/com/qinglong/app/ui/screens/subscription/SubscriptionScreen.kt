package com.qinglong.app.ui.screens.subscription

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.Subscription
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 拦截系统返回键：搜索状态时关闭搜索，多选状态时退出多选
    if (uiState.showSearch) {
        BackHandler { viewModel.toggleSearch() }
    }
    if (uiState.isBatchMode) {
        BackHandler { viewModel.toggleBatchMode() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar: [☰] + 订阅管理 + [新建] + [刷新] + [搜索] + [批量]
            QingLongTopBar(
                title = if (uiState.isBatchMode) "批量操作 (${uiState.selectedIds.size})" else "订阅管理",
                onMenuClick = onMenuClick,
                actions = {
                    if (uiState.isBatchMode) {
                        IconButton(onClick = { viewModel.toggleBatchMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消批量")
                        }
                    } else {
                        // 新建订阅按钮
                        IconButton(onClick = { viewModel.showCreateDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "新建")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(
                                if (uiState.showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        IconButton(onClick = { viewModel.toggleBatchMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                        }
                    }
                }
            )

            // 搜索栏
            AnimatedVisibility(visible = uiState.showSearch) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClear = { viewModel.updateSearchQuery("") },
                    onSearch = { viewModel.loadSubscriptions() },
                    searchMode = 0, // 订阅只有名称搜索
                    onSearchModeChange = {}
                )
            }

            // 批量操作栏
            AnimatedVisibility(visible = uiState.isBatchMode && uiState.selectedIds.isNotEmpty()) {
                BatchActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onRunAll = { viewModel.batchRun() },
                    onStopAll = { viewModel.batchStop() },
                    onEnableAll = { viewModel.batchEnable() },
                    onDisableAll = { viewModel.batchDisable() },
                    onPinAll = {},
                    onUnpinAll = {},
                    onDeleteAll = { viewModel.batchDelete() },
                    onSelectAll = { viewModel.selectAll() },
                    onInvertSelection = { viewModel.invertSelection() }
                )
            }

            // 内容区域
            when {
                uiState.isLoading && uiState.subscriptions.isEmpty() -> LoadingView()
                uiState.error != null && uiState.subscriptions.isEmpty() -> ErrorView(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadSubscriptions() }
                )
                uiState.subscriptions.isEmpty() && uiState.searchQuery.isNotBlank() ->
                    EmptyView("未搜索到「${uiState.searchQuery}」相关结果")
                uiState.subscriptions.isEmpty() -> EmptyView("暂无订阅")
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SubscriptionContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        // 加载覆盖层
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

    // ===================== 弹窗 =====================

    // 创建订阅弹窗
    if (uiState.showCreateDialog) {
        SubscriptionDialog(
            isCreate = true,
            onDismiss = { viewModel.hideCreateDialog() },
            onSave = { body -> viewModel.createSubscription(body) }
        )
    }

    // 编辑订阅弹窗
    if (uiState.showEditDialog && uiState.editingSubscription != null) {
        SubscriptionDialog(
            isCreate = false,
            subscription = uiState.editingSubscription!!,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { body -> viewModel.updateSubscription(uiState.editingSubscription!!.id, body) }
        )
    }

    // 删除确认弹窗
    if (uiState.showDeleteConfirm && uiState.deletingSubscription != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除订阅「${uiState.deletingSubscription!!.name}」吗？")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.setDeleteForce(!uiState.deleteForce) }
                    ) {
                        Checkbox(
                            checked = uiState.deleteForce,
                            onCheckedChange = { viewModel.setDeleteForce(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "同时删除关联任务和脚本",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSubscription(uiState.deletingSubscription!!.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) { Text("取消") }
            }
        )
    }

    // 日志弹窗 - 三态逻辑
    if (uiState.showLogDialog && uiState.logSubscription != null) {
        val sub = uiState.logSubscription!!
        if (sub.isRunning) {
            // 运行中 → 实时日志（带自动刷新）
            TaskLogDetailDialog(
                title = "实时日志: ${sub.name}",
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
            // 非运行中 → 历史日志文件列表
            TaskLogListDialog(
                task = sub.toTaskLike(),
                logFiles = uiState.logFiles,
                isLoading = uiState.isLoadingLogFiles,
                isBatchMode = uiState.isLogBatchMode,
                selectedFiles = uiState.selectedLogFiles,
                onDismiss = { viewModel.hideLogDialog() },
                onSelectFile = { viewModel.selectLogFile(it) },
                onDeleteFile = { viewModel.showDeleteLogConfirm(it) },
                onToggleBatchMode = { viewModel.toggleLogBatchMode() },
                onToggleFile = { viewModel.toggleLogSelection(it.filename) },
                onSelectAll = { viewModel.selectAllLogFiles() },
                onInvertSelection = { viewModel.deselectAllLogFiles() },
                onDeleteSelected = { viewModel.batchDeleteLogFiles() }
            )
        }
    }

    // 删除日志确认弹窗
    if (uiState.showDeleteLogConfirm && uiState.deletingLogFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteLogConfirm() },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除日志「${uiState.deletingLogFile!!.filename}」吗？此操作不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteLogFile() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteLogConfirm() }) { Text("取消") }
            }
        )
    }
}

// ===================== 订阅内容区域 =====================

@Composable
private fun SubscriptionContent(
    uiState: SubscriptionUiState,
    viewModel: SubscriptionViewModel
) {
    // 过滤
    val filteredSubscriptions = if (uiState.searchQuery.isNotBlank()) {
        uiState.subscriptions.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
    } else {
        uiState.subscriptions
    }

    // 分页
    val pageSize = uiState.pageSize
    val totalItems = filteredSubscriptions.size
    val totalPages = (totalItems + pageSize - 1) / pageSize
    var currentPage by remember { mutableIntStateOf(0) }

    // 切换搜索时重置页码
    LaunchedEffect(uiState.searchQuery) {
        currentPage = 0
    }

    // 当前页订阅列表
    val pageStart = currentPage * pageSize
    val pageEnd = (pageStart + pageSize).coerceAtMost(totalItems)
    val pageSubscriptions = remember(currentPage, filteredSubscriptions) {
        if (filteredSubscriptions.isEmpty()) emptyList()
        else filteredSubscriptions.subList(pageStart, pageEnd)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 页码指示器 + 操作栏
        if (filteredSubscriptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.searchQuery.isNotBlank()) {
                    Text(
                        text = "共 ${filteredSubscriptions.size} 个结果",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 上一页
                TextButton(
                    onClick = { if (currentPage > 0) currentPage-- },
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
                // 页码
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 下一页
                TextButton(
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
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
        }

        // 订阅卡片列表
        if (pageSubscriptions.isEmpty()) {
            EmptyView("当前无订阅")
        } else {
            pageSubscriptions.forEach { subscription ->
                SubscriptionCard(
                    subscription = subscription,
                    isSelected = subscription.id in uiState.selectedIds,
                    isBatchMode = uiState.isBatchMode,
                    onClick = {
                        if (uiState.isBatchMode) {
                            viewModel.toggleSelection(subscription.id)
                        } else {
                            viewModel.showEditDialog(subscription)
                        }
                    },
                    onRun = { viewModel.runSubscription(subscription.id) },
                    onStop = { viewModel.stopSubscription(subscription.id) },
                    onEdit = { viewModel.showEditDialog(subscription) },
                    onDelete = { viewModel.showDeleteConfirm(subscription) },
                    onViewLog = { viewModel.showLogDialog(subscription) },
                    onToggleEnable = {
                        if (subscription.isDisabled == 1) {
                            viewModel.enableSubscription(subscription.id)
                        } else {
                            viewModel.disableSubscription(subscription.id)
                        }
                    }
                )
            }
        }
    }
}

// ===================== SubscriptionCard =====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionCard(
    subscription: Subscription,
    isSelected: Boolean,
    isBatchMode: Boolean,
    onClick: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewLog: () -> Unit,
    onToggleEnable: () -> Unit,
    onPin: () -> Unit = {},
    onUnpin: () -> Unit = {}
) {
    val subName = subscription.name
    val subUrl = subscription.url
    val subType = subscription.type
    val subSchedule = subscription.schedule
    val subInterval = subscription.interval_schedule
    val subBranch = subscription.branch
    val isDisabled = subscription.isDisabled == 1
    val isRunning = subscription.isRunning

    // 一次性计算配置
    val cfg = remember(isRunning, isDisabled) {
        val tagText = when { isDisabled -> "已禁用"; isRunning -> "运行中"; else -> "空闲" }
        val tagColor = when { isDisabled -> cRed; isRunning -> cGreen; else -> cGray }
        val btnIcon = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow
        val btnDesc = when { isRunning -> "停止"; isDisabled -> "启用"; else -> "运行" }
        val btnColor = when { isRunning -> cRed; isDisabled -> cBlue; else -> cGreen }
        CardCfg(tagText, tagColor, btnIcon, btnDesc, btnColor)
    }

    // 信息框内容
    // 链接单独一行（可能很长），类型/定时规则/分支横排在一行
    val linkRow = remember(subUrl) {
        InfoRow(Icons.Default.Link, "链接", if (subUrl.isNotBlank()) subUrl else "暂无", color = cBlue)
    }
    val infoRows = remember(subType, subSchedule, subInterval, subBranch) {
        buildList {
            if (subType.isNotBlank()) {
                // 类型英文转中文
                val typeText = when (subType) {
                    "public-repo" -> "公开仓库"
                    "private-repo" -> "私有仓库"
                    "single-file" -> "单文件"
                    else -> subType
                }
                add(InfoRow(Icons.Default.Category, "类型", typeText, color = cOrange))
            }
            // 优先显示 cron 表达式，其次显示间隔调度
            val scheduleText = when {
                !subSchedule.isNullOrBlank() -> subSchedule
                subInterval != null -> {
                    val type = subInterval["type"] as? String
                    val value = subInterval["value"] as? Number
                    when {
                        type == "days" && value != null -> "每${value.toLong()}天"
                        type == "hours" && value != null -> "每${value.toLong()}小时"
                        type == "minutes" && value != null -> "每${value.toLong()}分钟"
                        else -> "暂无"
                    }
                }
                else -> "暂无"
            }
            add(InfoRow(Icons.Default.Schedule, "定时规则", scheduleText, color = cGreen))
            add(InfoRow(Icons.Default.Code, "分支", if (!subBranch.isNullOrBlank()) subBranch else "暂无", color = cRed))
        }
    }

    CommonCard(
        name = subName,
        cfg = cfg,
        isBatchMode = isBatchMode,
        isSelected = isSelected,
        isDisabled = isDisabled,
        infoRowsHeader = linkRow,
        infoRows = infoRows,
        infoRowsHorizontal = true, // 类型/定时规则/分支横排
        onToggleSelect = onClick,
        onToggleEnable = onToggleEnable,
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
                isPinned = false,
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
