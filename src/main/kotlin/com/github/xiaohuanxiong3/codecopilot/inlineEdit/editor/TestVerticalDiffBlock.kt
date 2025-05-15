package com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor

import com.github.xiaohuanxiong3.codecopilot.support.editor.EditorComponentInlaysManager
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.MyEditorComponentManager
import com.github.xiaohuanxiong3.codecopilot.support.editor.componentManager.renderer.RendererType
import com.github.xiaohuanxiong3.codecopilot.util.EditorUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JTextArea
import kotlin.math.min

/**
 * @Author Handsome Young
 * @Date 2025/1/26 19:14
 */
class TestVerticalDiffBlock(
    private val editor: Editor,
    private val project: Project,
    var startLine: Int,
    private val onAcceptReject: (TestVerticalDiffBlock, Boolean) -> Unit,
    private val editorComponentInlaysManager: EditorComponentInlaysManager
) {

    val deletedLines: MutableList<String> = mutableListOf()
    var deletedLineCount = 0
    val addedLines: MutableList<String> = mutableListOf()
    private val acceptButton: JButton
    private val rejectButton: JButton
    private var deletionInlay: Disposable? = null
    // Used for calculation of the text area height when rendering buttons
    private var textComponent: JComponent? = null
    private val greenKey = EditorUtil.createTextAttributesKey("CODE_COPILOT_DIFF_NEW_LINE", 0x3000FF00, editor)
    private val redKey = EditorUtil.createTextAttributesKey("CODE_COPILOT_DIFF_OLD_LINE", 0x30FF0000, editor)

    init {
        val (acceptBtn, rejectBtn) = createButtons()

        acceptButton = acceptBtn
        rejectButton = rejectBtn
    }

    fun handleAccept() {
        onAccept()
        onAcceptReject(this@TestVerticalDiffBlock, true)
    }

    fun handleReject() {
        onReject()
        onAcceptReject(this@TestVerticalDiffBlock, false)
    }

    fun onAccept() {
        clearEditorUI()
        acceptDiff()
    }

    fun onReject() {
        clearEditorUI()
        revertDiff()
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

        // render buttons
        renderButtons()
    }

    fun updatePosition(newLineNumber: Int) {
        startLine = newLineNumber

        val (x, y) = getButtonsXYPositions()

        rejectButton.location = Point(x, y)
        acceptButton.location = Point(x + rejectButton.preferredSize.width + 5, y)

        refreshEditor()
    }

    private fun renderDeletedLinesInlay() {
        createDeletionTextComponent(deletedLines.joinToString("\n")).let {
            deletionInlay = MyEditorComponentManager.addComponent(
                editor as EditorEx,
                it,
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
        font = editor.colorsScheme.getFont(EditorFontType.PLAIN)

        this@TestVerticalDiffBlock.textComponent = this
    }

    private fun renderButtons() {
        val (x, y) = getButtonsXYPositions()

        rejectButton.setBounds(
            x,
            y,
            rejectButton.preferredSize.width,
            rejectButton.preferredSize.height
        )

        acceptButton.setBounds(
            x + rejectButton.width + 2,
            y,
            acceptButton.preferredSize.width,
            acceptButton.preferredSize.height
        )

        editor.contentComponent.add(acceptButton)
        editor.contentComponent.add(rejectButton)

        editor.contentComponent.setComponentZOrder(acceptButton, 0)
        editor.contentComponent.setComponentZOrder(rejectButton, 0)

        refreshEditor()
    }

    private fun getButtonsXYPositions(): Pair<Int, Int> {
        val visibleArea = editor.scrollingModel.visibleArea
        val textAreaHeight = this.textComponent?.height ?: 0
        val lineStartPosition = editor.logicalPositionToXY(LogicalPosition(startLine, 0))

        val xPosition =
            visibleArea.x + visibleArea.width - acceptButton.preferredSize.width - rejectButton.preferredSize.width - 20
        val yPosition = lineStartPosition.y - textAreaHeight

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
            }

        val acceptButton =
            createButton(
                text = "ACCEPT",
                backgroundColor = JBColor(0x7700BB00.toInt(), 0x7700BB00.toInt())
            ).apply {
                addActionListener {
                    handleAccept()
                }
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