package com.github.xiaohuanxiong3.codecopilot.support.langchain4j.httpclient

import dev.langchain4j.http.client.jdk.JdkHttpClient
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder

/**
 * @Author Handsome Young
 * @Date 2025/5/7 19:22
 */
class DeepSeekInlineCompletionHttpClientBuilder : JdkHttpClientBuilder() {

    override fun build(): JdkHttpClient? {
        return DeepSeekInlineCompletionHttpClient(this)
    }

}