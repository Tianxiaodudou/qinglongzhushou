package com.qinglong.app.util

import java.util.Calendar
import java.util.TimeZone

/**
 * 轻量级 Cron 表达式解析器（数学跳转版）
 * 支持标准 5 位 cron 格式：分 时 日 月 周
 * 支持: * , - / 操作符
 *
 * 核心思路：从大到小逐层匹配，不匹配时直接跳到下一个候选值
 * 月份 → 日期（日月兼容） → 小时 → 分钟
 */
object CronParser {

    /**
     * 根据 cron 表达式计算下次运行时间
     * @param cron cron表达式，支持5位标准格式和6位（带秒）格式
     * @param fromTime 从哪个时间开始计算（Unix秒），null 则从当前时间开始
     * @return 下次运行时间的 Calendar 对象，如果无法解析则返回 null
     */
    fun getNextRunTime(cron: String, fromTime: Long? = null): Calendar? {
        try {
            var expr = cron.trim()

            // 展开 @ 别名
            expr = expandAlias(expr) ?: return null

            val parts = expr.split("\\s+".toRegex())

            // 处理 6 位 cron（带秒）：忽略第一位（秒）
            var minuteExpr: String
            var hourExpr: String
            var dayOfMonthExpr: String
            var monthExpr: String
            var dayOfWeekExpr: String

            when (parts.size) {
                5 -> {
                    minuteExpr = parts[0]
                    hourExpr = parts[1]
                    dayOfMonthExpr = parts[2]
                    monthExpr = parts[3]
                    dayOfWeekExpr = parts[4]
                }
                6 -> {
                    // 6 位：秒 分 时 日 月 周，忽略第一位（秒）
                    minuteExpr = parts[1]
                    hourExpr = parts[2]
                    dayOfMonthExpr = parts[3]
                    monthExpr = parts[4]
                    dayOfWeekExpr = parts[5]
                }
                else -> return null // 不支持
            }

            // 检查是否包含不支持的特殊字符（L, W, #）
            if (containsUnsupported(dayOfMonthExpr) || containsUnsupported(dayOfWeekExpr)) {
                return null
            }

            val cal = Calendar.getInstance(TimeZone.getDefault())
            if (fromTime != null) {
                cal.timeInMillis = fromTime * 1000
            }
            // 从下一分钟开始搜索
            cal.add(Calendar.MINUTE, 1)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // 最多搜索 5 年
            val maxYear = cal.get(Calendar.YEAR) + 5

            // 逐层匹配：月份 → 日期 → 小时 → 分钟
            while (cal.get(Calendar.YEAR) <= maxYear) {
                // 1. 匹配月份
                val month = cal.get(Calendar.MONTH) + 1
                if (!matchField(month, monthExpr, 1, 12)) {
                    // 跳到下个月 1 号 00:00
                    cal.add(Calendar.MONTH, 1)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    continue
                }

                // 2. 匹配日期（日 + 星期兼容）
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val cronDayOfWeek = when (dayOfWeek) {
                    Calendar.SUNDAY -> 0
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    else -> -1
                }
                val matchDay = matchField(day, dayOfMonthExpr, 1, 31)
                val matchWeek = matchField(cronDayOfWeek, dayOfWeekExpr, 0, 7) ||
                        matchField(7, dayOfWeekExpr, 0, 7)

                // 如果日和星期都是 *，或者其中一个匹配即可
                val dayIsStar = dayOfMonthExpr.trim() == "*"
                val weekIsStar = dayOfWeekExpr.trim() == "*" || dayOfWeekExpr.trim() == "?" 
                val dayMatched: Boolean
                if (dayIsStar && weekIsStar) {
                    dayMatched = true
                } else if (dayIsStar) {
                    dayMatched = matchWeek
                } else if (weekIsStar) {
                    dayMatched = matchDay
                } else {
                    // 两者都有限定，满足任一即可
                    dayMatched = matchDay || matchWeek
                }

                if (!dayMatched) {
                    // 跳到下一天 00:00
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    continue
                }

                // 3. 匹配小时
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (!matchField(hour, hourExpr, 0, 23)) {
                    // 跳到下一小时的 00 分
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    continue
                }

                // 4. 匹配分钟
                val minute = cal.get(Calendar.MINUTE)
                if (!matchField(minute, minuteExpr, 0, 59)) {
                    // 跳到下一分钟
                    cal.add(Calendar.MINUTE, 1)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    continue
                }

                // 全部匹配！
                return cal
            }

            return null
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 解析 cron 字段，返回所有符合条件的值列表（排序后）
     * 用于"跳到下一个候选值"
     */
    private fun parseFieldValues(expr: String, min: Int, max: Int): List<Int>? {
        val trimmed = expr.trim()
        if (trimmed == "*" || trimmed == "?") return null // null 表示全部匹配

        val result = mutableSetOf<Int>()

        // 逗号分隔
        if (trimmed.contains(",")) {
            for (part in trimmed.split(",")) {
                val values = parseFieldValues(part.trim(), min, max) ?: return null
                result.addAll(values)
            }
            return result.sorted()
        }

        // 步进: */5 或 1-10/2
        if (trimmed.contains("/")) {
            val slashParts = trimmed.split("/")
            val range = slashParts[0].trim()
            val step = slashParts[1].trim().toIntOrNull() ?: return null
            val rangeStart: Int
            val rangeEnd: Int
            if (range == "*") {
                rangeStart = min
                rangeEnd = max
            } else if (range.contains("-")) {
                val rangeParts = range.split("-")
                rangeStart = rangeParts[0].trim().toIntOrNull() ?: return null
                rangeEnd = rangeParts[1].trim().toIntOrNull() ?: return null
            } else {
                rangeStart = range.toIntOrNull() ?: return null
                rangeEnd = max
            }
            var v = rangeStart
            while (v <= rangeEnd) {
                result.add(v)
                v += step
            }
            return result.sorted()
        }

        // 范围: 1-5
        if (trimmed.contains("-")) {
            val rangeParts = trimmed.split("-")
            val start = rangeParts[0].trim().toIntOrNull() ?: return null
            val end = rangeParts[1].trim().toIntOrNull() ?: return null
            for (v in start..end) result.add(v)
            return result.sorted()
        }

        // 单个数字
        val num = trimmed.toIntOrNull() ?: return null
        return listOf(num)
    }

    /**
     * 判断值是否匹配 cron 字段表达式
     */
    private fun matchField(value: Int, expr: String, min: Int, max: Int): Boolean {
        val trimmed = expr.trim()
        if (trimmed == "*" || trimmed == "?") return true

        // 逗号分隔的多个值
        if (trimmed.contains(",")) {
            return trimmed.split(",").any { matchField(value, it.trim(), min, max) }
        }

        // 步进值: */5 或 1-10/2
        if (trimmed.contains("/")) {
            val parts = trimmed.split("/")
            val range = parts[0].trim()
            val step = parts[1].trim().toIntOrNull() ?: return false
            val rangeStart: Int
            val rangeEnd: Int
            if (range == "*") {
                rangeStart = min
                rangeEnd = max
            } else if (range.contains("-")) {
                val rangeParts = range.split("-")
                rangeStart = rangeParts[0].trim().toIntOrNull() ?: return false
                rangeEnd = rangeParts[1].trim().toIntOrNull() ?: return false
            } else {
                rangeStart = range.toIntOrNull() ?: return false
                rangeEnd = max
            }
            return value in rangeStart..rangeEnd && (value - rangeStart) % step == 0
        }

        // 范围: 1-5
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-")
            val start = parts[0].trim().toIntOrNull() ?: return false
            val end = parts[1].trim().toIntOrNull() ?: return false
            return value in start..end
        }

        // 单个数字
        val num = trimmed.toIntOrNull() ?: return false
        return value == num
    }

    /**
     * 展开 cron 别名
     * @return 展开后的标准 cron 表达式，如果无法识别则返回 null
     */
    private fun expandAlias(expr: String): String? {
        return when (expr.lowercase()) {
            "@yearly", "@annually" -> "0 0 1 1 *"
            "@monthly" -> "0 0 1 * *"
            "@weekly" -> "0 0 * * 0"
            "@daily", "@midnight" -> "0 0 * * *"
            "@hourly" -> "0 * * * *"
            else -> expr // 不是别名，原样返回
        }
    }

    /**
     * 检查 cron 字段是否包含不支持的特殊字符（L, W, #）
     */
    private fun containsUnsupported(expr: String): Boolean {
        val trimmed = expr.trim()
        if (trimmed == "*" || trimmed == "?") return false
        // 检查是否包含 L, W, # 字符（但排除数字中的字母）
        return trimmed.any { it == 'L' || it == 'W' || it == '#' }
    }

    /**
     * 将 Unix 时间戳格式化为可读的日期时间字符串
     */
    fun formatTimestamp(timestamp: Long): String {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = timestamp * 1000
        return String.format(
            "%04d-%02d-%02d %02d:%02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }
}
