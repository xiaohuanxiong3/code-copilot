package com.github.xiaohuanxiong3.codecopilot.inlineEdit

import com.github.xiaohuanxiong3.codecopilot.util.objectMapper

/**
 * @Author Handsome Young
 * @Date 2025/5/8 01:16
 */
object InlineEditTool {

    fun build(fileContent: String, userInstruct: String, returnDiffPatch: Boolean) : String {
        val promptMap = mapOf(
            "prompt" to if (returnDiffPatch)
                            "根据给定代码上下文context和用户指令instruct执行任务。返回diff patch。返回值是json格式的，格式如下 { \"r\": <diff patch>}"
                        else
                            "根据给定代码上下文context和用户指令instruct执行任务。返回修改后的完整文件内容。返回值是json格式的，格式如下 { \"r\": <file content>}",
//            "prompt" to "根据给定代码上下文context和用户指令instruct执行任务。返回diff patch。返回值需要是纯代码文本，不能是markdown格式",
            "instruct" to userInstruct,
            "context" to fileContent
        )

        return objectMapper.writeValueAsString(promptMap)
    }

}