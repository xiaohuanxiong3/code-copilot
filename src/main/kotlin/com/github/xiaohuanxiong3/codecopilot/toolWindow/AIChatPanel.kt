package com.github.xiaohuanxiong3.codecopilot.toolWindow

import com.github.xiaohuanxiong3.codecopilot.AIServiceConfigProvider
import com.github.xiaohuanxiong3.codecopilot.support.commonmark.Markdown
import com.github.xiaohuanxiong3.codecopilot.support.jcef.AsyncJBCefJSQuery
import com.github.xiaohuanxiong3.codecopilot.support.langchain4j.AIServiceJsonResponse
import com.github.xiaohuanxiong3.codecopilot.util.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModelReply
import dev.langchain4j.model.chat.chatFlow
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout


/**
 * @Author Handsome Young
 * @Date 2024/9/19 19:16
 */
class AIChatPanel(val project: Project) : JBPanel<AIChatPanel>(BorderLayout()) {

    private var jbCefBrowser = JBCefBrowser()

    private val devTools: CefBrowser = jbCefBrowser.cefBrowser.devTools

    private val chatQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val cancelChatQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val tabCommandQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val clearCommandQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val selectChatModelQuery = JBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val applyCodeQuery = AsyncJBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val applyCodeBetaQuery = AsyncJBCefJSQuery.create(jbCefBrowser as JBCefBrowserBase)

    private val chatMemory: ChatMemory = MessageWindowChatMemory.withMaxMessages(10)
    private var chatModel: StreamingChatLanguageModel? = null
    private var chatJob: Job? = null

    private val curSelectedFiles = mutableListOf<File>()

    private var applyCodeModel: ChatLanguageModel? = null

    init {
        initChatModel()
        initApplyCodeModel()

        initChatQuery()
        initCancelChatQuery()
        initTabCommandQuery()
        initClearCommandQuery()
        initApplyCodeQuery()
        initApplyCodeBetaQuery()
        initSelectChatModelQuery()

        val htmlContent = this::class.java.getResource("/chat-window/chat.html")?.readText() ?: ""

        // 添加加载完成的监听器
        jbCefBrowser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                // 页面加载完成，注入 CSS 和 JS
                browser?.let {
                    injectCSS(browser)
                    injectJS(browser)
                    initQueryBind(browser)
                }
            }
        }, jbCefBrowser.cefBrowser)

        jbCefBrowser.loadHTML(htmlContent)

        // debug
//        val devToolsBrowser = JBCefBrowser.createBuilder()
//            .setCefBrowser(devTools)
//            .setClient(jbCefBrowser.jbCefClient)
//            .build()

//        devToolsBrowser.openDevtools()

        add(jbCefBrowser.component, BorderLayout.CENTER)
    }

    private fun initChatModel() {
        // 初始化langchain4j AIService
        AIServiceConfigProvider.chatAIServiceConfig?.let { chatConfig->
            chatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(chatConfig.url)
                .apiKey(chatConfig.apiKey)
                .modelName(chatConfig.model)
                .temperature(0.0)
                .maxTokens(chatConfig.maxTokens ?: 4096)
                .build()
        } ?: {
            NotificationUtil.error("请配置正确的chat服务！")
            throw IllegalStateException("请配置正确的chat服务！")
        }
    }

    private fun initApplyCodeModel() {
        AIServiceConfigProvider.applyCodeAIServiceConfig?.let { applyCodeConfig ->
            applyCodeModel = OpenAiChatModel.builder()
                .baseUrl(applyCodeConfig.url)
                .apiKey(applyCodeConfig.apiKey)
                .modelName(applyCodeConfig.model)
                .temperature(0.0)
                .maxTokens(applyCodeConfig.maxTokens ?: 4096)
                .responseFormat("json_object")
                .build()
        }
    }

    private fun initChatQuery() {
        chatQuery.addHandler { message ->
            // 根据用户输入获取AI回答
//            curHttpCall = AIServiceDelegate.chat(
//                chatHistory.appendUserMessage(message, curSelectedFile),
//                callback = { content ->
//                    jbCefBrowser.cefBrowser.executeJavaScript(
//                        """
//                            streamResponse(`${content}`);
//                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
//                    )
//                },
//                finishCallback = {
//                    curSelectedFile.clear()
//                    jbCefBrowser.cefBrowser.executeJavaScript(
//                        """
//                            handleAfterChat();
//                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
//                    )
//                }
//            )
            val sb = StringBuilder()
            chatMemory.add(UserMessage(objectMapper.writeValueAsString(UserMessageWithFiles(message, curSelectedFiles))))
            chatJob = globalIOScope.launch {
                chatModel?.let { model ->
                    try {
                        model.chatFlow {
                            messages(chatMemory.messages())
                        }.collect { reply ->
                            when(reply) {
                                is StreamingChatLanguageModelReply.PartialResponse -> {
                                    sb.append(reply.partialResponse)
                                    jbCefBrowser.cefBrowser.executeJavaScript(
                                        """
                                            streamResponse(`${Markdown.parseMarkdown(sb.toString())}`);
                                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                                    )
                                }
                                is StreamingChatLanguageModelReply.CompleteResponse -> {
                                    jbCefBrowser.cefBrowser.executeJavaScript(
                                        """
                                            streamResponse(`${Markdown.parseMarkdown(sb.toString())}`);
                                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                                    )
                                    jbCefBrowser.cefBrowser.executeJavaScript(
                                        """
                                            handleAfterChat();
                                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                                    )
                                }
                                is StreamingChatLanguageModelReply.Error -> {
                                    jbCefBrowser.cefBrowser.executeJavaScript(
                                        """
                                            handleAfterChat();
                                        """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                                    )
                                }
                            }
                        }
                    } finally {
                        chatMemory.add(AiMessage(sb.toString()))
                    }
                }
            }
            null
        }
    }

    private fun initCancelChatQuery() {
        cancelChatQuery.addHandler { _ ->
//            curHttpCall?.cancel()
            chatJob?.cancel()
            jbCefBrowser.cefBrowser.executeJavaScript(
                """
                    showStopResponseButton = false;
                    document.getElementById('stop-button-container').style.display = 'none';
                    chatInput.style.display = 'block';
                    adjustTextareaHeight(userInput, true); // 发送消息后将用户输入框调整为默认高度
                """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
            )
            null
        }
    }

    private fun initTabCommandQuery() {
        tabCommandQuery.addHandler { _ ->
            invokeLater {
                val currentFileInfo = FileUtil.getCurrentFileInfo(project)
                currentFileInfo?.let {
                    curSelectedFiles.add(File(currentFileInfo.fileName, currentFileInfo.fileContent))
                    if (currentFileInfo.icon == null) {
                        jbCefBrowser.cefBrowser.executeJavaScript(
                            """
                                addContextWidget("", "${currentFileInfo.fileName}", `${currentFileInfo.fileContent.replace("`","\\`").replace("$","\\$")}`, false);
                            """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                        )
                    } else {
                        jbCefBrowser.cefBrowser.executeJavaScript(
                            """
                                addContextWidget("data:image/png;base64,${currentFileInfo.icon}", "${currentFileInfo.fileName}", `${currentFileInfo.fileContent.replace("`","\\`").replace("$","\\$")}`);
                            """.trimIndent(), jbCefBrowser.cefBrowser.url, 0
                        )
                    }
                }
            }
            null
        }
    }

    private fun initClearCommandQuery() {
        clearCommandQuery.addHandler { _ ->
//            chatHistory.clear()
            chatMemory.clear()
            curSelectedFiles.clear()
            null
        }
    }

    // todo 等用 react + typescript重写 chat页面后再改造
    private fun initSelectChatModelQuery() {
        selectChatModelQuery.addHandler { model->
//            AIServiceDelegate.setChatAIService(model)
            null
        }
    }

    private fun initApplyCodeQuery() {
        applyCodeQuery.addHandler { param ->
            FileUtil.getCurrentFile(project)?.let { file ->
//                curApplyCodeCall =
//                    AI.applyCode(project, file, param.request) {
//                        param.callback.success("awesome!")
//                    }
                globalIOScope.launch {
                    try {
                        val startTime = System.currentTimeMillis()
                        applyCodeModel?.chat(UserMessage(CodeApplyTool.build(file.readText(), param.request)))?.let { response ->
                            val costTime = System.currentTimeMillis() - startTime
                            DiffUtil.shouDiff(project, file, objectMapper.readValue(response.aiMessage().text(), AIServiceJsonResponse::class.java).r)
                            invokeLater {
                                NotificationUtil.info("apply code to ${file.name} takes $costTime ms")
                            }
                        }
                    } finally {
                        param.callback.success( "awesome")
                    }
                }
            }
            null
        }
    }

    // ApplyCodeBeta 准备放弃
    private fun initApplyCodeBetaQuery() {
        applyCodeBetaQuery.addHandler { param ->
            FileUtil.getCurrentFile(project)?.let { file ->
//                curApplyCodeCall =
//                    AI.applyCodeByFileContentRebuild(project, file, param.request) {
//                        param.callback.success("awesome!")
//                    }
                param.callback.success("emmm")
            }
            null
        }
    }

    fun injectCSS(browser: CefBrowser) {
        val chatCssCode = this::class.java.getResource("/chat-window/css/chat.css")?.readText() ?: ""
        val highlightCssCode = this::class.java.getResource("/chat-window/css/default.min.css")?.readText() ?: ""
        val injectCSS = (""
                + "var style = document.createElement('style');"
                + "style.type = 'text/css';"
                + "style.appendChild(document.createTextNode(`" + chatCssCode + "`));" // 使用模板字符串
                + "style.appendChild(document.createTextNode(`" + highlightCssCode + "`));" // 使用模板字符串
                + "document.head.appendChild(style);")
        browser.executeJavaScript(injectCSS, browser.url, 0)
    }

    fun injectJS(browser: CefBrowser) {
        val sb = StringBuilder()
        sb.append(this::class.java.getResource("/chat-window/js/highlight.min.js")?.readText() ?: "")
        sb.append(this::class.java.getResource("/chat-window/js/utils.js")?.readText() ?: "")
        sb.append(this::class.java.getResource("/chat-window/js/main.js")?.readText() ?: "")
        sb.append(this::class.java.getResource("/chat-window/js/command.js")?.readText() ?: "")
        sb.append("""
            // 初始化
            userInput.value = userInput.value.trim();
            adjustTextareaHeight(userInput, true);
        """.trimIndent())
//        val jsCode = this::class.java.getResource("/chat-window/js/highlight.min.js")?.readText() ?: ""
        browser.executeJavaScript(sb.toString(), browser.url, 0)
    }

    fun initQueryBind(browser: CefBrowser) {
        val jsCode = """
            function doChat(message) {
                ${chatQuery.inject("message")}
            }
            function doStopChat() {
                ${cancelChatQuery.inject("")}
            }
            function handleTabCommand() {
                ${tabCommandQuery.inject("")}
            }
            function handleClearCommand() {
                ${clearCommandQuery.inject("")}
            }
            function doApplyCode(code, applyButton, buttonSpecifiedClass) {
                ${applyCodeQuery.inject("code","""
                    function(response) {
                        afterApply(applyButton, buttonSpecifiedClass);
                    }
                """.trimIndent(), """
                    function(error_code, error_message) {}
                """.trimIndent())}
            }
            function doApplyCodeBeta(code, applyBetaButton, buttonSpecifiedClass) {
                ${applyCodeBetaQuery.inject("code","""
                    function(response) {
                        afterApply(applyBetaButton, buttonSpecifiedClass);
                    }
                """.trimIndent(), """
                    function(error_code, error_message) {}
                """.trimIndent())}
            }
            function handleModelChange(model) {
                ${selectChatModelQuery.inject("model")}
            }
        """.trimIndent()
        browser.executeJavaScript(jsCode, browser.url, 0)
    }
}