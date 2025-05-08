package com.github.xiaohuanxiong3.codecopilot.inlineEdit.diff

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor

/**
 * @Author Handsome Young
 * @Date 2025/1/27 21:07
 */
@Service(Service.Level.PROJECT)
class DiffService {

    private val handlers = mutableMapOf<Editor, DiffHandler>()

    fun register(handler: DiffHandler, editor: Editor) {
        if (handlers.containsKey(editor)) {
            // DiffHandler.rejectAll
            handlers[editor]?.rejectAll()
        }
        handlers[editor] = handler
        thisLogger().info("Registered handler for editor")
    }

    fun reject(editor: Editor) {
        handlers[editor]?.rejectAll()
        handlers.remove(editor)
    }

    fun accept(editor: Editor) {
        handlers[editor]?.acceptAll()
        handlers.remove(editor)
    }

}