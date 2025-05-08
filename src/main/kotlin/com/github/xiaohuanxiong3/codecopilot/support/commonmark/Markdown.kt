package com.github.xiaohuanxiong3.codecopilot.support.commonmark

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * @Author Handsome Young
 * @Date 2024/10/7 00:27
 */
object Markdown {

    private val parser: Parser = Parser.builder().build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .nodeRendererFactory { context ->
            EscapeCodeNodeRenderer(context)
        }
        .build()

    fun parseMarkdown(content : String) : String {
        return renderer.render(parser.parse(content))
    }
}