package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import org.jdesktop.swingx.JXPanel
import java.awt.Dimension
import java.awt.LayoutManager

/**
 * @Author Handsome Young
 * @Date 2025/1/19 20:51
 */
class ShadowPanel(layout: LayoutManager) : JXPanel(layout) {

    override fun getPreferredSize(): Dimension {
        val prefSize = super.getPreferredSize()
        val insets = getInsets()
        prefSize.width += insets.left + insets.right
        prefSize.height += insets.top + insets.bottom
        return prefSize
    }

}