package com.github.xiaohuanxiong3.codecopilot.completion.context.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile

/**
 * @Author Handsome Young
 * @Date 2025/5/23 18:05
 */
object JavaFileSimplifier {

    fun simplify(psiFile: PsiJavaFile): String {
        val sb = StringBuilder()
        // 处理 package 语句
        psiFile.packageStatement?.let { packageStatement ->
            sb.append(packageStatement.text + "\n")
        }
        // 处理 import 语句
        psiFile.importList?.allImportStatements?.forEach { importStatementBase ->
            sb.append(importStatementBase.text + "\n")
        }
        // 处理 classes
        psiFile.classes.forEach { psiClass ->
            // 如果 psiClass 是接口、枚举、记录（Record）、注解，不做处理
            if (psiClass.isInterface || psiClass.isEnum || psiClass.isRecord || psiClass.isAnnotationType) {
                sb.append(psiClass.text)
            } else {
                buildClassContent(sb, psiClass)
            }
        }
        return sb.toString()
    }

    private fun buildClassContent(sb: StringBuilder, psiClass: PsiClass) {
        buildClassContent(sb, psiClass, 4)
    }

    private fun buildClassContent(sb: StringBuilder, psiClass: PsiClass, indentLength: Int) {
        psiClass.annotations.forEach { psiAnnotation ->
            sb.append(psiAnnotation.qualifiedName + "\n")
        }
        val classModifiersText = psiClass.modifierList?.text?:""
        sb.append(" ".repeat(indentLength - 4) + "$classModifiersText class ${psiClass.name}${if (psiClass.extendsList == null) "" else " ${psiClass.extendsList!!.text}"}${if (psiClass.implementsList == null) "" else " ${psiClass.implementsList!!.text}"} {\n")
        psiClass.fields.forEach { field ->
            sb.append(" ".repeat(indentLength) + field.text + "\n")
        }
        psiClass.methods.forEach { method ->
            val methodModifiers = method.modifierList.text
            val returnType = method.returnType
            val methodName = method.name
            val parameterList = method.parameterList
            val paramsBuilder = StringBuilder()
            parameterList.parameters.forEach { parameter ->
                val parameterName = parameter.name
                val parameterType = parameter.type.canonicalText
                if (paramsBuilder.isNotEmpty()) {
                    paramsBuilder.append(", ")
                }
                paramsBuilder.append("$parameterType $parameterName")
            }
            val throwList = method.throwsList.text
            if (method.isConstructor) {
                sb.append(" ".repeat(indentLength) + "${if (methodModifiers.isEmpty()) "" else "$methodModifiers "}${methodName}(${paramsBuilder})${throwList.ifEmpty { "" }};\n")
            } else {
                sb.append(" ".repeat(indentLength) + "${if (methodModifiers.isEmpty()) "" else "$methodModifiers "}${returnType?.canonicalText?:"void"} ${methodName}(${paramsBuilder})${throwList.ifEmpty { "" }};\n")
            }
        }
        psiClass.innerClasses.forEach { innerClass->
            buildClassContent(sb, innerClass, indentLength + 4)
        }
        sb.append(" ".repeat(indentLength - 4) + "}\n")
    }

}