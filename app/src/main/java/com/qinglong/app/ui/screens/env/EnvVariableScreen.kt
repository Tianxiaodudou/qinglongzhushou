package com.qinglong.app.ui.screens.env

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.EnvVariable
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnvVariableScreen(
    viewModel: EnvVariableViewModel = hiltViewModel(),
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

    // 过滤后的列表
    val filteredVariables = remember(uiState.envVariables, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.envVariables
        } else {
            uiState.envVariables.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏
            QingLongTopBar(
                title = "环境变量",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { viewModel.toggleBatchMode() }) {
                        Icon(
                            if (uiState.isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = if (uiState.isBatchMode) "退出批量" else "批量操作"
                        )
                    }
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
                    }
                }
            )

            // 搜索栏
            AnimatedVisibility(visible = uiState.showSearch) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClear = { viewModel.toggleSearch() }
                )
            }

            // 批量操作栏
            if (uiState.isBatchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            if (uiState.selectedIds.size == filteredVariables.size) {
                                viewModel.deselectAll()
                            } else {
                                viewModel.selectAll()
                            }
                        },
                        label = {
                            Text(
                                if (uiState.selectedIds.size == filteredVariables.size && filteredVariables.isNotEmpty())
                                    "反选" else "全选",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                    Text(
                        "已选 ${uiState.selectedIds.size} 项",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BatchActionBar(
                    selectedCount = uiState.selectedIds.size,
                    onRunAll = {},
                    onStopAll = {},
                    onEnableAll = { viewModel.batchEnable() },
                    onDisableAll = { viewModel.batchDisable() },
                    onPinAll = { viewModel.batchPin() },
                    onUnpinAll = { viewModel.batchUnpin() },
                    onDeleteAll = { viewModel.batchDelete() },
                    onSelectAll = { viewModel.selectAll() },
                    onInvertSelection = { viewModel.invertSelection() }
                )
            }

            // 内容区域
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingView()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorView(
                            message = uiState.error ?: "未知错误",
                            onRetry = { viewModel.loadEnvVariables() }
                        )
                    }
                }
                filteredVariables.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyView(message = if (uiState.searchQuery.isNotBlank()) "未找到匹配的环境变量" else "暂无环境变量")
                    }
                }
                else -> {
                    // 分页
                    val pageSize = uiState.pageSize
                    val totalItems = filteredVariables.size
                    val totalPages = (totalItems + pageSize - 1) / pageSize
                    var currentPage by remember { mutableIntStateOf(0) }

                    // 切换搜索时重置页码
                    LaunchedEffect(uiState.searchQuery) {
                        currentPage = 0
                    }

                    // 当前页变量列表
                    val pageStart = currentPage * pageSize
                    val pageEnd = (pageStart + pageSize).coerceAtMost(totalItems)
                    val pageVariables = remember(currentPage, filteredVariables) {
                        if (filteredVariables.isEmpty()) emptyList()
                        else filteredVariables.subList(pageStart, pageEnd)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 页码指示器 + 操作栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 0.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.searchQuery.isNotBlank()) {
                                Text(
                                    text = "共 ${filteredVariables.size} 个结果",
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

                        pageVariables.forEach { variable ->
                            EnvCard(
                                variable = variable,
                                isBatchMode = uiState.isBatchMode,
                                isSelected = variable.id in uiState.selectedIds,
                                onToggleSelect = { viewModel.toggleSelection(variable.id) },
                                onEdit = { viewModel.showEditDialog(variable) },
                                onDelete = { viewModel.showDeleteConfirm(variable) },
                                onToggleEnable = { enable -> viewModel.toggleStatus(variable.id, enable) },
                                onPin = { viewModel.pinEnvVariable(variable.id) },
                                onUnpin = { viewModel.unpinEnvVariable(variable.id) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }

    // 创建弹窗
    if (uiState.showCreateDialog) {
        EnvDialog(
            title = "新建环境变量",
            initialName = "",
            initialValue = "",
            initialRemarks = "",
            onConfirm = { name, value, remarks ->
                viewModel.createEnvVariable(name, value, remarks)
            },
            onDismiss = { viewModel.hideCreateDialog() }
        )
    }

    // 编辑弹窗
    if (uiState.showEditDialog && uiState.editingVariable != null) {
        EnvDialog(
            title = "编辑环境变量",
            initialName = uiState.editingVariable!!.name,
            initialValue = uiState.editingVariable!!.value,
            initialRemarks = uiState.editingVariable!!.remarks ?: "",
            onConfirm = { name, value, remarks ->
                viewModel.saveEdit(uiState.editingVariable!!.id, name, value, remarks)
            },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认弹窗
    if (uiState.showDeleteConfirm && uiState.deletingVariable != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            title = { Text("确认删除") },
            text = { Text("确定要删除环境变量「${uiState.deletingVariable!!.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteEnvVariable(uiState.deletingVariable!!.id) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirm() }) {
                    Text("取消")
                }
            }
        )
    }
}

// ===================== 环境变量卡片 =====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnvCard(
    variable: EnvVariable,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnable: (Boolean) -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit
) {
    val isEnabled = variable.status == 0
    val isPinned = variable.isPinned == 1

    val cfg = CardCfg(
        tagText = if (isEnabled) "已启用" else "已禁用",
        tagColor = if (isEnabled) StatusRunning else StatusFailed,
        btnIcon = Icons.Default.PlayArrow,
        btnDesc = if (isEnabled) "已启用" else "已禁用",
        btnColor = if (isEnabled) StatusRunning else StatusFailed
    )

    CommonCard(
        name = variable.name,
        cfg = cfg,
        isBatchMode = isBatchMode,
        isSelected = isSelected,
        isDisabled = !isEnabled,
        infoRows = listOf(
            InfoRow(
                icon = Icons.Default.Code,
                label = "数值",
                value = variable.value,
                color = cBlue
            ),
            InfoRow(
                icon = Icons.Default.Info,
                label = "备注",
                value = variable.remarks?.takeIf { it.isNotBlank() } ?: "暂无",
                color = cOrange,
                maxLines = Int.MAX_VALUE
            )
        ),
        onToggleSelect = onToggleSelect,
        onToggleEnable = { onToggleEnable(!isEnabled) },
        enableSwitchStyle = false,
        bottomButtons = {
            PinButton(
                isPinned = isPinned,
                onPin = onPin,
                onUnpin = onUnpin
            )
            EditButton(onClick = onEdit)
            DeleteButton(onClick = onDelete)
        }
    )
}

// ===================== 创建/编辑弹窗 =====================

@Composable
private fun EnvDialog(
    title: String,
    initialName: String,
    initialValue: String,
    initialRemarks: String,
    onConfirm: (name: String, value: String, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var value by remember { mutableStateOf(initialValue) }
    var remarks by remember { mutableStateOf(initialRemarks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    placeholder = { Text("例如: JD_COOKIE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("数值") },
                    placeholder = { Text("请输入环境变量的值") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("备注") },
                    placeholder = { Text("可选，添加备注说明") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, value, remarks) },
                enabled = name.isNotBlank() && value.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
