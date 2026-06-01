package com.qinglong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 公共代码编辑器组件
 * 包含行号 + 语法高亮 + BasicTextField 编辑区
 * 不包含顶栏、弹窗等外围 UI，由调用方自行组装
 *
 * @param textFieldValue 编辑器内容
 * @param onValueChange 内容变化回调
 * @param language 语法高亮语言（shell/javascript/python/json 等）
 * @param isDarkTheme 是否深色模式
 * @param placeholder 占位文字
 * @param modifier 修饰符
 */
@Composable
fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    isDarkTheme: Boolean,
    placeholder: String = "在此输入代码...",
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
    val textColor = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF333333)
    val lineNumberColor = if (isDarkTheme) Color(0xFF858585) else Color(0xFF999999)
    val cursorColor = if (isDarkTheme) Color(0xFF569CD6) else Color(0xFF0066CC)

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Row(modifier = modifier.background(bgColor)) {
        // 行号
        val lineCount = textFieldValue.text.count { it == '\n' } + 1
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .verticalScroll(verticalScroll)
                .padding(top = 8.dp, end = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 1..lineCount) {
                Text(
                    text = i.toString(),
                    color = lineNumberColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // 分隔线
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(lineNumberColor.copy(alpha = 0.3f))
        )

        // 代码编辑区
        BasicTextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = textColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            ),
            cursorBrush = SolidColor(cursorColor),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(8.dp),
            visualTransformation = SyntaxHighlightTransformation(language, isDarkTheme),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = lineNumberColor.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * 语法高亮转换器
 */
class SyntaxHighlightTransformation(
    private val language: String,
    private val isDarkTheme: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotated = buildAnnotatedString {
            append(text.text)
            highlightSyntax(this, text.text, language, isDarkTheme)
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

/**
 * 语法高亮
 * 支持 shell/javascript/python/json 等语言
 */
fun highlightSyntax(
    builder: AnnotatedString.Builder,
    text: String,
    @Suppress("UNUSED_PARAMETER") language: String,
    isDarkTheme: Boolean
) {
    val keywordColor = if (isDarkTheme) Color(0xFF569CD6) else Color(0xFF0000FF)
    val stringColor = if (isDarkTheme) Color(0xFFCE9178) else Color(0xFF008000)
    val commentColor = if (isDarkTheme) Color(0xFF6A9955) else Color(0xFF008000)
    val numberColor = if (isDarkTheme) Color(0xFFB5CEA8) else Color(0xFF098658)
    val functionColor = if (isDarkTheme) Color(0xFFDCDCAA) else Color(0xFF795E26)

    // 关键字
    val keywords = setOf(
        "function", "var", "let", "const", "if", "else", "for", "while", "do",
        "return", "break", "continue", "switch", "case", "default", "try", "catch",
        "finally", "throw", "new", "this", "typeof", "instanceof", "void", "delete",
        "import", "export", "from", "class", "extends", "super", "yield", "await",
        "async", "of", "in", "true", "false", "null", "undefined", "NaN",
        "def", "if", "elif", "else", "for", "while", "break", "continue",
        "return", "yield", "import", "from", "as", "class", "try", "except",
        "finally", "raise", "with", "pass", "lambda", "and", "or", "not",
        "is", "in", "True", "False", "None"
    )

    // 按行处理
    val lines = text.split("\n")
    var pos = 0
    for (line in lines) {
        var i = 0
        val lineLen = line.length

        while (i < lineLen) {
            // 注释（# 或 //）
            if ((i + 1 < lineLen && line[i] == '/' && line[i + 1] == '/') || line[i] == '#') {
                builder.addStyle(
                    SpanStyle(color = commentColor),
                    pos + i, pos + lineLen
                )
                break
            }

            // 多行注释 /* */
            if (i + 1 < lineLen && line[i] == '/' && line[i + 1] == '*') {
                var end = text.indexOf("*/", pos + i)
                if (end == -1) end = text.length
                builder.addStyle(
                    SpanStyle(color = commentColor),
                    pos + i, end + 2
                )
                i = end + 2 - pos
                continue
            }

            // 字符串（单引号、双引号、反引号）
            if (line[i] == '"' || line[i] == '\'' || line[i] == '`') {
                val quote = line[i]
                var end = i + 1
                while (end < lineLen) {
                    if (line[end] == '\\') { end += 2; continue }
                    if (line[end] == quote) { end++; break }
                    end++
                }
                builder.addStyle(
                    SpanStyle(color = stringColor),
                    pos + i, pos + end
                )
                i = end
                continue
            }

            // 数字
            if (line[i].isDigit() && (i == 0 || !line[i - 1].isLetterOrDigit())) {
                var end = i
                while (end < lineLen && (line[end].isDigit() || line[end] == '.')) end++
                builder.addStyle(
                    SpanStyle(color = numberColor),
                    pos + i, pos + end
                )
                i = end
                continue
            }

            // 关键字和函数名
            if (line[i].isLetter() || line[i] == '_') {
                var end = i
                while (end < lineLen && (line[end].isLetterOrDigit() || line[end] == '_')) end++
                val word = line.substring(i, end)
                if (word in keywords) {
                    builder.addStyle(
                        SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                        pos + i, pos + end
                    )
                } else if (end < lineLen && line[end] == '(') {
                    builder.addStyle(
                        SpanStyle(color = functionColor),
                        pos + i, pos + end
                    )
                }
                i = end
                continue
            }

            i++
        }
        pos += lineLen + 1 // +1 for newline
    }
}

/**
 * 根据文件名推断代码语言
 */
fun getLanguageByFilename(filename: String): String {
    val ext = filename.substringAfterLast('.', "")
    return when (ext) {
        "sh" -> "shell"
        "js" -> "javascript"
        "py" -> "python"
        "ts" -> "typescript"
        "json" -> "json"
        "ini" -> "ini"
        "yaml", "yml" -> "yaml"
        "conf" -> "shell"
        "env" -> "shell"
        else -> "shell"
    }
}
