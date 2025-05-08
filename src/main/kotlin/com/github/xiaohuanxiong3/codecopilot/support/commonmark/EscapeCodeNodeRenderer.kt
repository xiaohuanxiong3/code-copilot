package com.github.xiaohuanxiong3.codecopilot.support.commonmark

import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlNodeRendererContext
import org.commonmark.renderer.html.HtmlWriter


/**
 * 对所有代码中的 $ 进行转义，防止 html被``包裹的字符串将 ${text} 模式的字符串识别为表达式
 * @Author Handsome Young
 * @Date 2024/10/7 00:28
 */
class EscapeCodeNodeRenderer(private val context: HtmlNodeRendererContext) : NodeRenderer {

    private var html: HtmlWriter = context.writer

    override fun getNodeTypes(): MutableSet<Class<out Node>> {
        return mutableSetOf(IndentedCodeBlock::class.java, FencedCodeBlock::class.java, Code::class.java)
    }

    override fun render(node: Node?) {
        if (node is Code) {
            html.tag("code", getAttrs(node, "code"))
            html.text(node.literal.replace("$","\\$").replace("`","\\`"))
            html.tag("/code")
        } else if (node is FencedCodeBlock) {
            val literal: String = node.literal
            val attributes: MutableMap<String, String> = LinkedHashMap()
            val info: String = node.info
            if (node.info.isNotEmpty()) {
                val space = info.indexOf(" ")
                val language = if (space == -1) {
                    info
                } else {
                    info.substring(0, space)
                }
                attributes["class"] = "language-$language"
            }
            html.line()
            html.tag("pre", getAttrs(node, "pre"))
            html.tag("code", getAttrs(node, "code", attributes))
            html.text(literal.replace("$", "\\$").replace("`","\\`"))
            html.tag("/code")
            html.tag("/pre")
            html.line()
        } else if (node is IndentedCodeBlock) {
            val literal: String = node.literal
            html.line()
            html.tag("pre", getAttrs(node, "pre"))
            html.tag("code", getAttrs(node, "code", emptyMap()))
            html.text(literal.replace("$", "\\$").replace("`","\\`"))
            html.tag("/code")
            html.tag("/pre")
            html.line()
        }
    }

    private fun getAttrs(node: Node, tagName: String) : Map<String, String> {
        return getAttrs(node, tagName, emptyMap())
    }

    private fun getAttrs(node: Node, tagName: String, defaultAttributes: Map<String,String>) : Map<String, String> {
        return context.extendAttributes(node, tagName, defaultAttributes)
    }
}