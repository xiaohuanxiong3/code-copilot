package com.github.xiaohuanxiong3.codecopilot.completion.context.java

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

/**
 * @Author Handsome Young
 * @Date 2025/2/24 22:47
 */
object JavaImportContext {

    fun attach(psiFile: PsiFile, fileIndex: ProjectFileIndex, map: MutableMap<String, String>) {
        (psiFile as PsiJavaFile).let {
            it.importList?.importStatements?.forEach { importStatement ->
                importStatement.importReference?.resolve()?.let { resolvedElement ->
                    resolvedElement.text?.let {
                        if (fileIndex.isInSource(resolvedElement.containingFile.virtualFile)) {
                            map[importStatement.importReference!!.qualifiedName] = resolvedElement.text
                        }
                    }
                }
            }
        }
    }

}