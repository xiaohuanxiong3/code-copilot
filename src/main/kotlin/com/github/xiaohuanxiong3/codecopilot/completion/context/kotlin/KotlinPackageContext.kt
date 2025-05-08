package com.github.xiaohuanxiong3.codecopilot.completion.context.kotlin

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getFileOrScriptDeclarations

/**
 * @Author Handsome Young
 * @Date 2025/2/25 00:30
 */
object KotlinPackageContext {

    fun attach(psiFile: PsiFile, map: MutableMap<String, String>) {
        (psiFile as KtFile).getFileOrScriptDeclarations().forEach { declaration ->
            declaration.kotlinFqName?.let {
                map[it.toString()] = declaration.text
            }
        }
    }

}