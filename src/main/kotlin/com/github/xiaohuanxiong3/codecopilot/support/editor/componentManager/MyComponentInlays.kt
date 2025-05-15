package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.MyRenderer
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FoldingListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.Disposer
import com.intellij.util.application
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

/**
 * @Author Handsome Young
 * @Date 2025/5/15 14:32
 */
class MyComponentInlays(editor: EditorEx) : Disposable {

    private val myEditor: EditorEx = editor

    init {
        setup()
    }

    fun add(offset: Int, renderer: MyRenderer) : Inlay<MyRenderer>? {
        if (myEditor.isDisposed) {
            return null
        } else {
//            val renderer = MyRenderer(component, myEditor.scrollPane)
            val inlay = myEditor.inlayModel.addBlockElement(offset,
                InlayProperties().showAbove(true).priority(0),
                renderer)
            if (inlay == null) {
                return null
            } else {
                Disposer.register(this, inlay)
                renderer.addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        val var3 = e.source
                        if (var3 is MyRenderer) {
                            this@MyComponentInlays.revalidateComponents(renderer.bounds.y)
                        }
                    }
                })
                // 确保嵌入式组件的鼠标滚轮事件能正确传递到编辑器内容组件
                renderer.addMouseWheelListener(myEditor.contentComponent::dispatchEvent)
                renderer.setInlay(inlay)
                myEditor.contentComponent.add(renderer)
                Disposer.register(inlay, {
                    val runnable = Runnable {
                        renderer.setInlay(null)
                        myEditor.contentComponent.remove(renderer)
                    }
                    if (application.isDispatchThread) {
                        runnable.run()
                    } else {
                        application.invokeLater( runnable)
                    }
                })
                if (!myEditor.inlayModel.isInBatchMode) {
                    renderer.revalidate()
                }
                return inlay
            }
        }
    }

    private fun revalidateComponents() {
        revalidateComponents(Int.MIN_VALUE)
    }

    private fun revalidateComponents(yTop: Int) {
        myEditor.contentComponent.components.forEach { component ->
            if (component is MyRenderer && component.y >= yTop) {
                component.revalidate()
            }
        }
    }

    private fun setup() {
        Disposer.register((myEditor as EditorImpl).disposable, this)
        myEditor.foldingModel.addListener(object : FoldingListener {
            override fun onFoldProcessingEnd() {
                this@MyComponentInlays.revalidateComponents()
            }
        }, this)

        myEditor.document.addDocumentListener(object : DocumentListener {
            private var linesBefore : Int = 0

            override fun beforeDocumentChange(event: DocumentEvent) {
                linesBefore = event.document.lineCount
            }

            override fun documentChanged(event: DocumentEvent) {
                if (linesBefore != event.document.lineCount && !event.document.isInBulkUpdate) {
                    val y = this@MyComponentInlays.myEditor.logicalPositionToXY(LogicalPosition(event.document.getLineNumber(event.offset), 0)).y
                    this@MyComponentInlays.revalidateComponents(y)
                }
            }

            override fun bulkUpdateFinished(document: Document) {
                this@MyComponentInlays.revalidateComponents()
            }
        })

        myEditor.inlayModel.addListener(object : InlayModel.SimpleAdapter() {
            override fun onUpdated(inlay: Inlay<*>, changeFlags: Int) {
                if (changeFlags and 2 != 0) {
                    val var4 : EditorCustomElementRenderer = inlay.renderer
                    if (var4 is MyRenderer) {
                        if (var4.isValid) {
                            var4.revalidate()
                        }
                    }
                }
            }

            override fun onRemoved(inlay: Inlay<*>) {
                Disposer.dispose(inlay)
            }
        }, this)

        val viewportListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                this@MyComponentInlays.revalidateComponents()
            }
        }
        val viewport = myEditor.scrollPane.viewport
        viewport.addComponentListener(viewportListener)
        Disposer.register(this) { viewport.removeComponentListener(viewportListener) }
    }


    override fun dispose() {
    }
}