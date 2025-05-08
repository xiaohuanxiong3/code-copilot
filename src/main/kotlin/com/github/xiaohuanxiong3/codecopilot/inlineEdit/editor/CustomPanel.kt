package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.github.xiaohuanxiong3.codecopilot.util.getMetaKeyLabel
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Ref
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * @Author Handsome Young
 * @Date 2025/1/19 21:46
 */
class CustomPanel(
    layout: MigLayout,
    project: Project,
    modelTitles: List<String>,
    comboBoxRef: Ref<JComboBox<String>>,
    private val onEnter: () -> Unit,
    private val onCancel: () -> Unit,
    private val onAccept: () -> Unit,
    private val onReject: () -> Unit,
    private val textArea: JTextArea
) : JPanel(layout) {
    private val shadowSize = 5
    private val cornerRadius = 8
    private val shadowColor = Color(0, 0, 0, 40) // Lighter shadow
    private val borderColor = Color(128, 128, 128, 128)
    private val borderThickness = 1
    private val triangleSize = 6
    private val rightMargin = 3.0
    private val closeButton: JComponent = createCloseButton()
    private val originalTextColor: Color = textArea.foreground
    private val greyTextColor: Color = Color(128, 128, 128, 128)
    var isFinished = false

    init {
        isOpaque = false
        add(closeButton, "pos 100%-33 0 -3 3, w 20!, h 20!")
    }

    private fun createCloseButton(): JComponent {
        val closeIcon = IconLoader.getIcon("/icons/close.svg", javaClass)
        return JLabel(closeIcon).apply {
            background = Color(0, 0, 0, 0)
            border = EmptyBorder(2, 6, 2, 0)
            toolTipText = "`esc` to cancel"
            isOpaque = false
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent?) {
                        cursor = Cursor(Cursor.HAND_CURSOR)
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        cursor = Cursor.getDefaultCursor()
                    }

                    override fun mouseClicked(e: MouseEvent?) {
                        onCancel()
                    }
                }
            )
        }
    }

    private val subPanelA: JPanel =
        JPanel(MigLayout("insets 0, fillx")).apply {
            val globalScheme = EditorColorsManager.getInstance().globalScheme
            val defaultBackground = globalScheme.defaultBackground
            val dropDown =
                JComboBox(modelTitles.toTypedArray()).apply {
                    setUI(TransparentArrowButtonUI())
                    isEditable = true
                    background = defaultBackground
                    foreground = Color(128, 128, 128, 200)
                    font = UIUtil.getFontWithFallback("Arial", Font.PLAIN, 12)
                    border = EmptyBorder(8, 8, 8, 8)
                    isOpaque = false
                    isEditable = false
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    preferredSize = Dimension(200, 30)

                    renderer = object : DefaultListCellRenderer() {
                        override fun getListCellRendererComponent(
                            list: JList<*>?,
                            value: Any?,
                            index: Int,
                            isSelected: Boolean,
                            cellHasFocus: Boolean
                        ): Component {
                            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                            if (component is JLabel) {
                                component.border = EmptyBorder(4, 6, 4, 6)
                            }
                            return component
                        }
                    }.apply {
                        horizontalAlignment = SwingConstants.LEFT
                    }

                    selectedIndex = 0

                }

            comboBoxRef.set(dropDown)

            val rightButton =
                CustomButton("⏎  Enter") { onEnter() }
                    .apply {
                        background = JBColor(0x999998, 0x999998)
                        foreground = JBColor(0xF5F5F5, 0xF5F5F5)
                        border = EmptyBorder(2, 6, 2, 6)
                    }

            val rightPanel = JPanel(MigLayout("insets 0, fillx")).apply {
                isOpaque = false
                add(rightButton, "align right")
            }

            border = EmptyBorder(0, 0, 16, 12)
            isOpaque = false

            add(dropDown, "align left")
            add(rightPanel, "align right")

            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

    private val subPanelB: JPanel =
        JPanel(BorderLayout()).apply {
            isOpaque = false

            val progressBar = JProgressBar().apply { isIndeterminate = true }

            add(progressBar, BorderLayout.CENTER)
        }

    private val subPanelC: JPanel =
        JPanel(MigLayout("insets 0, fillx")).apply {
            val leftLabel =
                JLabel("Type to re-prompt").apply {
                    foreground = Color(156, 163, 175) // text-gray-400
                    font = UIUtil.getFontWithFallback("Arial", Font.PLAIN, 11)
                    border = EmptyBorder(0, 4, 0, 0)
                }

            val leftButton =
                CustomButton("Reject All (${getMetaKeyLabel()}⇧⌫)") { onReject() }
                    .apply {
                        background = JBColor(0x30FF0000, 0x30FF0000)
                        foreground = JBColor(0xF5F5F5, 0xF5F5F5)
                    }

            val rightButton =
                CustomButton("Accept All (${getMetaKeyLabel()}⇧⏎)") { onAccept() }
                    .apply {
                        background = JBColor(0x3000FF00.toInt(), 0x3000FF00.toInt())
                        foreground = JBColor(0xF5F5F5.toInt(), 0xF5F5F5.toInt())
                    }

            val rightPanel =
                JPanel(MigLayout("insets 0, fillx")).apply {
                    isOpaque = false
                    add(leftButton, "align right")
                    add(rightButton, "align right")
                    border = EmptyBorder(0, 0, 0, 0)
                }

            add(leftLabel, "align left")
            add(rightPanel, "align right")
            border = BorderFactory.createEmptyBorder(0, 0, 16, 12)
            isOpaque = false
        }

    fun setup() {
        remove(subPanelB)
        remove(subPanelC)
        add(subPanelA, "grow, gap 0!")
        isFinished = false
        revalidate()
        repaint()
        textArea.foreground = originalTextColor
    }

    fun enter() {
        remove(subPanelA)
        remove(subPanelC)
        add(subPanelB, "grow, gap 0!")
        revalidate()
        repaint()
    }

    fun finish() {
        remove(subPanelB)
        add(subPanelC, "grow, gap 0!")
        revalidate()
        repaint()
        isFinished = true
        textArea.foreground = greyTextColor
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width - shadowSize
        val h = height - shadowSize

        // Create the shape for the tooltip/triangle
        val shape = Path2D.Double()
        shape.moveTo(borderThickness / 2.0, cornerRadius.toDouble())
        shape.quadTo(
            borderThickness / 2.0,
            borderThickness / 2.0,
            cornerRadius.toDouble(),
            borderThickness / 2.0
        )
        shape.lineTo(w - cornerRadius.toDouble() - rightMargin, borderThickness / 2.0)
        shape.quadTo(
            w - borderThickness / 2.0 - rightMargin,
            borderThickness / 2.0,
            w - borderThickness / 2.0 - rightMargin,
            cornerRadius.toDouble()
        )
        shape.lineTo(
            w - borderThickness / 2.0 - rightMargin, h - cornerRadius - triangleSize.toDouble()
        )
        shape.quadTo(
            w - borderThickness / 2.0 - rightMargin,
            h - triangleSize.toDouble(),
            w - cornerRadius.toDouble() - rightMargin,
            h - triangleSize.toDouble()
        )
        shape.lineTo(triangleSize.toDouble(), h - triangleSize.toDouble())
        shape.lineTo(borderThickness / 2.0, h.toDouble())
        shape.lineTo(borderThickness / 2.0, cornerRadius.toDouble())

        // Draw shadow
        g2.color = shadowColor
        g2.fill(
            shape.createTransformedShape(
                AffineTransform.getTranslateInstance(shadowSize.toDouble(), shadowSize.toDouble())
            )
        )

        // Draw main shape
        g2.color = background
        g2.fill(shape)

        // Draw border
        g2.color = borderColor
        g2.stroke = BasicStroke(borderThickness.toFloat())
        g2.draw(shape)
    }
}