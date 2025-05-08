package com.github.xiaohuanxiong3.codecopilot.inlineEdit

/**
 * @Author Handsome Young
 * @Date 2025/1/24 17:16
 */
data class InlineEditContext(
    val filePath: String,
    val originalText: String,
    val modifiedText: String? = null,
    var diffLines: List<DiffLine>? = null
)

data class DiffLine(
    val type: DiffLineType,
    val text: String,
)

enum class DiffLineType {
    SAME,
    NEW,
    OLD
}


