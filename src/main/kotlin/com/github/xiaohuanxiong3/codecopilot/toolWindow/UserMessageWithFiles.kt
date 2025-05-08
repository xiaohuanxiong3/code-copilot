package com.github.xiaohuanxiong3.codecopilot.toolWindow

/**
 * @Author Handsome Young
 * @Date 2025/5/4 19:45
 */
data class UserMessageWithFiles(
    val message: String,
    val files: List<File>
)

data class File(
    val fileName: String,
    val fileContent: String
)
