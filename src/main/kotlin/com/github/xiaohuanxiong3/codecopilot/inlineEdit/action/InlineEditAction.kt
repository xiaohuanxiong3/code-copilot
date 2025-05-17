package com.github.xiaohuanxiong3.codecopilot.inlineEdit.action

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.xiaohuanxiong3.codecopilot.AIServiceConfigProvider
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.InlineEditContext
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.InlineEditTool
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.diff.DiffHandler
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.diff.DiffService
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor.CustomPanel
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.editor.JComponentMaker
import com.github.xiaohuanxiong3.codecopilot.support.diff.MyUnifiedDiffUtils
import com.github.xiaohuanxiong3.codecopilot.support.editor.EditorComponentInlaysManager
import com.github.xiaohuanxiong3.codecopilot.support.langchain4j.AIServiceJsonResponse
import com.github.xiaohuanxiong3.codecopilot.support.langchain4j.httpclient.DeepSeekInlineCompletionHttpClientBuilder
import com.github.xiaohuanxiong3.codecopilot.util.EditorUtil
import com.github.xiaohuanxiong3.codecopilot.util.globalIOScope
import com.github.xiaohuanxiong3.codecopilot.util.objectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.launch
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComboBox
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * @Author Handsome Young
 * @Date 2025/1/18 22:51
 */
class InlineEditAction : AnAction(), DumbAware {

    private var inlineEditModel: OpenAiChatModel? = null
        get() {
            if (field == null) {
                field = AIServiceConfigProvider.inlineEditAIServiceConfig?.let { inlineEditConfig->
                    OpenAiChatModel.builder()
                        .httpClientBuilder(DeepSeekInlineCompletionHttpClientBuilder())
                        .baseUrl(inlineEditConfig.url)
                        .apiKey(inlineEditConfig.apiKey)
                        .modelName(inlineEditConfig.model)
                        .temperature(0.0)
                        .maxTokens(inlineEditConfig.maxTokens ?: 4096)
                        .responseFormat("json_object")
                        .build()
                }
            }
            return field
        }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
        e.presentation.isVisible = true
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val project = e.getData(PlatformDataKeys.PROJECT) ?: return
        openInlineEditor(project, editor)
    }

    private fun openInlineEditor(project: Project?, editor: Editor) {
        if (project == null) return

        // 在 terminal 中不打开内联编辑对话框
        if (EditorUtil.isTerminal(editor)) return

        // 限制 Editor 为 EditorEx，以便获取 JScrollPane
        if (editor !is EditorEx) return

        val manager = EditorComponentInlaysManager.from(editor, true)

        // Get list of model titles
        val modelTitles = mutableListOf<String>().apply {
            add("DeepSeek")
        }

        // Get indentation width in pixels
        val lineNumber = editor.document.getLineNumber(editor.caretModel.primaryCaret.offset)
        val lineStart = editor.document.getLineStartOffset(lineNumber)
        val lineEnd = editor.document.getLineEndOffset(lineNumber)
        val text = editor.document.getText(TextRange(lineStart, lineEnd))
        val indentation = text.takeWhile { it == ' ' }.length
        val charWidth =
            editor.contentComponent
                .getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))
                .charWidth(' ')
        val leftInset = (indentation - 1) * charWidth

        val inlayRef = Ref<Disposable>()
        val customPanelRef = Ref<CustomPanel>()

        // Create text area，attach key listener
        val textArea = JComponentMaker.makeTextArea()

        val comboBoxRef = Ref<JComboBox<String>>()

        val diffHandler =
            DiffHandler(
                project,
                editor,
                {
                    inlayRef.get()?.dispose()
                }
            )

        val diffService = project.service<DiffService>()
        diffService.register(diffHandler, editor)

        val returnDiffPatch = false

        fun onEnter() {
            if (textArea.text.isNotBlank()) {
                customPanelRef.get().enter()

                globalIOScope.launch {
                    try {
                        inlineEditModel?.chat(
                            UserMessage(InlineEditTool.build(editor.document.text, textArea.text, returnDiffPatch))
                        )?.let { chatResponse ->
                            val response = objectMapper.readValue(chatResponse.aiMessage().text(), AIServiceJsonResponse::class.java).r
                            editor.project?.let { project ->
                                val inlineEditContext = InlineEditContext(
                                    editor.virtualFile.path,
                                    editor.document.text
                                )
                                if (!returnDiffPatch) {
                                    MyUnifiedDiffUtils.handleUnifiedDiff(inlineEditContext,
                                        UnifiedDiffUtils.generateUnifiedDiff(
                                            "o.txt",
                                            "r.txt",
                                            editor.document.text.lines(),
                                            DiffUtils.diff(
                                                editor.document.text.lines(),
                                                response.lines()
                                            ),
                                            3
                                        ).joinToString("\n"))
                                } else {
                                    MyUnifiedDiffUtils.handleUnifiedDiff(inlineEditContext, response)
                                }
                                diffHandler.applyDiffLinesToEditor(inlineEditContext) {
                                    customPanelRef.get().finish()
                                }
                            }
                        }
                    } finally {
                        customPanelRef.get().finish()
                    }

                }
            }
        }

        val panel = JComponentMaker.makePanel(
            project,
            customPanelRef,
            textArea,
            inlayRef,
            comboBoxRef,
            leftInset,
            modelTitles,
            { onEnter() },
            {
                /* onCancel */
                diffService.reject(editor)
            },
            {
                /* onAccept */
                diffService.accept(editor)
            },
            {
                /* onReject */
                diffService.reject(editor)
                inlayRef.get().dispose()
            }
        )

        val inlay = manager.insert(lineNumber, panel, true)

        panel.revalidate()
        inlayRef.set(inlay)
        val viewport = (editor as? EditorImpl)?.scrollPane?.viewport
        viewport?.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))

        // Add key listener to text area
        textArea.addKeyListener(object : KeyAdapter() {

            override fun keyTyped(e: KeyEvent?) {
                if (customPanelRef.get().isFinished) {
                    customPanelRef.get().setup()
                }
            }

            override fun keyPressed(e: KeyEvent) {
                when(e.keyCode) {
                    KeyEvent.VK_ESCAPE -> {
                        // todo
                        diffService.reject(editor)
                        inlayRef.get()?.let { Disposer.dispose(it) }
                    }

                    KeyEvent.VK_ENTER -> {
                        when(e.modifiersEx) {
                            KeyEvent.SHIFT_DOWN_MASK -> {

                            }

                            0 -> {
                                onEnter()
                                e.consume()
                            }
                        }
                    }
                }
            }

            // We need this because backspace/delete is not registering properly on keyPressed for an
            // unknown reason
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE) {
                    if (customPanelRef.get().isFinished) {
                        customPanelRef.get().setup()
                    }
                }
            }

        })

        // Listen for changes to textarea line count
        textArea.document.addDocumentListener(
            object : DocumentListener {
                private var lastNumLines: Int = 0

                private fun updateSize() {
                    val numLines = textArea.text.lines().size
                    if (numLines != lastNumLines) {
                        lastNumLines = numLines
                        viewport?.dispatchEvent(ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED))
                    }
                }

                override fun insertUpdate(e: DocumentEvent?) {
                    updateSize()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    updateSize()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    updateSize()
                }

            })

        textArea.requestFocus()
    }

}