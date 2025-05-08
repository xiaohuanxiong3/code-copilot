package com.github.xiaohuanxiong3.codecopilot.support.langchain4j.httpclient

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.xiaohuanxiong3.codecopilot.util.objectMapper
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.jdk.JdkHttpClient
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser

/**
 * @Author Handsome Young
 * @Date 2025/5/7 19:29
 */
class DeepSeekInlineCompletionHttpClient(builder: JdkHttpClientBuilder) : JdkHttpClient(builder) {

    override fun execute(request: HttpRequest?): SuccessfulHttpResponse? {
        return request?.let {
            val finalRequest = modifyRequest(request)
            super.execute(finalRequest)
        }
    }

    override fun execute(request: HttpRequest?, parser: ServerSentEventParser?, listener: ServerSentEventListener?) {
        request?.let {
            val finalRequest = modifyRequest(request)
            super.execute(finalRequest, parser, listener)
        }
    }

    private fun modifyRequest(request: HttpRequest): HttpRequest {
        val requestBody = request.body()
        val jsonNode = objectMapper.readTree(requestBody)
        val messagesNode = jsonNode.get("messages")
        if (!messagesNode.isEmpty) {
            val lastMessageNode = messagesNode.get(messagesNode.size() - 1)
            if (lastMessageNode.isObject) {
                (lastMessageNode as ObjectNode).put("prefix", true)
            }
        }
        val finalRequestBode = objectMapper.writeValueAsString(jsonNode)
        return HttpRequest.builder()
            .method(request.method())
            .url(request.url())
            .headers(request.headers())
            .body(finalRequestBode)
            .build()
    }

}