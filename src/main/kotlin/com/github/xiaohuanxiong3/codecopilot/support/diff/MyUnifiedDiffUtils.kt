package com.github.xiaohuanxiong3.codecopilot.support.diff

import com.github.difflib.patch.ChangeDelta
import com.github.difflib.patch.Chunk
import com.github.difflib.patch.Patch
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.DiffLine
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.DiffLineType
import com.github.xiaohuanxiong3.codecopilot.inlineEdit.InlineEditContext
import java.util.regex.Pattern

/**
 * @Author Handsome Young
 * @Date 2025/1/22 18:59
 */
object MyUnifiedDiffUtils {

    @JvmStatic
    private val UNIFIED_DIFF_CHUNK_REGEXP: Pattern = Pattern
        .compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@$")

    fun parseUnifiedDiff(diff: List<String>): Patch<String> {
//        var inPrelude = true
        val rawChunk: MutableList<Array<String>> = java.util.ArrayList()
        val patch = Patch<String>()

        var old_ln = 0
        var new_ln = 0
        var tag: String
        var rest: String
        for (line in diff) {
            // Skip leading lines until after we've seen one starting with '+++'
            // 由于大模型返回的diff patch不稳定，去掉此处处理，改为在 DiffUtil.preprocessDiffContent() 中处理
            val m = UNIFIED_DIFF_CHUNK_REGEXP.matcher(line)
            if (m.find()) {
                // Process the lines in the previous chunk
                processLinesInPrevChunk(rawChunk, patch, old_ln, new_ln)
                // Parse the @@ header
                old_ln = if (m.group(1) == null) 1 else m.group(1).toInt()
                new_ln = if (m.group(3) == null) 1 else m.group(3).toInt()

                if (old_ln == 0) {
                    old_ln = 1
                }
                if (new_ln == 0) {
                    new_ln = 1
                }
            } else {
                if (line.length > 0) {
                    tag = line.substring(0, 1)
                    rest = line.substring(1)
                    if (" " == tag || "+" == tag || "-" == tag) {
                        rawChunk.add(arrayOf(tag, rest))
                    }
                } else {
                    rawChunk.add(arrayOf(" ", ""))
                }
            }
        }

        // Process the lines in the last chunk
        processLinesInPrevChunk(rawChunk, patch, old_ln, new_ln)

        return patch
    }

    private fun processLinesInPrevChunk(
        rawChunk: MutableList<Array<String>>,
        patch: Patch<String>,
        old_ln: Int,
        new_ln: Int
    ) {
        var tag: String
        var rest: String
        if (!rawChunk.isEmpty()) {
            val oldChunkLines: MutableList<String> = ArrayList()
            val newChunkLines: MutableList<String> = ArrayList()

            val removePosition: MutableList<Int> = ArrayList()
            val addPosition: MutableList<Int> = ArrayList()
            var removeNum = 0
            var addNum = 0
            for (raw_line in rawChunk) {
                tag = raw_line[0]
                rest = raw_line[1]
                if (" " == tag || "-" == tag) {
                    removeNum++
                    oldChunkLines.add(rest)
                    if ("-" == tag) {
                        removePosition.add(old_ln - 1 + removeNum)
                    }
                }
                if (" " == tag || "+" == tag) {
                    addNum++
                    newChunkLines.add(rest)
                    if ("+" == tag) {
                        addPosition.add(new_ln - 1 + addNum)
                    }
                }
            }
            patch.addDelta(
                ChangeDelta(
                    Chunk(
                        old_ln - 1, oldChunkLines, removePosition
                    ), Chunk(
                        new_ln - 1, newChunkLines, addPosition
                    )
                )
            )
            rawChunk.clear()
        }
    }

    data class ChunkInfo(
        val startLine: Int,
        var chunk: MutableList<Array<String>>
    )

    fun handleUnifiedDiff(inlineEditContext: InlineEditContext, diffContent: String) {
        val diff = preprocessDiffContent(diffContent)
        val chunkList: MutableList<ChunkInfo> = mutableListOf()
        var rawChunk: MutableList<Array<String>> = mutableListOf()
        var tag: String
        var rest: String
        for (line in diff) {
            val m = UNIFIED_DIFF_CHUNK_REGEXP.matcher(line)
            if (m.find()) {
                // 处理之前的diff块中的内容
                // 由于大模型返回的diff块原文件和新文件的起始行号通常是不准确的，所以这里不做处理
                if (rawChunk.isNotEmpty()) {
                    val startLine = findStartLineInOriginalText(inlineEditContext.originalText.lines(), rawChunk)
                    chunkList.add(ChunkInfo(startLine, rawChunk))
                }
                rawChunk = mutableListOf()
            } else {
                if (line.isNotEmpty()) {
                    tag = line.substring(0, 1)
                    rest = line.substring(1)
                    if (" " == tag || "+" == tag || "-" == tag) {
                        rawChunk.add(arrayOf(tag, rest))
                    }
                } else {
                    // todo 感觉这里有问题
                    rawChunk.add(arrayOf(" ", ""))
                }
            }
        }
        // 处理最后一个diff块中的内容
        if (rawChunk.isNotEmpty()) {
            val startLine = findStartLineInOriginalText(inlineEditContext.originalText.lines(), rawChunk)
            chunkList.add(ChunkInfo(startLine, rawChunk))
        }

        // 处理完所有diff块后，根据diff块的内容构造diffLines
        var lineIndex = 0
        var chunkIndex = 0
        val diffLines = mutableListOf<DiffLine>()
        val originalLines = inlineEditContext.originalText.lines()
        while (lineIndex < originalLines.size) {
            if (chunkIndex >= chunkList.size || lineIndex < chunkList[chunkIndex].startLine) {
                diffLines.add(DiffLine(DiffLineType.SAME, originalLines[lineIndex]))
                lineIndex++
            } else {
                chunkList[chunkIndex].chunk.forEachIndexed { index, strings ->
                    when(strings[0]) {
                        " " -> {
                            diffLines.add(DiffLine(DiffLineType.SAME, strings[1]))
                            lineIndex++
                        }
                        "+" -> {
                            diffLines.add(DiffLine(DiffLineType.NEW, strings[1]))
                        }
                        "-" -> {
                            diffLines.add(DiffLine(DiffLineType.OLD, strings[1]))
                            lineIndex++
                        }
                    }
                }
                chunkIndex++
            }
        }
        inlineEditContext.diffLines = diffLines
    }

    private fun preprocessDiffContent(diffContent: String): List<String> {
        val result = mutableListOf<String>()
        diffContent.lines().forEach { line ->
            if (line.startsWith("+++")) {
                result.clear()
            } else if (line.startsWith("@@")) {
                result.add(line.substringBeforeLast("@@") + "@@")
            } else {
                result.add(line)
            }
        }
        return result
    }

    private fun findStartLineInOriginalText(originalLines: List<String>, chunk: MutableList<Array<String>>): Int {
        var chunkLinesOfOriginalText = chunk.filter{ it[0] == " " || it[0] == "-" }.map { it[1] }
        var chunkLinesOfModifiedText = chunk.filter { it[0] == " " || it[0] == "+" }.map { it[1] }
        var startLine = originalLines.windowed(chunkLinesOfOriginalText.size)
            .indexOfFirst { it == chunkLinesOfOriginalText }
        while (startLine == -1) {
            if (chunkLinesOfOriginalText.last() == chunkLinesOfModifiedText.last()) {
                chunkLinesOfModifiedText = chunkLinesOfModifiedText.dropLast(1)
                chunkLinesOfOriginalText = chunkLinesOfOriginalText.dropLast(1)
                chunk.removeLast()
            } else {
                throw IllegalArgumentException("Invalid diff patch!")
            }
            startLine = originalLines.windowed(chunkLinesOfOriginalText.size)
                .indexOfFirst { it == chunkLinesOfOriginalText }
        }
        return startLine
    }

}