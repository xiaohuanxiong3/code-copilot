package com.github.xiaohuanxiong3.codecopilot.completion.validation

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


/**
 * 通过 JavaCodeFragmentFactory 等进行语义分析 ---- 没有统一的接口定义，感觉难度较大
 * 通过 PsiFile 进行分析 ---- 不同语言的Psi接口定义不同，需要分别处理，且处理逻辑较为复杂
 * @Author Handsome Young
 * @Date 2024/12/9 19:59
 */
object SemanticAnalyzer {

    fun semanticallyCorrect(psiFile: PsiFile) : Boolean {
        var correct = true
        when(psiFile.fileType.name) {
             "Java" -> {
                runReadAction {
//                    PsiTreeUtil.findChildrenOfType(psiFile, PsiReferenceExpression::class.java).forEach { referenceExpression ->
//                        if (referenceExpression.resolve() == null) {
//                            correct = false
//                            return@runReadAction
//                        }
//                    }
//                    PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java).forEach { methodCallExpression ->
//                        if (methodCallExpression.resolveMethod() == null) {
//                            correct = false
//                            return@runReadAction
//                        }
//                    }
                }
            }
            "Kotlin" -> {

            }
        }
        return correct
    }

    fun checkSyntaxErrors(project: Project, psiFile: PsiFile): Boolean {

        // 使用 DaemonCodeAnalyzer 进行语法分析
        DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, true)

        // 获取错误高亮
        val highlights: MutableList<HighlightInfo> = DaemonCodeAnalyzerImpl.getHighlights(
            psiFile.viewProvider.document,
            HighlightSeverity.ERROR,
            project
        )

        // 如果存在错误高亮，说明有语法错误
        return highlights.size > 0
    }

    fun simpleAnalyze(project: Project, psiFile: PsiFile): Boolean {
//        HighlightingSettingsPerFile.getInstance(project).setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.ESSENTIAL)

        val daemonCodeAnalyzer = DaemonCodeAnalyzerImpl(project)
        val highlights = runReadAction {
            daemonCodeAnalyzer.restart(psiFile)
            daemonCodeAnalyzer.waitForTermination()
//            daemonCodeAnalyzer.getFileLevelHighlights(project, psiFile)
            DaemonCodeAnalyzerImpl.getHighlights(psiFile.viewProvider.document, HighlightSeverity.ERROR, project)
        }
        return !highlights.any { it.severity == HighlightSeverity.ERROR }
    }
}