package com.github.xiaohuanxiong3.codecopilot.completion.render

import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

/**
 * @Author Handsome Young
 * @Date 2024/11/25 21:36
 */
class InlineCompletionRenderer {

    data class RenderingContext(
        val id : String,
        val editor: Editor,
        val offset:  Int,
        val extraCodeBlock: String?,
        val mainCodeBlock: String?,
        val inlays: List<Inlay<*>>
    )

    var current: RenderingContext? = null
        private set

    fun show(completionContentHolder: InlineCompletionContext.CompletionContentHolder) {
        current?.let {
            it.inlays.forEach(Disposer::dispose)
            current = null
        }
        if (completionContentHolder.editor.caretModel.offset != completionContentHolder.offset) {
            return
        }
        completionContentHolder.completionContent?.let { completionContent ->
            val inlays = mutableListOf<Inlay<*>>()
            if (completionContentHolder.lineSuffixStartOffset != -1) {
                val extraCodeBlock = completionContent.substring(0, completionContentHolder.lineSuffixStartOffset)
                renderCodeBlock(
                    completionContentHolder.editor,
                    completionContentHolder.offset,
                    extraCodeBlock,
                    inlays)
                val mainCodeBlockStartOffset = completionContentHolder.offset + completionContentHolder.lineSuffixLength
                val mainCodeBlock =
                    if(completionContent.length > completionContentHolder.lineSuffixStartOffset + completionContentHolder.lineSuffixLength)
                        completionContent.substring(completionContentHolder.lineSuffixStartOffset + completionContentHolder.lineSuffixLength).apply {
                            renderCodeBlock(
                                completionContentHolder.editor,
                                mainCodeBlockStartOffset,
                                this,
                                inlays
                            )
                        }
                    else null
                current = RenderingContext(completionContentHolder.completionId, completionContentHolder.editor, completionContentHolder.offset, extraCodeBlock, mainCodeBlock, inlays)
            } else {
                renderCodeBlock(
                    completionContentHolder.editor,
                    completionContentHolder.offset,
                    completionContent,
                    inlays)
                current = RenderingContext(completionContentHolder.completionId, completionContentHolder.editor, completionContentHolder.offset, null, completionContent, inlays)
            }
        }
    }

    private fun renderCodeBlock(editor: Editor, offset: Int, codeBlock: String, inlays: MutableList<Inlay<*>>) {
        val textLines = codeBlock.lines()

        createInlayText(editor, textLines[0], offset, 0)?.let { inlays.add(it) }

        if (textLines.size > 1) {
            textLines.forEachIndexed { index, line ->
                if (index > 0) {
                    createInlayText(editor, line, offset, index)?.let { inlays.add(it) }
                }
            }
        }

    }

    fun hide() {
        current?.let {
            invokeLater {
                it.inlays.forEach(Disposer::dispose)
            }
            current = null
        }
    }

    private fun createInlayText(editor: Editor, text: String, offset: Int, lineOffset: Int): Inlay<*>? {
        val renderer = object : EditorCustomElementRenderer {

            override fun getContextMenuGroupId(inlay: Inlay<*>): String? {
                return "CodeCopilot.InlineCompletionContextMenu"
            }

            override fun calcWidthInPixels(inlay: Inlay<*>): Int {
                return maxOf(getWidth(inlay.editor, text), 1)
            }

            override fun paint(inlay: Inlay<*>, graphics: Graphics, targetRect: Rectangle, textAttributes: TextAttributes) {
                graphics.font = getFont(inlay.editor)
                graphics.color = JBColor.GRAY
                graphics.drawString(text, targetRect.x, targetRect.y + inlay.editor.ascent)
            }

            private fun getFont(editor: Editor): Font {
                return editor.colorsScheme.getFont(EditorFontType.ITALIC).let {
                    UIUtil.getFontWithFallbackIfNeeded(it, text).deriveFont(editor.colorsScheme.editorFontSize)
                }
            }

            private fun getWidth(editor: Editor, line: String): Int {
                val font = getFont(editor)
                val metrics = FontInfo.getFontMetrics(font, FontInfo.getFontRenderContext(editor.contentComponent))
                return metrics.stringWidth(line)
            }
        }

        return if (lineOffset == 0) {
            editor.inlayModel.addInlineElement(offset, true, renderer)
        } else {
            editor.inlayModel.addBlockElement(offset, true, false, -lineOffset, renderer)
        }
    }
}