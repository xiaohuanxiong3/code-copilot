package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JLabel
import javax.swing.border.EmptyBorder

/**
 * @Author Handsome Young
 * @Date 2025/1/19 20:32
 */
class CustomButton(text: String, onClick: () -> Unit) : JLabel(text, CENTER) {

    private var isHovered = false

    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    isHovered = true
                    repaint()
                }

                override fun mouseExited(e: MouseEvent?) {
                    isHovered = false
                    repaint()
                }

                override fun mouseClicked(e: MouseEvent?) {
                    onClick()
                }
            }
        )

        font = UIUtil.getFontWithFallback("Arial", Font.PLAIN, 11)
        border = EmptyBorder(2, 6, 2, 6)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cornerRadius = 8
        val rect = Rectangle(0, 0, width, height)
        val roundRect =
            RoundRectangle2D.Float(
                rect.x.toFloat(),
                rect.y.toFloat(),
                rect.width.toFloat(),
                rect.height.toFloat(),
                cornerRadius.toFloat(),
                cornerRadius.toFloat()
            )

        g2.color = background
        g2.fill(roundRect)
        g2.color = foreground
        g2.drawString(
            text,
            (width / 2 - g.fontMetrics.stringWidth(text) / 2).toFloat(),
            (height / 2 + g.fontMetrics.ascent / 2).toFloat()
        )
    }

}