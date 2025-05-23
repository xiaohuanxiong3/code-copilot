package com.github.xiaohuanxiong3.codecopilot.completion.context.java

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

/**
 * @Author Handsome Young
 * @Date 2025/2/25 00:22
 */
object JavaPackageContext {

    fun attach(psiFile: PsiFile, map: MutableMap<String, String>) {
        (psiFile as PsiJavaFile).let {
//            it.classes.forEach { clazz ->
//                clazz.qualifiedName?.let { qualifiedName ->
//                    map[qualifiedName] = clazz.text
//                }
//            }
            map[it.name] = JavaFileSimplifier.simplify(it)
        }
    }

}