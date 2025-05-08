package com.github.xiaohuanxiong3.codecopilot.completion.action

import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionService
import com.github.xiaohuanxiong3.codecopilot.support.actionPromoter.HasPriority
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

/**
 * @Author Handsome Young
 * @Date 2024/11/28 23:37
 */
class RejectOrCancelCompletionAction : EditorAction(object : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        val inlineCompletionService = editor.project?.serviceOrNull<InlineCompletionService>() ?: return
        inlineCompletionService.dismiss()
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
        return true
    }
}), HasPriority {
    override val priority: Int = 1
}