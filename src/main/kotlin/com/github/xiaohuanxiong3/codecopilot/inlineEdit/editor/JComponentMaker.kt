package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextArea
import kotlin.math.max
import kotlin.math.min

/**
 * @Author Handsome Young
 * @Date 2025/1/19 23:34
 */
object JComponentMaker {

    const val MAIN_FONT_SIZE = 13

    fun makeTextArea(): JTextArea {
        val textArea =
            CustomTextArea(1, 40).apply {
                lineWrap = true
                wrapStyleWord = true
                isOpaque = false
                background = getSecondaryDark()
                maximumSize = Dimension(400, Short.MAX_VALUE.toInt())
                margin = JBUI.insets(6, 8, 2, 4)
                font = UIUtil.getFontWithFallback("Arial", Font.PLAIN, MAIN_FONT_SIZE)
                preferredSize = Dimension(400, 75)
            }
        textArea.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
        return textArea
    }

    private fun getSecondaryDark(): Color {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        val defaultBackground = globalScheme.defaultBackground
        val grayscale =
            (defaultBackground.red * 0.3 + defaultBackground.green * 0.59 + defaultBackground.blue * 0.11).toInt()

        val adjustedRed: Int
        val adjustedGreen: Int
        val adjustedBlue: Int

        val tint: Int = 20
        if (grayscale > 128) { // if closer to white
            adjustedRed = max(0, defaultBackground.red - tint)
            adjustedGreen = max(0, defaultBackground.green - tint)
            adjustedBlue = max(0, defaultBackground.blue - tint)
        } else { // if closer to black
            adjustedRed = min(255, defaultBackground.red + tint)
            adjustedGreen = min(255, defaultBackground.green + tint)
            adjustedBlue = min(255, defaultBackground.blue + tint)
        }

        return Color(adjustedRed, adjustedGreen, adjustedBlue)
    }

    fun makePanel(
        project: Project,
        customPanelRef: Ref<CustomPanel>,
        textArea: JTextArea,
        inlayRef: Ref<Disposable>,
        comboBoxRef: Ref<JComboBox<String>>,
        leftInset: Int,
        modelTitles: List<String>,
        onEnter: () -> Unit,
        onCancel: () -> Unit,
        onAccept: () -> Unit,
        onReject: () -> Unit
    ): JPanel {
        val topPanel =
            ShadowPanel(MigLayout("wrap 1, insets 2 ${leftInset} 2 2, gap 0!")).apply {
                background = JBColor(0x20888888.toInt(), 0x20888888.toInt())
                isOpaque = false
            }

        val panel =
            CustomPanel(
                MigLayout("wrap 1, insets 4 10 0 2, gap 0!, fillx"),
                project,
                modelTitles,
                comboBoxRef,
                onEnter,
                onCancel,
                onAccept,
                onReject,
                textArea
            )
                .apply {
                    val globalScheme = EditorColorsManager.getInstance().globalScheme
                    val defaultBackground = globalScheme.defaultBackground
                    background = defaultBackground
                    add(textArea, "grow, gap 0!, height 100%")

                    putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
                    preferredSize = textArea.preferredSize
                    setup()
                }

        customPanelRef.set(panel)

        textArea.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    panel.revalidate()
                    panel.repaint()
                }
            })

        topPanel.add(panel, "grow, gap 0!")

        return topPanel
    }

}