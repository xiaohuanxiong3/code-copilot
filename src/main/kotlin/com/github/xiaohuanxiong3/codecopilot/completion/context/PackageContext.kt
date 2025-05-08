package com.github.xiaohuanxiong3.codecopilot.completion.context

import com.github.xiaohuanxiong3.codecopilot.completion.context.java.JavaPackageContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.kotlin.KotlinPackageContext
import com.github.xiaohuanxiong3.codecopilot.completion.context.python.PythonPackageContext
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
//                JavaLanguage.INSTANCE -> {
//                    (file as PsiJavaFile).let {
//                        it.classes.forEach { clazz ->
//                            clazz.qualifiedName?.let { qualifiedName ->
//                                map[qualifiedName] = clazz.text
//                            }
//                        }
//                    }
//                }
//                KotlinLanguage.INSTANCE -> {
//                    (file as KtFile).getFileOrScriptDeclarations().forEach { declaration ->
//                        declaration.kotlinFqName?.let {
//                            map[it.toString()] = declaration.text
//                        }
//                    }
//                }
//                else -> {
//                    map[file.getFqNameByDirectory().toString()] = file.text
//                }
            }
        }
    }

}