package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.JXTextArea
import java.awt.Color
import java.awt.Font
import java.awt.Graphics

/**
 * @Author Handsome Young
 * @Date 2025/1/19 20:42
 */
class CustomTextArea(rows: Int, columns: Int) : JXTextArea("") {

    private val MAIN_FONT_SIZE = 13

    init {
        setRows(rows)
        setColumns(columns)
    }

    override fun paintComponent(g: Graphics) {
        // Draw placeholder
        if (text.isEmpty()) {
            g.color = Color(128, 128, 128, 128)
            g.font = UIUtil.getFontWithFallback("Arial", Font.PLAIN, MAIN_FONT_SIZE)
            g.drawString("Enter instructions...", 8, 20)
        }

        super.paintComponent(g)
    }

}