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
 * @Date 2024/11/28 23:32
 */
abstract class InlineCompletionAction(private val inlineCompletionHandler: InlineCompletionActionHandler) :
    EditorAction(object : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
            val inlineCompletionService = editor.project?.serviceOrNull<InlineCompletionService>() ?: return
            inlineCompletionHandler.doExecute(editor, caret, inlineCompletionService)
        }

        override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
            val inlineCompletionService = editor.project?.serviceOrNull<InlineCompletionService>() ?: return false
            return inlineCompletionService.isInlineCompletionVisibleAt(
                editor,
                caret.offset
            ) && inlineCompletionHandler.isEnabledForCaret(editor, caret, inlineCompletionService)
        }
    }), HasPriority {
    override val priority: Int = 1
}