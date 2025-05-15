package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer

import javax.swing.JComponent
import javax.swing.JScrollPane

/**
 * @Author Handsome Young
 * @Date 2025/5/15 21:38
 */
class InlineCompletionRenderer(private val component: JComponent, private val myEditorScrollPane: JScrollPane) : MyRenderer(component, myEditorScrollPane) {

    override fun getPreferredWidth(): Int {
        return myEditorScrollPane.viewport.width
    }

    // 重写 contains 方法禁止所有鼠标事件？
    override fun contains(x: Int, y: Int): Boolean {
        return false
    }

}