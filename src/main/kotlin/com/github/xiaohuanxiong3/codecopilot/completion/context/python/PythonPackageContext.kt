package com.github.xiaohuanxiong3.codecopilot.completion.context.python

import com.intellij.psi.PsiFile
import com.jetbrains.python.extensions.getQName
import com.jetbrains.python.psi.PyFile

/**
 * @Author Handsome Young
 * @Date 2025/2/25 00:28
 */
object PythonPackageContext {

    fun attach(psiFile: PsiFile, map: MutableMap<String, String>) {
        (psiFile as PyFile).let {
            map[it.getQName().toString()] = it.text
        }
    }

}