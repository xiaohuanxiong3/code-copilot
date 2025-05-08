package com.github.xiaohuanxiong3.codecopilot.completion.action

import com.github.xiaohuanxiong3.codecopilot.completion.AcceptType
import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionService
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor

/**
 * @Author Handsome Young
 * @Date 2024/11/29 00:25
 */
class AcceptNextLineAction : InlineCompletionAction(object : InlineCompletionActionHandler{

    override fun doExecute(editor: Editor, caret: Caret?, inlineCompletionService: InlineCompletionService) {
        inlineCompletionService.accept(editor, editor.caretModel.offset, AcceptType.NEXT_LINE)
    }

}) {
}