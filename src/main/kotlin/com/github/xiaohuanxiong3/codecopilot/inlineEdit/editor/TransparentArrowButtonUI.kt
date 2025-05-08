package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.ComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.ListDataListener
import javax.swing.plaf.basic.BasicComboBoxUI

/**
 * @Author Handsome Young
 * @Date 2025/1/19 20:54
 */
class TransparentArrowButtonUI : BasicComboBoxUI() {

    val DOWN_ARROW = " ▾"

    override fun createArrowButton() =
        JButton().apply {
            isVisible = false
            preferredSize = Dimension(0, 0)
        }

    override fun getInsets(): Insets {
        return JBUI.insets(0, 6, 0, 0)
    }

    override fun installUI(c: JComponent?) {
        super.installUI(c)
        comboBox.isOpaque = false
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        comboBox.background = globalScheme.defaultBackground

        // Modify the ComboBoxModel to include the down symbol
        val originalModel = comboBox.model
        comboBox.model = object : ComboBoxModel<Any> {
            override fun getSize(): Int = originalModel.size

            override fun getElementAt(index: Int): Any? = originalModel.getElementAt(index)

            override fun setSelectedItem(anItem: Any?) {
                originalModel.selectedItem = anItem
            }

            override fun getSelectedItem(): Any? {
                val item = originalModel.selectedItem
                return "$item$DOWN_ARROW"
            }

            override fun addListDataListener(l: ListDataListener?) {
                originalModel.addListDataListener(l)
            }

            override fun removeListDataListener(l: ListDataListener?) {
                originalModel.removeListDataListener(l)
            }

        }
    }

    override fun paintCurrentValueBackground(g: Graphics?, bounds: Rectangle?, hasFocus: Boolean) {
        // Do nothing to prevent painting the background
    }

    override fun paintCurrentValue(g: Graphics?, bounds: Rectangle, hasFocus: Boolean) {
        val renderer = comboBox.renderer
        val item = comboBox.selectedItem

        if (item != null) {
            val c = renderer.getListCellRendererComponent(listBox, item, -1, false, false)
            c.font = comboBox.font
            c.foreground = Color(156, 163, 175) // text-gray-400
            c.background = comboBox.background

            if (c is JComponent) {
                c.isOpaque = false
            }

            val currentValuePane = currentValuePane
            currentValuePane.paintComponent(
                g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, true
            )
        }
    }
}