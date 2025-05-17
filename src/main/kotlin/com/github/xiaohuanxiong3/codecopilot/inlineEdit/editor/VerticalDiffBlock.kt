package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.MyEditorComponentManager
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.context.InlineCompletionRendererContext
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.MyRenderer
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.RendererType
import com.github.xiaohuanxiong3.codecopilot.util.EditorUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JTextArea
import kotlin.math.min

/**
 * @Author Handsome Young
 * @Date 2025/1/26 19:14
 */
class VerticalDiffBlock(
    private val editor: EditorEx,
    private val project: Project,
    var startLine: Int,
    private val onAcceptReject: (VerticalDiffBlock, Boolean) -> Unit
) {

    val deletedLines: MutableList<String> = mutableListOf()
    var deletedLineCount = 0
    val addedLines: MutableList<String> = mutableListOf()
    private val acceptButton: JButton
    private val rejectButton: JButton
    var deletionInlay: Inlay<MyRenderer>? = null
    private val greenKey = EditorUtil.createTextAttributesKey("CODE_COPILOT_DIFF_NEW_LINE", 0x3000FF00, editor)

    init {
        val (acceptBtn, rejectBtn) = createButtons()

        acceptButton = acceptBtn
        rejectButton = rejectBtn
    }

    fun handleAccept() {
        onAccept()
        onAcceptReject(this@VerticalDiffBlock, true)
    }

    fun handleReject() {
        onReject()
        onAcceptReject(this@VerticalDiffBlock, false)
    }

    fun onAccept() {
        clearEditorUI()
        acceptDiff()
    }

    fun onReject() {
        clearEditorUI()
        revertDiff()
    }

    // 编辑器可见区域发生变化时触发
    fun onVisibleAreaChange() {
        updateButtonsLocation()
    }

    fun updateButtonsVisible(isVisible: Boolean) {
        if (isVisible) {
            acceptButton.isVisible = true
            rejectButton.isVisible = true
        } else {
            acceptButton.isVisible = false
            rejectButton.isVisible = false
        }
    }

    private fun acceptDiff() {
        clearEditorUI()
    }

    private fun revertDiff() {
        WriteCommandAction.runWriteCommandAction(project) {
            val startOffset = editor.document.getLineStartOffset(startLine)
            // Delete the added lines
            if (addedLines.isNotEmpty()) {
                val endOffset = editor.document.getLineEndOffset(startLine + addedLines.size - 1) + 1
                editor.document.deleteString(startOffset, endOffset)
            }

            // Add the deleted lines back
            if (deletedLines.isNotEmpty()) {
                editor.document.insertString(startOffset, deletedLines.joinToString("\n") + "\n")
            }
        }
    }

    fun clearEditorUI() {
        deletionInlay?.let {
            // Ensure that dispose is executed on EDT
            if (application.isDispatchThread) {
                it.dispose()
            } else {
                invokeLater { it.dispose() }
            }
        }
        removeHighlighters()
        removeButtons()
    }

    private fun removeHighlighters() {
        val highlightersToRemove = editor.markupModel.allHighlighters.filter { highlighter ->
            val highlighterLine = editor.document.getLineNumber(highlighter.startOffset)
            highlighterLine in startLine until (startLine + addedLines.size)
        }

        highlightersToRemove.forEach { editor.markupModel.removeHighlighter(it) }
    }

    private fun removeButtons() {
        editor.contentComponent.remove(acceptButton)
        editor.contentComponent.remove(rejectButton)

        refreshEditor()
    }

    fun addNewLine(text: String, line: Int) {
        if (line == editor.document.lineCount) {
            editor.document.insertString(editor.document.textLength, "\n")
        }

        val offset = editor.document.getLineStartOffset(line)

        editor.document.insertString(offset, text + "\n")
        editor.markupModel.addLineHighlighter(greenKey, line, HighlighterLayer.LAST)

        addedLines.add(text)
    }

    fun deleteLineAt(line: Int) {
        val startOffset = editor.document.getLineStartOffset(line)
        val endOffset = min(editor.document.getLineEndOffset(line) + 1, editor.document.textLength)
        val deletedText = editor.document.getText(TextRange(startOffset, endOffset))

        deletedLines.add(deletedText.trimEnd())

        editor.document.deleteString(startOffset, endOffset)
    }

    fun onLastDiffLine() {

        if (deletedLines.isNotEmpty()) {
            renderDeletedLinesInlay()
        }

        // add buttons to editor
        editor.contentComponent.add(acceptButton)
        editor.contentComponent.add(rejectButton)
        editor.contentComponent.setComponentZOrder(acceptButton, 0)
        editor.contentComponent.setComponentZOrder(rejectButton, 0)

        // render buttons
        updateButtonsLocation()
        refreshEditor()
    }

    fun updatePosition(newLineNumber: Int) {
        startLine = newLineNumber

        updateButtonsLocation()
        refreshEditor()
    }

    private fun renderDeletedLinesInlay() {
        createDeletionTextComponent(deletedLines.joinToString("\n")).let {
            deletionInlay = MyEditorComponentManager.addComponent(
                editor,
                InlineCompletionRendererContext(it),
                editor.document.getLineStartOffset(startLine),
                RendererType.INLINE_COMPLETION)
        }
    }

    private fun createDeletionTextComponent(text: String) = JTextArea(text).apply {
        isEditable = false
        background = JBColor(0x30FF0000, 0x30FF0000)
        foreground = JBColor.GRAY
        border = JBUI.Borders.empty()
        lineWrap = false
        wrapStyleWord = false
        // 解决中文乱码问题
        font = editor.colorsScheme.getFont(EditorFontType.PLAIN).let {
            UIUtil.getFontWithFallbackIfNeeded(it, text)
        }
    }

    private fun updateButtonsLocation() {
        val (x, y) = getButtonsXYPositions()

        rejectButton.setBounds(
            x,
            y,
            rejectButton.preferredSize.width,
            rejectButton.preferredSize.height
        )

        acceptButton.setBounds(
            x + rejectButton.width + editor.scrollPane.verticalScrollBar.size.width,
            y,
            acceptButton.preferredSize.width,
            acceptButton.preferredSize.height
        )
    }

    private fun getButtonsXYPositions(): Pair<Int, Int> {
        val visibleArea = editor.scrollingModel.visibleArea
        val lineStartPosition = editor.logicalPositionToXY(LogicalPosition(startLine - 1, 0))

        val xPosition =
            min(visibleArea.x + visibleArea.width, editor.scrollPane.width) - acceptButton.preferredSize.width - rejectButton.preferredSize.width - 2 * editor.scrollPane.verticalScrollBar.size.width
        val yPosition = lineStartPosition.y + editor.lineHeight

        return Pair(xPosition, yPosition)
    }

    private fun createButtons(): Pair<JButton, JButton> {
        val rejectButton =
            createButton(
                text = "REJECT",
                backgroundColor = JBColor(0x99FF0000.toInt(), 0x99FF0000.toInt())
            ).apply {
                addActionListener {
                    handleReject()
                }
                isVisible = false
            }

        val acceptButton =
            createButton(
                text = "ACCEPT",
                backgroundColor = JBColor(0x7700BB00.toInt(), 0x7700BB00.toInt())
            ).apply {
                addActionListener {
                    handleAccept()
                }
                isVisible = false
            }

        return Pair(acceptButton, rejectButton)
    }

    private fun createButton(text: String, backgroundColor: JBColor): JButton {
        return object : JButton(text) {
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = backgroundColor
                g2.fillRoundRect(0, 0, width - 1, height - 1, 4, 4)
                super.paintComponent(g2)
                g2.dispose()
            }
        }.apply {
            foreground = Color(240, 240, 240)
            font = Font("Arial", Font.BOLD, 9)
            isContentAreaFilled = false
            isOpaque = false
            border = BorderFactory.createEmptyBorder(4, 2, 4, 2)
            preferredSize = Dimension(preferredSize.width - 30, 14)
            cursor = Cursor(Cursor.HAND_CURSOR)
        }
    }

    private fun refreshEditor() {
        editor.contentComponent.revalidate()
        editor.contentComponent.repaint()
    }
}