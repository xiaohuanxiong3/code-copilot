package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.context.InlineCompletionRendererContext
import com.intellij.openapi.editor.ex.EditorEx
import net.miginfocom.layout.CC

/**
 * @Author Handsome Young
 * @Date 2025/5/15 21:38
 */
class InlineCompletionRenderer(private val context: InlineCompletionRendererContext, private val editor: EditorEx) : MyRenderer(context, editor) {

    init {
        add(context.textArea, CC().growX())
    }

    override fun getPreferredWidth(): Int {
        return editor.scrollPane.width - editor.scrollPane.verticalScrollBar.width
    }

    // 重写 contains 方法禁止所有鼠标事件？
    override fun contains(x: Int, y: Int): Boolean {
        return false
    }

}