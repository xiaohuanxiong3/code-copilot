package com.github.xiaohuanxiong3.codecopilot.completion

import com.github.xiaohuanxiong3.codecopilot.AIServiceConfigProvider
import com.github.xiaohuanxiong3.codecopilot.completion.context.ImportContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.PackageContext
import com.github.xiaohuanxiong3.codecopilot.completion.render.InlineCompletionRenderer
import com.github.xiaohuanxiong3.codecopilot.completion.validation.ValidationUtil
import com.github.xiaohuanxiong3.codecopilot.support.langchain4j.httpclient.DeepSeekInlineCompletionHttpClientBuilder
import com.github.xiaohuanxiong3.codecopilot.util.EditorUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

/**
 * @Author Handsome Young
 * @Date 2024/11/25 22:38
 */
data class InlineCompletionContext(
    val requestContext: RequestContext,
    val completionContentHolder: CompletionContentHolder,
    val job: Job?
) {

    data class RequestContext(
        /**
         * 补全ID
         */
        val completionId: String,
        /**
         * 编辑器
         */
        val editor: Editor,
        /**
         * 光标偏移量
         */
        val offset: Int,
        /**
         * 光标所在行的结束偏移量
         */
        val lineEnd: Int?,
        /**
         * 光标所在行从行开头到光标所处位置的文本
         */
        val linePrefix: String?,
        /**
         * 光标所在行从光标所处位置到行结尾的文本
         */
        val lineSuffix: String?,
        /**
         * 光标所在行从光标所处位置到行结尾的文本满足特殊处理要求
         */
        val lineSuffixMatch: Boolean,
        /**
         * 光标处对应的代码前缀
         */
        val prefix: String?,
        /**
         * 光标处对应的代码后缀
         */
        val suffix: String?,
        /**
         * 文件对应的代码上下文
         */
        val codeContext: Map<String, String>?,
        ) {

        companion object {
            fun from(editor: Editor, offset: Int) : RequestContext{
                val completionId = UUID.randomUUID().toString()

                val lineStart = editor.document.getLineStartOffset(editor.caretModel.primaryCaret.logicalPosition.line)
                val lineEnd = editor.document.getLineEndOffset(editor.caretModel.primaryCaret.logicalPosition.line)
                val linePrefix = if (offset == lineStart) "" else editor.document.getText(TextRange(lineStart, offset))
                val lineSuffix = if (offset == lineEnd) null else editor.document.getText(TextRange(offset, lineEnd))
                val lineSuffixMatch = match(lineSuffix)
                val prefix = editor.document.getText(TextRange(0, lineStart))
                val suffix = if (lineSuffixMatch) editor.document.getText(TextRange(lineEnd, editor.document.textLength)) else editor.document.getText(TextRange(offset, editor.document.textLength))

                val psiFile = editor.project?.let { PsiDocumentManager.getInstance(it).getPsiFile(editor.document) }
                val codeContext = mutableMapOf<String,String>()
                psiFile?.let {
                    PackageContext.attach(it, codeContext)
                    ImportContext.attach(it, codeContext)
                }

                return RequestContext(completionId, editor, offset, lineEnd, linePrefix, lineSuffix, lineSuffixMatch, prefix, suffix, codeContext)
            }

            private fun match(lineSuffix: String?) : Boolean {
                return !lineSuffix.isNullOrBlank() && lineSuffix.length <= 3
            }
        }

    }

    data class CompletionContentHolder(
        val completionId: String,
        val editor: Editor,
        var offset: Int,
        /**
         * 注意：此offset不包括起始行行前的缩进
         */
        val completionContentStartLineOffset: Int,
        var completionContent: String? = null,
        /**
         * 当前补全内容中光标所在行的后缀文本的起始偏移量
         */
        var lineSuffixStartOffset: Int = -1,
        var lineSuffixLength: Int = 0,
        var partialAccepted: Boolean = false
    ) {

        fun withPartialAccept(acceptType: AcceptType, callBack: () -> Unit) {
            if (completionContent.isNullOrBlank()) {
                return
            }
            when (acceptType) {
                AcceptType.FULL_COMPLETION -> {
                    if (lineSuffixStartOffset == -1) {
                        val tOffset = offset
                        val tCompletionContent = completionContent!!
                        invokeLater {
                            WriteCommandAction.runWriteCommandAction(editor.project) {
                                editor.document.insertString(tOffset, tCompletionContent)
                                editor.caretModel.moveToOffset(tOffset + tCompletionContent.length)
                            }
                            callBack()
                        }
                        offset += completionContent!!.length
                        completionContent = null
                    } else {
                        val tOffset = offset
                        val tCompletionContent = completionContent!!
                        invokeLater {
                            WriteCommandAction.runWriteCommandAction(editor.project) {
                                editor.document.replaceString(tOffset, tOffset + lineSuffixLength, tCompletionContent)
                                editor.caretModel.moveToOffset(tOffset + tCompletionContent.length)
                            }
                            callBack()
                        }
                        offset += completionContent!!.length
                        completionContent = null
                    }
                }
                AcceptType.NEXT_WORD -> {
                    val nextWord = Regex("\\w+|\\W+").find(completionContent!!)?.value ?: ""
                    if (lineSuffixStartOffset == -1) {
                        val tOffset = offset
                        invokeLater {
                            WriteCommandAction.runWriteCommandAction(editor.project) {
                                editor.document.insertString(tOffset, nextWord)
                                editor.caretModel.moveToOffset(tOffset + nextWord.length)
                            }
                            callBack()
                        }
                        offset += nextWord.length
                        completionContent = completionContent!!.substring(nextWord.length)
                        partialAccepted = true
                    } else {
                        if (nextWord.length <= lineSuffixStartOffset) {
                            val tOffset = offset
                            invokeLater {
                                WriteCommandAction.runWriteCommandAction(editor.project) {
                                    editor.document.insertString(tOffset, nextWord)
                                    editor.caretModel.moveToOffset(tOffset + nextWord.length)
                                }
                                callBack()
                            }
                            offset += nextWord.length
                            completionContent = completionContent!!.substring(nextWord.length)
                            lineSuffixStartOffset -= nextWord.length
                            partialAccepted = true
                        } else {
                            val finalWordLength = nextWord.length.coerceAtLeast(lineSuffixStartOffset + lineSuffixLength)
                            val finalWord = completionContent!!.substring(0, finalWordLength)
                            val tOffset = offset
                            invokeLater {
                                WriteCommandAction.runWriteCommandAction(editor.project) {
                                    editor.document.replaceString(tOffset, tOffset + lineSuffixLength, finalWord)
                                    editor.caretModel.moveToOffset(tOffset + finalWord.length)
                                }
                                callBack()
                            }
                            offset += finalWord.length
                            completionContent = completionContent!!.substring(finalWord.length)
                            lineSuffixStartOffset = -1
                            partialAccepted = true
                        }
                    }
                }
                AcceptType.NEXT_LINE -> {
                    if (lineSuffixStartOffset == -1) {
                        val lines = completionContent!!.lines()
                        lines.first().ifBlank {
                            lines.subList(0, 2).joinToString("\n")
                        }.let { nextLine ->
                            val tOffset = offset
                            invokeLater {
                                WriteCommandAction.runWriteCommandAction(editor.project) {
                                    editor.document.insertString(tOffset, nextLine)
                                    editor.caretModel.moveToOffset(tOffset + nextLine.length)
                                }
                                callBack()
                            }
                            offset += nextLine.length
                            completionContent = completionContent!!.substring(nextLine.length)
                            partialAccepted = true
                        }
                    } else {
                        completionContent!!.lines().firstOrNull()?.let { firstLine ->
                            val tOffset = offset
                            invokeLater {
                                WriteCommandAction.runWriteCommandAction(editor.project) {
                                    editor.document.replaceString(tOffset, tOffset + lineSuffixLength, firstLine)
                                    editor.caretModel.moveToOffset(tOffset + firstLine.length)
                                }
                                callBack()
                            }
                            offset += firstLine.length
                            completionContent = completionContent!!.substring(firstLine.length)
                            lineSuffixStartOffset = -1
                            partialAccepted = true
                        }
                    }
                }
            }
        }

        fun handleUserInput(userInput: String, matchCallBack: () -> Unit) : Boolean {
            return completionContent?.let { content ->
                if (!content.startsWith(userInput)) return false
                val userInputLength = userInput.length
                offset += userInputLength
                completionContent = content.substring(userInputLength)
                if (lineSuffixStartOffset != -1) {
                    if (userInputLength <= lineSuffixStartOffset) {
                        lineSuffixStartOffset -= userInputLength
                    } else {
//                        if (userInputLength < lineSuffixStartOffset + lineSuffixLength) {
//
//                        } else {
//
//                        }
                        return false
                    }
                }
                matchCallBack()
                true
            } ?: false
        }

        fun handleBackSpace(backSpaceContent: String, matchCallBack: () -> Unit) : Boolean {
            if (completionContent.isNullOrBlank()) return false
            val newOffset = offset - backSpaceContent.length
            return if (newOffset < completionContentStartLineOffset) {
                    completionContent = null
                    false
                } else {
                    completionContent = backSpaceContent + completionContent
                    offset = newOffset
                    if (lineSuffixStartOffset != -1) {
                        lineSuffixStartOffset += backSpaceContent.length
                    }
                    matchCallBack()
                    true
                }
        }
    }

    fun isMatch(editor: Editor, offset: Int) : Boolean {
        return this.completionContentHolder.editor == editor && this.completionContentHolder.offset == offset
    }

}

enum class AcceptType {
    FULL_COMPLETION,
    NEXT_WORD,
    NEXT_LINE
}

@Service(Service.Level.PROJECT)
class InlineCompletionService(private val project: Project) : Disposable {

//    private val messageBusConnection = project.messageBus.connect()
//    private val editorManager = FileEditorManager.getInstance(project)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var renderer = InlineCompletionRenderer()
    var current : InlineCompletionContext? = null
    private val currentContextWriteLock = Object()
    private val widget: AutocompleteSpinnerWidget? by lazy {
        WindowManager.getInstance().getStatusBar(project).getWidget("AutocompleteSpinnerWidget") as? AutocompleteSpinnerWidget
    }
    private var autoCompletionModel: OpenAiChatModel? = null
        get() {
            if (field == null) {
                field = AIServiceConfigProvider.completionAIServiceConfig?.let { autoCompletionConfig->
                    OpenAiChatModel.builder()
                        .httpClientBuilder(DeepSeekInlineCompletionHttpClientBuilder())
                        .baseUrl(autoCompletionConfig.url)
                        .apiKey(autoCompletionConfig.apiKey)
                        .modelName(autoCompletionConfig.model)
                        .temperature(0.0)
                        .maxTokens(autoCompletionConfig.maxTokens ?: 4096)
                        .build()
                }
            }
            return field
        }

    fun isInlineCompletionVisibleAt(editor: Editor, offset: Int): Boolean =
        renderer.current?.editor == editor && renderer.current?.offset == offset

    fun triggerInlineCompletion(editor: Editor, offset: Int) {
        synchronized(currentContextWriteLock) {
            current?.let {
                it.job?.cancel()
                if (it.completionContentHolder.partialAccepted) {
                    return
                }
                renderer.hide()
                current = null
            }

            val requestContext = runReadAction { InlineCompletionContext.RequestContext.from(editor, offset) }
            val startLineOffset = runReadAction { editor.document.getLineStartOffset(editor.document.getLineNumber(offset)) +
                    EditorUtil.getIndentLength(editor, offset) }
            val job = scope.launch {
                widget?.setLoading(true)
                val completionId = requestContext.completionId
                try {
                    autoCompletionModel?.chat(
                        UserMessage(InlineCompletionTool.buildUserMessage(requestContext)),
                        AiMessage(requestContext.linePrefix!!)
                    )?.let { response ->
                        val completionContent = response.aiMessage().text()
                        current?.let { context ->
                            if (completionId == context.requestContext.completionId && completionContent.isNotBlank()) {
                                ValidationUtil.validateSuccessThen(
                                    project,
                                    context.requestContext,
                                    completionContent
                                ) {
                                    val line = context.requestContext.linePrefix
                                    context.completionContentHolder.completionContent =
                                        if (line != null && line.endsWith(" "))
                                            completionContent.trimStart() else completionContent
                                    if (context.requestContext.lineSuffixMatch) {
                                        val firstLine = context.completionContentHolder.completionContent!!.lines().firstOrNull()
                                        if (!firstLine.isNullOrBlank() && firstLine.lastIndexOf(context.requestContext.lineSuffix!!) != -1) {
                                            context.completionContentHolder.lineSuffixStartOffset = firstLine.lastIndexOf(context.requestContext.lineSuffix)
                                            context.completionContentHolder.lineSuffixLength = context.requestContext.lineSuffix.length
                                        } else {
                                            return@validateSuccessThen
                                        }
                                    }
                                    invokeLater {
                                        renderCurrentCompletion()
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    widget?.setLoading(false)
                }
            }
            current = InlineCompletionContext(requestContext, InlineCompletionContext.CompletionContentHolder(requestContext.completionId, editor, offset, startLineOffset), job)
        }
    }

    fun renderCurrentCompletion() {
        val context = current ?: return
        renderer.show(context.completionContentHolder)
    }

    fun accept(editor: Editor, offset: Int, acceptType: AcceptType) {
        synchronized(currentContextWriteLock) {
            val context = current ?: return
            if (!context.isMatch(editor, offset)) {
                return
            }
            context.completionContentHolder.withPartialAccept(acceptType) {
                renderCurrentCompletion()
            }
        }
    }

    fun dismiss() {
        synchronized(currentContextWriteLock) {
            renderer.current?.let {
                renderer.hide()
            }
            current?.let {
                it.job?.cancel()
                widget?.setLoading(false)
                current = null
            }
        }
    }

    override fun dispose() {
        dismiss()
//        messageBusConnection.dispose()
    }
}

