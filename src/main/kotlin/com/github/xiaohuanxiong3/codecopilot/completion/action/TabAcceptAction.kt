package com.github.xiaohuanxiong3.codecopilot.completion.action

import com.github.xiaohuanxiong3.codecopilot.completion.AcceptType
import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionService
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor

/**
 * @Author Handsome Young
 * @Date 2024/11/28 23:35
 */
class TabAcceptAction : InlineCompletionAction(object : InlineCompletionActionHandler {

    override fun doExecute(editor: Editor, caret: Caret?, inlineCompletionService: InlineCompletionService) {
        inlineCompletionService.accept(editor, editor.caretModel.offset, AcceptType.FULL_COMPLETION)
    }

}) {
}