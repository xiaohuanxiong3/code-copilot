package com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.component.RendererComponent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.components.JBScrollPane
import net.miginfocom.swing.MigLayout
import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.JScrollPane
import kotlin.math.max

/**
 * @Author Handsome Young
 * @Date 2025/5/15 14:36
 */
abstract class MyRenderer(private val component: RendererComponent, myEditor: EditorEx) : JPanel(
    MigLayout("fill, insets 0")
), EditorCustomElementRenderer {

    private var myInlay: Inlay<MyRenderer>? = null

    private val myEditorScrollPane: JScrollPane = myEditor.scrollPane

    init {
        isOpaque = false
    }

    open fun getPreferredWidth() : Int {
        return preferredSize.width
    }

    fun getPreferredHeight() : Int {
        return preferredSize.height
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return max(height, 0)
    }

    override fun calcWidthInPixels(p0: Inlay<*>): Int {
        return max(width, 0)
    }

    override fun paint(g: Graphics?) {
        val g2d = g as Graphics2D
        val old = g2d.composite

        try {
            g2d.composite = AlphaComposite.SrcOver
            super<JPanel>.paint(g)
        } finally {
            g2d.composite = old
        }
    }

    override fun doLayout() {
        runReadAction {
            synchronizeBoundsWithInlay()
            super.doLayout()
        }
    }

    override fun validate() {
        synchronizeBoundsWithInlay()
        super.validate()
    }

    private fun synchronizeBoundsWithInlay() {
        myInlay?.let {
            if (!it.editor.document.isInBulkUpdate) {
                val inlayBounds = it.bounds
                var shouldUpdateInlay: Boolean = false
                if (inlayBounds != null) {
                    inlayBounds.setLocation(inlayBounds.x + verticalScrollbarLeftShift(), inlayBounds.y)
                    // not calculate visible width
                    val newBounds = Rectangle(inlayBounds.x, inlayBounds.y, getPreferredWidth(), getPreferredHeight())
                    shouldUpdateInlay = !isVisible || newBounds != inlayBounds
                    if (shouldUpdateInlay || newBounds != bounds) {
                        isVisible = true
                        bounds = newBounds
                    }
                } else {
                    shouldUpdateInlay = this.isVisible
                    this.isVisible = false
                }

                if (shouldUpdateInlay) {
                    it.update()
                }
            }
        }
    }

    fun setInlay(inlay: Inlay<MyRenderer>?) {
        this.myInlay = inlay
    }

    private fun verticalScrollbarLeftShift(): Int {
        val flipProperty: Any? = this.myEditorScrollPane.getClientProperty(JBScrollPane.Flip::class.java)
        return if (flipProperty !== JBScrollPane.Flip.HORIZONTAL && flipProperty !== JBScrollPane.Flip.BOTH) 0 else this.myEditorScrollPane.getVerticalScrollBar()
            .getWidth()
    }
}