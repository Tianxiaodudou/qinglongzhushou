package com.qinglong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ===================== 硬编码颜色常量 =====================
val cGreen = Color(0xFF43A047)
val cRed = Color(0xFFE53935)
val cBlue = Color(0xFF1976D2)
val cOrange = Color(0xFFFFA726)
val cGray = Color(0xFF9E9E9E)

// 浅色模式颜色（保留供外部引用，但 CommonCard 内部使用 MaterialTheme）
val cSurfaceLight = Color(0xFFFFFFFF)
val cOutlineVariantLight = Color(0xFFCAC4D0)
val cOnSurfaceVariantLight = Color(0xFF49454F)
val cSurfaceVariantLight = Color(0xFFE7E0EC)

// 深色模式颜色
val cSurfaceDark = Color(0xFF1E1E1E)
val cOutlineVariantDark = Color(0xFF444444)
val cOnSurfaceVariantDark = Color(0xFFB0B0B0)
val cSurfaceVariantDark = Color(0xFF2A2A2A)

// ===================== 卡片配置数据类 =====================

/**
 * 卡片配置，一次性计算所有状态相关的值
 */
data class CardCfg(
    val tagText: String,
    val tagColor: Color,
    val btnIcon: ImageVector,
    val btnDesc: String,
    val btnColor: Color
)

/**
 * 信息框条目：图标 + 名称 + 值
 * @param color 条目颜色（图标和文字使用），null 则使用默认色 cOnSurfaceVariant
 * @param maxLines 最大行数，默认 1（单行截断），传 Int.MAX_VALUE 表示多行完整显示
 */
data class InfoRow(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val color: Color? = null,
    val maxLines: Int = 1
)

// ===================== 公共卡片组件 =====================

/**
 * 通用卡片组件，供任务卡片、订阅卡片、环境变量卡片使用
 *
 * @param name 卡片名称
 * @param cfg 卡片配置（状态标签、按钮等）
 * @param isBatchMode 是否处于多选模式
 * @param isSelected 是否被选中
 * @param isDisabled 是否禁用
 * @param infoRows 信息框内容列表（图标+名称+值）
 * @param infoRowsHeader 信息框顶部额外行（如链接），显示在 infoRows 上方
 * @param infoRowsHorizontal 信息框是否横排显示（true=所有条目在一行，false=每个条目一行）
 * @param bottomButtons 底部操作按钮列表
 * @param onClick 点击卡片
 * @param onLongClick 长按卡片
 * @param onToggleSelect 切换选中
 * @param onToggleEnable 切换启用/禁用
 * @param enableSwitchStyle 禁用开关样式：true=滑动开关，false=圆角矩形按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonCard(
    name: String,
    cfg: CardCfg,
    isBatchMode: Boolean = false,
    isSelected: Boolean = false,
    isDisabled: Boolean = false,
    infoRows: List<InfoRow> = emptyList(),
    infoRowsHeader: InfoRow? = null,
    infoRowsHorizontal: Boolean = false,
    bottomButtons: @Composable RowScope.() -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onToggleEnable: () -> Unit = {},
    enableSwitchStyle: Boolean = true
) {
    val cSurface = MaterialTheme.colorScheme.surface
    val cOutlineVariant = MaterialTheme.colorScheme.outlineVariant
    val cOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val cSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(androidx.compose.foundation.BorderStroke(0.5.dp, cOutlineVariant), RoundedCornerShape(12.dp))
            .background(cSurface)
            .then(
                if (isBatchMode) Modifier.clickable { onToggleSelect() }
                else Modifier
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // === 第一行：复选框（批量模式）+ 名称 + 状态标签 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBatchMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // 名称
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // 状态标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cfg.tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cfg.tagText,
                        fontSize = 11.sp,
                        color = cfg.tagColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // === 信息框（非多选模式且 infoRows 非空时显示） ===
            if (!isBatchMode && (infoRowsHeader != null || infoRows.isNotEmpty())) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(cSurfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        // 顶部额外行（如链接）
                        if (infoRowsHeader != null) {
                            // header 独立框
                            val headerColor = infoRowsHeader.color ?: cOnSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(headerColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        infoRowsHeader.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = headerColor
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = infoRowsHeader.value,
                                        fontSize = 11.sp,
                                        color = headerColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (infoRows.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        // 主体信息行
                        if (infoRowsHorizontal) {
                        // 横排：所有条目在一行内
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            infoRows.forEach { row ->
                                val rowColor = row.color ?: cOnSurfaceVariant
                                // 每个信息独立框
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(rowColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            row.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = rowColor
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = row.value,
                                            fontSize = 11.sp,
                                            color = rowColor,
                                            maxLines = row.maxLines,
                                            overflow = if (row.maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Visible
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 竖排：每个条目一行
                        Column {
                            infoRows.forEachIndexed { index, row ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                                // 每个信息独立框
                                val rowColor = row.color ?: cOnSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(rowColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            row.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = rowColor
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = row.value,
                                            fontSize = 11.sp,
                                            color = rowColor,
                                            maxLines = row.maxLines,
                                            overflow = if (row.maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Visible
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // === 底部栏：禁用开关 + 操作按钮（非多选模式） ===
            if (!isBatchMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左下角：禁用开关
                    if (enableSwitchStyle) {
                        // 滑动开关样式（任务卡片用）
                        Switch(
                            checked = !isDisabled,
                            onCheckedChange = { onToggleEnable() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cGreen,
                                checkedTrackColor = cGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = cRed,
                                uncheckedTrackColor = cRed.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    } else {
                        // 圆角矩形按钮样式（订阅卡片用）
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDisabled) cRed.copy(alpha = 0.3f) else cGreen.copy(alpha = 0.3f))
                                .clickable { onToggleEnable() }
                                .padding(horizontal = 2.dp),
                            contentAlignment = if (isDisabled) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDisabled) cRed else cGreen)
                            )
                        }
                    }

                    // 右下角：操作按钮组
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        bottomButtons()
                    }
                }
            }
        }
    }
}

// ===================== 通用操作按钮 =====================

/**
 * 圆形操作按钮（运行/停止/日志/置顶/编辑/删除等）
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(32.dp)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = tint
        )
    }
}

// ===================== 日志按钮 =====================

/**
 * 日志按钮组件
 */
@Composable
@Suppress("DEPRECATION")
fun LogButton(onViewLog: () -> Unit, tint: Color) {
    ActionButton(
        icon = Icons.Default.Article,
        contentDescription = "日志",
        tint = tint,
        onClick = onViewLog
    )
}

// ===================== 运行/停止按钮 =====================

/**
 * 运行/停止按钮
 * @param isRunning 是否正在运行
 * @param btnIcon 图标（PlayArrow 或 Stop）
 * @param btnDesc 描述（"运行"或"停止"）
 * @param btnColor 颜色
 * @param onRun 运行回调
 * @param onStop 停止回调
 */
@Composable
fun RunStopButton(
    isRunning: Boolean,
    btnIcon: ImageVector,
    btnDesc: String,
    btnColor: Color,
    onRun: () -> Unit,
    onStop: () -> Unit
) {
    ActionButton(
        icon = btnIcon,
        contentDescription = btnDesc,
        tint = btnColor,
        onClick = { if (isRunning) onStop() else onRun() }
    )
}

// ===================== 置顶/取消置顶按钮 =====================

/**
 * 置顶/取消置顶按钮
 * @param isPinned 是否已置顶
 * @param onPin 置顶回调
 * @param onUnpin 取消置顶回调
 */
@Composable
fun PinButton(
    isPinned: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit
) {
    ActionButton(
        icon = Icons.Default.PushPin,
        contentDescription = if (isPinned) "取消置顶" else "置顶",
        tint = if (isPinned) cOrange else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = { if (isPinned) onUnpin() else onPin() }
    )
}

// ===================== 编辑按钮 =====================

/**
 * 编辑按钮
 */
@Composable
fun EditButton(onClick: () -> Unit) {
    ActionButton(
        icon = Icons.Default.Edit,
        contentDescription = "编辑",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    )
}

// ===================== 删除按钮 =====================

/**
 * 删除按钮
 */
@Composable
fun DeleteButton(onClick: () -> Unit) {
    ActionButton(
        icon = Icons.Default.Delete,
        contentDescription = "删除",
        tint = cRed.copy(alpha = 0.7f),
        onClick = onClick
    )
}
