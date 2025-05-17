package com.github.xiaohuanxiong3.codecopilot.completion.context.python

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyTargetExpression

/**
 * @Author Handsome Young
 * @Date 2025/2/24 23:00
 */
object PythonImportContext {

    fun attach(psiFile: PsiFile, fileIndex: ProjectFileIndex, map: MutableMap<String, String>) {
        (psiFile as PyFile).let {
            it.importBlock.forEach { importStatement ->
                importStatement.importElements.forEach { importElement ->
                    importElement.multiResolve().forEach { resolveResult ->
                        resolveResult.element?.let { element ->
                            if (element.text.isNotEmpty() && !fileIndex.isInLibraryClasses(element.containingFile.virtualFile) && !fileIndex.isInLibrarySource(element.containingFile.virtualFile)) {
                                when(element) {
                                    is PyTargetExpression -> {
                                        element.findAssignedValue()?.let { assignedValue ->
                                            map[element.qualifiedName.toString()] = assignedValue.text
                                        }
                                    }
                                    else -> {
                                        map[element.toString()] = element.text
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}