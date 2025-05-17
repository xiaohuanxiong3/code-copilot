package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.component.InlineCompletionRendererComponent
import com.github.xiaohuanxiong3.codecopilot.ui.UI
import com.intellij.openapi.editor.ex.EditorEx

/**
 * @Author Handsome Young
 * @Date 2025/5/15 21:38
 */
class InlineCompletionRenderer(private val component: InlineCompletionRendererComponent, private val editor: EditorEx) : MyRenderer(component, editor) {

    init {
        add(component.textArea, UI.fill())
    }

    override fun getPreferredWidth(): Int {
        return editor.scrollPane.width - editor.scrollPane.verticalScrollBar.width
    }

    // 重写 contains 方法禁止所有鼠标事件？
    override fun contains(x: Int, y: Int): Boolean {
        return false
    }

}