package com.github.xiaohuanxiong3.codecopilot.completion.context

import com.github.xiaohuanxiong3.codecopilot.completion.context.java.JavaPackageContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.kotlin.KotlinPackageContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.python.PythonPackageContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiFile
import com.jetbrains.python.extensions.getQName

/**
 * @Author Handsome Young
 * @Date 2024/11/2 22:48
 */
object PackageContext {

    // 返回特定文件所在包下的所有文件的全名和内容
    fun attach(psiFile: PsiFile, map: MutableMap<String, String>){
        val directory = psiFile.containingDirectory
        try {
            directory.files.filter{ it != psiFile }.forEach { file ->
                when (file.language.id) {
                    "JAVA" -> {
                        JavaPackageContext.attach(file, map)
                    }
                    "kotlin" -> {
                        KotlinPackageContext.attach(file, map)
                    }
                    "Python" -> {
                        PythonPackageContext.attach(file, map)
                    }
                    else -> {
                        map[file.getQName().toString()] = file.text
                    }
                }
            }
        } catch (e : Exception) {
            thisLogger().info("PackageContext.attach error: ${e.message}")
        }

    }

}