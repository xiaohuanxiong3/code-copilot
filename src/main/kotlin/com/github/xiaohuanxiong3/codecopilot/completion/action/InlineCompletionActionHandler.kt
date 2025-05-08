package com.github.xiaohuanxiong3.codecopilot.completion.action

import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionService
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor

/**
 * @Author Handsome Young
 * @Date 2024/11/28 23:32
 */
interface InlineCompletionActionHandler {
    fun doExecute(editor: Editor, caret: Caret?, inlineCompletionService: InlineCompletionService)
    fun isEnabledForCaret(editor: Editor, caret: Caret, inlineCompletionService: InlineCompletionService): Boolean {
        return true
    }
}