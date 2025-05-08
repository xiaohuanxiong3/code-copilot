package com.github.xiaohuanxiong3.codecopilot.util

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.ui.JBColor

/**
 * @Author Handsome Young
 * @Date 2025/1/18 22:53
 */
object EditorUtil {

    fun isTerminal(editor: Editor): Boolean {
        return editor.javaClass.name.contains("Terminal")
    }

    fun createTextAttributesKey(name: String, color: Int, editor: Editor): TextAttributesKey {
        val attributes = TextAttributes().apply {
            backgroundColor = JBColor(color, color)
        }

        return TextAttributesKey.createTextAttributesKey(name).also {
            editor.colorsScheme.setAttributes(it, attributes)
        }
    }

    fun getPsiFile(editor: Editor): PsiFile? = editor.project?.let { PsiDocumentManager.getInstance(it) }?.getPsiFile(editor.document)

    // 获取 offset 对应行行前的缩进字符串长度
    fun getIndentLength(editor: Editor, offset: Int): Int {
        return getPsiFile(editor)?.let {
            val lineNumber = editor.document.getLineNumber(offset)
            getIndentLength(editor.document, lineNumber, CodeStyle.getIndentOptions(it) )
        } ?: throw RuntimeException("editor does not have a PsiFile")
    }

    private fun getIndentLength(document: Document, lineNumber: Int, indentOptions: CommonCodeStyleSettings.IndentOptions): Int {
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)

        val lineContent = document.getText(TextRange(lineStartOffset, lineEndOffset))

        var indentation = 0
        for (c in lineContent.toCharArray()) {
            if (c == ' ') {
                indentation++
            } else if (c == '\t') {
                // 查了一下制表符用途，发现在本场景中此分支不能进来，进来之后可能有问题
                indentation += indentOptions.INDENT_SIZE
            } else {
                break
            }
        }
        return indentation
    }

}