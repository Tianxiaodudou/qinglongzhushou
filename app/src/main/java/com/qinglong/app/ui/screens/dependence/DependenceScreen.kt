package com.qinglong.app.ui.screens.dependence

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.Dependence
import com.qinglong.app.data.model.dependenceStatusColor
import com.qinglong.app.data.model.dependenceStatusName
import com.qinglong.app.data.model.dependenceTypeName
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun DependenceScreen(
    viewModel: DependenceViewModel = hiltViewModel(),
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

    if (uiState.showSearch) {
        BackHandler { viewModel.toggleSearch() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar
            QingLongTopBar(
                title = "依赖管理",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建")
                    }
                    IconButton(onClick = { viewModel.loadDependencies() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(
                            if (uiState.showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索"
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
                    onSearch = { viewModel.loadDependencies() },
                    searchMode = 0,
                    onSearchModeChange = {}
                )
            }

            // 类型筛选胶囊
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("nodejs" to "Node.js", "python3" to "Python3", "linux" to "Linux")
                types.forEach { (type, label) ->
                    val isSelected = uiState.typeFilter == type
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
                            .clickable { viewModel.setTypeFilter(type) }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Content
            when {
                uiState.isLoading && uiState.dependencies.isEmpty() -> LoadingView()
                uiState.error != null && uiState.dependencies.isEmpty() -> ErrorView(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadDependencies() }
                )
                uiState.dependencies.isEmpty() -> EmptyView("暂无依赖")
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.dependencies.forEach { dep ->
                            DependenceCard(
                                dependence = dep,
                                onEdit = { viewModel.showEditDialog(dep) },
                                onDelete = { viewModel.requestDeleteDependence(dep) },
                                onReinstall = { viewModel.reinstallDependence(dep) },
                                onCancel = { viewModel.cancelDependence(dep) },
                                onLog = { viewModel.showLogDialog(dep) }
                            )
                        }
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 创建弹窗
    if (uiState.showCreateDialog) {
        DependenceDialog(
            dependence = null,
            onDismiss = { viewModel.hideCreateDialog() },
            onSave = { body -> viewModel.createDependencies(body) }
        )
    }

    // 编辑弹窗
    if (uiState.showEditDialog && uiState.editingDependence != null) {
        DependenceDialog(
            dependence = uiState.editingDependence,
            onDismiss = { viewModel.hideEditDialog() },
            onUpdate = { body -> viewModel.updateDependence(body) }
        )
    }

    // 删除确认
    if (uiState.showDeleteConfirm && uiState.deletingDependence != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("确认删除") },
            text = { Text("确定要删除依赖「${uiState.deletingDependence!!.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("取消")
                }
            }
        )
    }

    // 日志弹窗（使用通用实时日志组件）
    if (uiState.showLogDialog && uiState.logDependence != null) {
        TaskLogDetailDialog(
            title = "日志 - ${uiState.logDependence!!.name}",
            logContent = uiState.logContent,
            isLoading = uiState.isLoadingLog,
            isRunning = true,
            autoRefreshEnabled = uiState.autoRefreshLog,
            onRefresh = { viewModel.refreshLog() },
            onToggleAutoRefresh = { viewModel.toggleAutoRefresh(it) },
            onDismiss = { viewModel.hideLogDialog() }
        )
    }
}

@Composable
private fun DependenceCard(
    dependence: Dependence,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReinstall: () -> Unit,
    onCancel: () -> Unit,
    onLog: () -> Unit
) {
    val isRunning = dependence.status == 0 || dependence.status == 3 // 安装中或删除中
    val isQueued = dependence.status == 6
    val isCancelled = dependence.status == 7
    val isInstalled = dependence.status == 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 第一行：名称 + 状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dependence.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 状态标签
                val statusColor = when (dependence.status) {
                    0 -> Color(0xFF2196F3)   // 安装中 - 蓝色
                    1 -> Color(0xFF4CAF50)   // 已安装 - 绿色
                    2 -> Color(0xFFF44336)   // 安装失败 - 红色
                    3 -> Color(0xFF2196F3)   // 删除中 - 蓝色
                    4 -> Color(0xFF9E9E9E)   // 已删除 - 灰色
                    5 -> Color(0xFFF44336)   // 删除失败 - 红色
                    6 -> Color(0xFF9E9E9E)   // 队列中 - 灰色
                    7 -> Color(0xFF9E9E9E)   // 已取消 - 灰色
                    else -> Color(0xFF9E9E9E)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dependenceStatusName(dependence.status),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第二行：类型 + 备注
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dependenceTypeName(dependence.type),
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 备注
                if (!dependence.remark.isNullOrBlank()) {
                    Text(
                        text = dependence.remark,
                        fontSize = 13.sp,
                        color = Color(0xFF757575),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部操作栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日志按钮（非队列中、非已取消）
                if (!isQueued && !isCancelled) {
                    TextButton(
                        onClick = onLog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "日志",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("日志", fontSize = 12.sp)
                    }
                }

                // 取消安装（队列中、安装中、删除中）
                if (isRunning || isQueued) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "取消安装",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("取消", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                // 重新安装（非队列中、非安装中、非删除中）
                if (!isRunning && !isQueued) {
                    TextButton(
                        onClick = onReinstall,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重新安装",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("重装", fontSize = 12.sp)
                    }
                }

                // 编辑
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("编辑", fontSize = 12.sp)
                }

                // 删除（仅已安装状态）
                if (isInstalled) {
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DependenceDialog(
    dependence: Dependence?,
    onDismiss: () -> Unit,
    onSave: ((List<Map<String, Any>>) -> Unit)? = null,
    onUpdate: ((Map<String, Any>) -> Unit)? = null
) {
    val isCreate = dependence == null
    var name by remember { mutableStateOf(dependence?.name ?: "") }
    var type by remember { mutableIntStateOf(dependence?.type ?: 0) }
    var remark by remember { mutableStateOf(dependence?.remark ?: "") }
    var split by remember { mutableStateOf("0") }  // 0=不拆分, 1=拆分
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreate) "创建依赖" else "编辑依赖") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 依赖类型
                Text("依赖类型", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Node.js", 1 to "Python3", 2 to "Linux").forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }

                // 自动拆分（仅创建时）
                if (isCreate) {
                    Text("自动拆分", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("0" to "否", "1" to "是").forEach { (v, label) ->
                            FilterChip(
                                selected = split == v,
                                onClick = { split = v },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }
                }

                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    placeholder = { Text(if (isCreate) "多个依赖用换行或 & 分隔" else "依赖名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = !isCreate || split == "0",
                    minLines = if (isCreate && split == "1") 3 else 1
                )

                // 备注
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("备注") },
                    placeholder = { Text("可选备注") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) return@TextButton
                    loading = true
                    if (isCreate && onSave != null) {
                        val names = if (split == "1") {
                            name.split(Regex("[&\n]")).map { it.trim() }.filter { it.isNotBlank() }
                        } else {
                            listOf(name.trim())
                        }
                        val body = names.map { n ->
                            mapOf<String, Any>(
                                "name" to n,
                                "type" to type,
                                "remark" to remark
                            )
                        }
                        onSave(body)
                    } else if (!isCreate && onUpdate != null) {
                        val body = mapOf<String, Any>(
                            "id" to dependence!!.id,
                            "name" to name.trim(),
                            "type" to type,
                            "remark" to remark
                        )
                        onUpdate(body)
                    }
                    loading = false
                },
                enabled = name.isNotBlank()
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isCreate) "创建" else "保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
