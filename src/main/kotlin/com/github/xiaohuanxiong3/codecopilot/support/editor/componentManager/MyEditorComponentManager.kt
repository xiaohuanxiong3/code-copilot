package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.component.InlineCompletionRendererComponent
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.component.RendererComponent
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.InlineCompletionRenderer
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.MyRenderer
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.RendererType
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * 参考 EditorEmbeddedComponentManager 实现
 * @see com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
 * @Author Handsome Young
 * @Date 2025/5/15 14:25
 */
object MyEditorComponentManager {

    private val COMPONENT_INLAYS_KEY = Key.create<MyComponentInlays>("ComponentInlays")

    fun addComponent(editor: EditorEx, component: RendererComponent, offset: Int, rendererType: RendererType) : Inlay<MyRenderer>? {
        ThreadingAssertions.assertEventDispatchThread()
        val inlays = getComponentInlaysFor(editor)
        return when(rendererType) {
            RendererType.INLINE_COMPLETION -> {
                inlays.add(offset, InlineCompletionRenderer(component as InlineCompletionRendererComponent, editor))
            }
        }
    }

    fun getComponentInlaysFor(editor: EditorEx) : MyComponentInlays {
        if (!COMPONENT_INLAYS_KEY.isIn(editor)) {
            COMPONENT_INLAYS_KEY.set(editor, MyComponentInlays(editor))
        }
        return COMPONENT_INLAYS_KEY.get(editor)
    }

}