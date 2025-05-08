package com.github.xiaohuanxiong3.codecopilot.completion

import com.github.xiaohuanxiong3.codecopilot.util.objectMapper

/**
 * @Author Handsome Young
 * @Date 2025/5/7 17:28
 */
object InlineCompletionTool {

    fun buildUserMessage(inlineCompletionRequestContext: InlineCompletionContext.RequestContext): String {
        val promptMap = mapOf(
            "message" to "你是一个经验丰富的编程专家，给你可能用到的代码上下文context、代码前缀prefix和代码后缀suffix，预测需要在代码前缀和代码后缀之间补全的代码，返回预测的补全代码。当你预测有需要补全的代码时优先只返回一行补全代码。如果没有需要补全的代码，返回空字符串。",
            "context" to inlineCompletionRequestContext.codeContext,
            "prefix" to inlineCompletionRequestContext.prefix,
            "suffix" to inlineCompletionRequestContext.suffix
        )
        return objectMapper.writeValueAsString(promptMap)
    }

}