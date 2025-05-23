package com.github.xiaohuanxiong3.codecopilot.completion

import com.github.xiaohuanxiong3.codecopilot.util.DOCUMENT_CHANGED_TRIGGER_KEY
import com.github.xiaohuanxiong3.codecopilot.util.globalIOScope
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.psi.PsiDocumentManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Author Handsome Young
 * @Date 2024/11/27 23:32
 */
private var documentChangedListenerJob : Job? = null

class InlineCompletionCaretListener(private val inlineCompletionService: InlineCompletionService) : CaretListener {

    override fun caretPositionChanged(event: CaretEvent) {
        val caret = event.caret ?: return
        val offset = caret.offset
        val editor = caret.editor

        val completionContext = inlineCompletionService.current
        if (completionContext != null && completionContext.isMatch(editor, offset)) {
            return
        }
        inlineCompletionService.dismiss()
    }
}

class InlineCompletionDocumentListener(private val editor: Editor, private val editorManager: FileEditorManager, private val inlineCompletionService: InlineCompletionService) : DocumentListener {

    override fun beforeDocumentChange(event: DocumentEvent) {
        if (editor == editorManager.selectedTextEditor) {
            if (event.newFragment.isEmpty()) {
                if (!event.oldFragment.isEmpty()) {
                    inlineCompletionService.current?.let { current ->
                        // 当前有补全内容时，若用户删除内容，不重新请求大模型进行补全，而是修正现有补全内容后重新渲染补全内容
                        // 感觉没有必要，用户删除内容之后
                        // 如果重新输入之前的补全内容，emmm，对于纠结症患者来说可能有用
                        // 如果输入的内容与之前的补全内容不相同，则重新请求大模型进行补全
                        // 这么来看，还是有必要的
                        if (current.completionContentHolder.handleBackSpace(event.oldFragment.toString()) {
                                editor.project?.let { project ->
                                    PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
                                        invokeLater {
                                            inlineCompletionService.renderCurrentCompletion()
                                        }
                                    }
                                }
                            }) {
                            return
                        } else {
                            inlineCompletionService.dismiss()
                        }
                    }
                } else {
                    if (inlineCompletionService.current == null || inlineCompletionService.current!!.completionContentHolder.completionContent == null) inlineCompletionService.dismiss()
                }
            }
        }
    }

    override fun documentChanged(event: DocumentEvent) {
        if (editor == editorManager.selectedTextEditor) {
            documentChangedListenerJob?.cancel()

            // 判断是什么原因触发文档发生变化，决定是否需要触发补全
            DOCUMENT_CHANGED_TRIGGER_KEY.get(event.document)?.let {
                if (it == DocumentChangedTrigger.INLINE_EDIT) return
            }

            inlineCompletionService.current?.let { current ->
                if (!event.newFragment.isEmpty()) {
                    // 用户输入匹配当前补全内容的前缀时，更新补全内容上下文，然后返回（不重新请求大模型进行补全）
                    if (current.completionContentHolder.handleUserInput(event.newFragment.toString()) {
                            editor.project?.let { project ->
                                PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
                                    invokeLater {
                                        inlineCompletionService.renderCurrentCompletion()
                                    }
                                }
                            }
                        })
                    {
                        return
                    }
                } else if (!event.oldFragment.isEmpty()) {
                    // 匹配场景：用户删除内容后，不需重新请求大模型进行补全
                    if (!current.completionContentHolder.completionContent.isNullOrBlank()) {
                        return
                    }
                }
            }

            documentChangedListenerJob = globalIOScope.launch {
                delay(500)
                editor.project?.let { project ->
                    PsiDocumentManager.getInstance(project).performForCommittedDocument(editor.document) {
//                        invokeLater {
                            inlineCompletionService.triggerInlineCompletion(editor, runReadAction { editor.caretModel.offset } )
//                        }
                    }
                }
            }
        }
    }

}

class InlineCompletionEditorListener : EditorFactoryListener{
    private val disposables = mutableMapOf<Editor, () -> Unit>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        val editorManager = project.let { FileEditorManager.getInstance(it) } ?: return

        val inlineCompletionService = project.service<InlineCompletionService>()

        // listen to caret position change
        val caretListener = InlineCompletionCaretListener(inlineCompletionService)
        editor.caretModel.addCaretListener(caretListener)

        // listen to editor selection change
        val connection = editor.project?.messageBus?.connect()
        connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                inlineCompletionService.dismiss()
            }
        })

        // listen to document change
        val documentListener = InlineCompletionDocumentListener(editor, editorManager, inlineCompletionService)
        editor.document.addDocumentListener(documentListener)

        disposables[editor] = {
            editor.caretModel.removeCaretListener(caretListener)
            connection?.disconnect()
            editor.document.removeDocumentListener(documentListener)
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val disposable = disposables[editor]
        disposable?.invoke()
        disposables.remove(editor)
    }
}