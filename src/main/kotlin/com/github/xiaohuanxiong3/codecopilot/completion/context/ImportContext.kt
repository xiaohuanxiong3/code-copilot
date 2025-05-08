package com.github.xiaohuanxiong3.codecopilot.completion.context

import com.github.xiaohuanxiong3.codecopilot.completion.context.java.JavaImportContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.kotlin.KotlinImportContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.python.PythonImportContext
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile

/**
 * @Author Handsome Young
 * @Date 2024/11/4 21:41
 */
object ImportContext {

    fun attach(psiFile: PsiFile, map: MutableMap<String, String>) {
        val fileIndex = ProjectRootManager.getInstance(psiFile.project).fileIndex
        when(psiFile.language.id) {
            "JAVA" -> {
                JavaImportContext.attach(psiFile, fileIndex, map)
            }
            "kotlin" -> {
                KotlinImportContext.attach(psiFile, fileIndex, map)
            }
            "Python" -> {
                PythonImportContext.attach(psiFile, fileIndex, map)
            }
            else -> {
                // do nothing
            }
        }
    }

}