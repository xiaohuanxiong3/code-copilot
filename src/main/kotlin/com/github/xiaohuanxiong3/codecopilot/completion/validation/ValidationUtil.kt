package com.github.xiaohuanxiong3.codecopilot.completion.validation

import com.github.xiaohuanxiong3.codecopilot.completion.InlineCompletionContext
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile


/**
 * @Author Handsome Young
 * @Date 2024/11/26 20:33
 */
object ValidationUtil {

    fun validate(project: Project, requestContext: InlineCompletionContext.RequestContext, completionContent: String) : Boolean {
        val prefixContinuation : Boolean = requestContext.offset == requestContext.lineEnd && !requestContext.linePrefix.isNullOrBlank()
        val newCode = if (prefixContinuation) {
            requestContext.prefix + requestContext.linePrefix + completionContent + requestContext.suffix
        } else {
            requestContext.prefix + completionContent + requestContext.suffix
        }
        val valid = runReadAction {
            !PsiTreeUtil.hasErrorElements(PsiFileFactory.getInstance(project).createFileFromText("temp.${requestContext.editor.virtualFile.name.split(".")[1]}", requestContext.editor.virtualFile.fileType, newCode))
        }
        return valid
    }

    fun validateSuccessThen(project: Project, requestContext: InlineCompletionContext.RequestContext, completionContent: String, callback: () -> Unit) {
        val newCode = insertCompletionContent(requestContext, completionContent)
//        val tempDocument = DocumentImpl(newCode)
        val psiFile = runReadAction {
            PsiFileFactory.getInstance(project).createFileFromText("temp.${requestContext.editor.virtualFile.name.split(".")[1]}", requestContext.editor.virtualFile.fileType, newCode)
        }
//        if (!runReadAction { PsiTreeUtil.hasErrorElements(psiFile) } && SemanticAnalyzer.semanticallyCorrect(psiFile)) {
//            callback()
//        }
        if (!runReadAction { PsiTreeUtil.hasErrorElements(psiFile) } && SemanticAnalyzer.semanticallyCorrect(psiFile)) {
            callback()
        }
//        HighlightInfoProcessor.getEmpty()
//        val highlightInfos = DaemonCodeAnalyzerImpl(project).runMainPasses(
//            psiFile,
//            psiFile.fileDocument,
//            ProgressManager.getInstance().progressIndicator
//        ).filter { it.type == HighlightInfoType.ERROR }
//        return hasErrorHighlightInfo(project, psiFile, tempDocument)
//        return highlightInfos.isEmpty()
    }

    fun validateTest(project: Project, requestContext: InlineCompletionContext.RequestContext, completionContent: String, callback: () -> Unit) {
//        runReadAction {
//            PsiDocumentManager.getInstance(project).getPsiFile(requestContext.editor.document)
//        }?.let { psiFile ->
//            val valid = SemanticAnalyzer.simpleAnalyze(project, psiFile)
//            if (valid)
//                callback()
//        }
//        runReadAction {
//            PsiDocumentManager.getInstance(project).getPsiFile(requestContext.editor.document)
//        }?.let { psiFile ->
        val virtualFile = requestContext.editor.virtualFile
        val tempContent = insertCompletionContent(requestContext, completionContent)

        val tempVirtualFile = createModifiedVirtualFile(virtualFile, tempContent).apply {
            this.charset = virtualFile.charset
        }
        runReadAction {
            PsiFileFactory.getInstance(project).createFileFromText("temp.${requestContext.editor.virtualFile.name.split(".")[1]}", requestContext.editor.virtualFile.fileType, tempContent)
        }?.let { tempPsiFile ->
            invokeLater {
                val valid = SemanticAnalyzer.simpleAnalyze(project, tempPsiFile)
                if (valid)
                    callback()
            }
        }
    }

    private fun insertCompletionContent(requestContext: InlineCompletionContext.RequestContext, completionContent: String) : String {
        val prefixContinuation : Boolean = requestContext.linePrefix != null
        return if (prefixContinuation) {
            requestContext.prefix + requestContext.linePrefix + completionContent + requestContext.suffix
        } else {
            requestContext.prefix + completionContent + requestContext.suffix
        }
    }

    private fun createModifiedVirtualFile(originalFile: VirtualFile, modifiedContent: String): VirtualFile {
        return LightVirtualFile(
            originalFile.name,
            FileTypeRegistry.getInstance().getFileTypeByFileName(originalFile.name),
            modifiedContent)
    }

    private fun hasErrorHighlightInfo(project: Project, psiFile: PsiFile, document: Document) : Boolean{
//        val result: MutableList<HighlightInfo?> = mutableListOf()
//        val virtualFile: VirtualFile = psiFile.virtualFile
//        if (!virtualFile.fileType.isBinary) {
//            DumbService.getInstance(project).runReadActionInSmartMode {
//                val mainPasses = TextEditorHighlightingPassRegistrarEx.getInstanceEx(project).instantiateMainPasses(psiFile, document, HighlightInfoProcessor.getEmpty())
//
//                mainPasses.forEach { pass ->
//                    pass.doCollectInformation(progress)
//                    pass.infos.find {
//                        it.type == HighlightInfoType.ERROR
//                    }
//                }
//
//            }
//        }
//        val localInspectionsPass = LocalInspectionsPassFactory().createMainHighlightingPass(
//            psiFile,
//            document,
//            HighlightInfoProcessor.getEmpty()
//        )
//        localInspectionsPass?.doCollectInformation(progress)
//        val highlights = runReadAction {
//            DaemonCodeAnalyzerImpl.getHighlights(psiFile.fileDocument, HighlightSeverity.ERROR, project)
//        }
//        return highlights.any { it.type == HighlightInfoType.ERROR } ?: false
        return false
    }

}