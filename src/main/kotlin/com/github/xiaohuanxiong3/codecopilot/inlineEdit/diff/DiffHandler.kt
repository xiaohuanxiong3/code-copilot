package com.github.xiaohuanxiong3.codecopilot.inlineEdit.diff

import com.github.xiaohuanxiong3.codecopilot.inlineEdit.DiffLineType
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.InlineEditContext
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor.VerticalDiffBlock
import com.github.xiaohuanxiong3.codecopilot.util.EditorUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseMotionListener
import kotlin.math.min

/**
 * @Author Handsome Young
 * @Date 2025/1/27 21:08
 */
class DiffHandler(
    private val project: Project,
    private val editor: EditorEx,
    private val onClose: () -> Unit
){

    private data class CurLineState(
        var index: Int,
        var highlighter: RangeHighlighter? = null,
        var diffBlock: VerticalDiffBlock? = null
    )

    private var curLine = CurLineState(0)

    private val diffBlocks: MutableList<VerticalDiffBlock> = mutableListOf()
    private val curLineKey = EditorUtil.createTextAttributesKey("CODE_COPILOT_CURRENT_LINE", 0x40888888, editor)

    private var myVisibleAreaChangeHandler : VisibleAreaListener? = null

    private var myMouseMotionListener: MouseMotionListener? = null

    fun applyDiffLinesToEditor(inlineEditContext: InlineEditContext, callback: () -> Unit) {
        invokeLater {
            inlineEditContext.diffLines?.forEach { diffLine ->
                WriteCommandAction.runWriteCommandAction(project) {
                    handleDiffLine(diffLine.type, diffLine.text)
                }
            }

            cleanupProgressHighlighters()

            initAndAddVisibleAreaChangeHandler()
            initAndAddMouseMotionListener()

            callback()
        }
    }

    /**
     * 给编辑器添加临时的 VisibleAreaListener
     */
    private fun initAndAddVisibleAreaChangeHandler() {
        myVisibleAreaChangeHandler = VisibleAreaListener {
            diffBlocks.forEach {
                it.onVisibleAreaChange()
            }
            editor.contentComponent.revalidate()
            editor.contentComponent.repaint()
        }
        myVisibleAreaChangeHandler?.let { editor.scrollingModel.addVisibleAreaListener(it) }
    }

    /**
     * 给编辑器添加临时的 MouseMotionListener
     */
    private fun initAndAddMouseMotionListener() {
        myMouseMotionListener = object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val curPoint = e.point
                val curLogicalPosition = editor.xyToLogicalPosition(curPoint)
                val curLine = curLogicalPosition.line
                diffBlocks.forEach {
                    if (it.deletionInlay != null) {
                        it.deletionInlay!!.bounds?.let { bounds ->
                            if (bounds.contains(curPoint)) {
                                it.updateButtonsVisible(true)
                            } else {
                                it.updateButtonsVisible(curLine in it.startLine until it.startLine + it.addedLines.size)
                            }
                        } ?: {
                            it.updateButtonsVisible(curLine in it.startLine until it.startLine + it.addedLines.size)
                        }
                    } else {
                        it.updateButtonsVisible(curLine in it.startLine until it.startLine + it.addedLines.size)
                    }
                }
                editor.contentComponent.revalidate()
                editor.contentComponent.repaint()
            }
        }
        myMouseMotionListener?.let { editor.contentComponent.addMouseMotionListener(it) }
    }

    private fun handleDiffLine(diffLineType: DiffLineType, lineText: String) {
        try {
            when (diffLineType) {
                DiffLineType.SAME -> {
                    handleSameLine()
                }
                DiffLineType.NEW -> {
                    handleNewLine(lineText)
                }
                DiffLineType.OLD -> {
                    handleOldLine()
                }
            }

            updateProgressHighlighters(diffLineType)
        } catch (e: Exception) {
            thisLogger().info("""
                Error handling diff line -
                Line index: ${curLine.index}, 
                Line type: $diffLineType, 
                Line text:  $lineText
                Error message: ${e.message}
            """.trimIndent())
        }

    }

    private fun handleSameLine() {
        if (curLine.diffBlock != null) {
            curLine.diffBlock!!.onLastDiffLine()
        }

        curLine.diffBlock = null
        curLine.index++
    }

    private fun handleNewLine(text: String) {
        if (curLine.diffBlock == null) {
            curLine.diffBlock = createDiffBlock()
        }

        curLine.diffBlock!!.addNewLine(text, curLine.index)

        curLine.index++
    }

    private fun createDiffBlock(): VerticalDiffBlock {
        val diffBlock = VerticalDiffBlock(
            editor = editor,
            project = project,
            startLine = curLine.index,
            onAcceptReject = ::handleDiffBlockAcceptOrReject
        )

        diffBlocks.add(diffBlock)

        return diffBlock
    }

    private fun handleDiffBlockAcceptOrReject(diffBlock: VerticalDiffBlock, didAccept: Boolean) {
        diffBlocks.remove(diffBlock)

        // handle didAccept or didReject
        if (diffBlocks.isNotEmpty()) {
            if (didAccept) {
                updatePositionsOnAccept(diffBlock.startLine, diffBlock.addedLines.size, diffBlock.deletedLines.size)
            } else {
                updatePositionsOnReject(diffBlock.startLine, diffBlock.addedLines.size, diffBlock.deletedLines.size)
            }
        } else {
            resetState()
        }
    }

    private fun updatePositionsOnAccept(startLine: Int, numAdditions: Int, numDeletions: Int) {
//        val offset = - numDeletions
        updatePositions(startLine, 0)
    }

    private fun updatePositionsOnReject(startLine: Int, numAdditions: Int, numDeletions: Int) {
        val offset = - numAdditions + numDeletions
        updatePositions(startLine, offset)
    }

    private fun updatePositions(startLine: Int, offset: Int) {
        diffBlocks.forEach { block ->
            if (block.startLine > startLine) {
                block.updatePosition(block.startLine + offset)
            }
        }
    }

    private fun handleOldLine() {
        if (curLine.diffBlock == null) {
            curLine.diffBlock = createDiffBlock()
        }

        curLine.diffBlock!!.deleteLineAt(curLine.index)
//        curLine.index++
    }

    private fun updateProgressHighlighters(type: DiffLineType) {
        // update the highlighter to show the current line
        curLine.highlighter?.let { editor.markupModel.removeHighlighter(it) }
        curLine.highlighter = editor.markupModel.addLineHighlighter(
            curLineKey,
            min(editor.document.lineCount - 1, curLine.index),
            HighlighterLayer.LAST
        )

        // Remove the unfinished lines highlighter
        // 暂时不需要
    }

    private fun cleanupProgressHighlighters() {
        curLine.highlighter?.let { editor.markupModel.removeHighlighter(it) }
    }

    fun acceptAll() {
        try {
            diffBlocks.forEach {
                it.onAccept()
                updatePositionsOnAccept(it.startLine, it.addedLines.size, it.deletedLineCount)
            }
        } catch (e: Exception) {
            thisLogger().info("Error accepting all diffs: ${e.message}")
        }
        resetState()
    }

    fun rejectAll() {
        try {
            diffBlocks.forEach {
                it.onReject()
                updatePositionsOnReject(it.startLine, it.addedLines.size, it.deletedLines.size)
            }
        } catch (e: Exception) {
            thisLogger().info("Error rejecting all diffs: ${e.message}")
        }
        resetState()
    }

    // todo 可能改个名字比较好
    private fun resetState() {
        // Clear state vars
        diffBlocks.clear()
        curLine = CurLineState(0)

        // Close the Edit input
        onClose()

        // remove the temporary VisibleAreaListener
        myVisibleAreaChangeHandler?.let { editor.scrollingModel.removeVisibleAreaListener(it) }
        myVisibleAreaChangeHandler = null
        myMouseMotionListener?.let { editor.contentComponent.removeMouseMotionListener(it) }
        myMouseMotionListener = null
    }

}