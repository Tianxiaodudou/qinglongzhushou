package com.qinglong.app.ui.screens.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.ui.components.*
import com.qinglong.app.ui.theme.QingLongGreen

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
                IconButton(onClick = { /* TODO: search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                IconButton(onClick = { /* TODO: more options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }
        )

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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskCard(
                                name = task.name,
                                schedule = task.schedule,
                                isRunning = task.isRunning,
                                isDisabled = task.isDisabled,
                                lastRunTime = task.lastRunTime?.let {
                                    java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(it))
                                },
                                execStatus = when (task.lastExecCode) {
                                    0 -> com.qinglong.app.ui.components.ExecStatus.SUCCESS
                                    null -> null
                                    else -> com.qinglong.app.ui.components.ExecStatus.FAILED
                                },
                                onRun = { viewModel.runTask(task.id) },
                                onStop = { viewModel.stopTask(task.id) }
                            )
                        }
                    }
                }
            }

            // FAB: Add new task
            FloatingActionButton(
                onClick = { /* TODO: add task */ },
                containerColor = QingLongGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建任务")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        QingLongTopBar(
            title = title,
            onMenuClick = { }
        )
        EmptyView(message = "$title （待实现）")
    }
}
