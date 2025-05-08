package com.github.xiaohuanxiong3.codecopilot.completion.context.kotlin

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.caches.KotlinShortNamesCache
import org.jetbrains.kotlin.psi.KtFile

/**
 * @Author Handsome Young
 * @Date 2025/2/24 22:52
 */
object KotlinImportContext {

    fun attach(psiFile: PsiFile, fileIndex: ProjectFileIndex, map: MutableMap<String, String>) {
        val kotlinShortNamesCache = KotlinShortNamesCache(psiFile.project)
        (psiFile as KtFile).let {
            it.importList?.imports?.forEach{ import ->
                import.importedReference?.let { importedReference ->
                    if(importedReference.isValid) {
                        kotlinShortNamesCache.getClassesByName(importedReference.text.substringAfterLast("."), GlobalSearchScope.allScope(psiFile.project))?.forEach { psiClass ->
                            if (fileIndex.isInSource(psiClass.containingFile.virtualFile)) {
                                map[psiClass.kotlinFqName.toString()] = psiClass.text
                            }
                        }
                    }
                }
            }
        }
    }

}