package com.github.xiaohuanxiong3.codecopilot

import com.github.xiaohuanxiong3.codecopilot.util.moshi
import com.squareup.moshi.JsonAdapter

/**
 * @Author Handsome Young
 * @Date 2025/4/29 23:12
 */
object AIServiceConfigProvider {

    data class AIServiceConfig(
        val provider: String,
        val model: String,
        val name: String,
        val url: String,
        val apiKey: String,
        val maxTokens: Int?
    )

    data class AIServiceConfigs(
        val chatModels: List<AIServiceConfig>,
        val autoCompleteModels: List<AIServiceConfig>,
        val inlineEditModels: List<AIServiceConfig>,
        val applyCodeModels: List<AIServiceConfig>,
    )

    val aiServiceConfigsAdapter: JsonAdapter<AIServiceConfigs> = moshi.adapter(AIServiceConfigs::class.java)

    var aiServiceConfigs: AIServiceConfigs? = null

    var chatAIServiceConfig: AIServiceConfig? = null

    var completionAIServiceConfig: AIServiceConfig? = null

    var inlineEditAIServiceConfig: AIServiceConfig? = null

    var applyCodeAIServiceConfig: AIServiceConfig? = null

    init {
        val configText = this::class.java.getResource("/config.json")?.readText()
        aiServiceConfigs = configText?.let { aiServiceConfigsAdapter.fromJson(it) }?.apply {
            chatAIServiceConfig = chatModels.firstOrNull()
            completionAIServiceConfig = autoCompleteModels.firstOrNull()
            inlineEditAIServiceConfig = inlineEditModels.firstOrNull()
            applyCodeAIServiceConfig = applyCodeModels.firstOrNull()
        }
    }



}