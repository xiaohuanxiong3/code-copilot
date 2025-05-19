package com.github.xiaohuanxiong3.codecopilot.util

import com.github.xiaohuanxiong3.codecopilot.completion.DocumentChangedTrigger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key

/**
 * @Author Handsome Young
 * @Date 2025/5/19 22:43
 */
val DOCUMENT_CHANGED_TRIGGER_KEY = Key.create<DocumentChangedTrigger>("DOCUMENT_CHANGED_TRIGGER")

fun Document.deleteString(startOffset: Int, endOffset: Int, documentChangedTrigger: DocumentChangedTrigger) {
    putUserData(DOCUMENT_CHANGED_TRIGGER_KEY, documentChangedTrigger)
    deleteString(startOffset, endOffset)
    putUserData(DOCUMENT_CHANGED_TRIGGER_KEY, DocumentChangedTrigger.OTHERS)
}

fun Document.insertString(offset: Int, text: String, documentChangedTrigger: DocumentChangedTrigger) {
    putUserData(DOCUMENT_CHANGED_TRIGGER_KEY, documentChangedTrigger)
    insertString(offset, text)
    putUserData(DOCUMENT_CHANGED_TRIGGER_KEY, DocumentChangedTrigger.OTHERS)
}