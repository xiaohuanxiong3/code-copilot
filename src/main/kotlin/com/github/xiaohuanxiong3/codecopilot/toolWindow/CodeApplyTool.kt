package com.github.xiaohuanxiong3.codecopilot.toolWindow

import com.github.xiaohuanxiong3.codecopilot.util.objectMapper

/**
 * @Author Handsome Young
 * @Date 2025/5/4 22:59
 */
object CodeApplyTool {

    private fun getApplyCodePrompt() : String {
        return """
            将以json格式提供的applyCode全部合并到originalCode中，合并时以applyCode的内容为准
            返回值需要是json格式，返回值中的换行符需要是unix格式，返回值格式如下:
            {
                "r": ......
            }
        """.trimIndent()
    }

    data class CodeApplyMessage(
        val message: String,
        val originalCode: String,
        val applyCode: String
    )

    fun build(originalCode: String, applyCode: String): String {
        return objectMapper.writeValueAsString(CodeApplyMessage(getApplyCodePrompt(), originalCode, applyCode))
    }

}