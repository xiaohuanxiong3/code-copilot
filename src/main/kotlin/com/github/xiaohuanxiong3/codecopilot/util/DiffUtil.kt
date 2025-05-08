package com.github.xiaohuanxiong3.codecopilot.util

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile


/**
 * @Author Handsome Young
 * @Date 2024/10/13 19:23
 */
object DiffUtil {

    private val diffContentFactory : DiffContentFactory by lazy {
        DiffContentFactory.getInstance()
    }

    fun shouDiff(project: Project, currentFile: VirtualFile, newFileContent: String) {
        val originalContent = diffContentFactory.create(project, currentFile)
        val newContent = diffContentFactory.create(project, createModifiedVirtualFile(currentFile, newFileContent))
        val diffRequest = SimpleDiffRequest(
            "AI Suggested Changes",
            originalContent,
            newContent,
            "Current File",
            "AI Suggestion"
        )
        invokeLater {
            DiffManager.getInstance().showDiff(project, diffRequest, DiffDialogHints.MODAL)
        }
//        callback()
    }

    private fun createModifiedVirtualFile(originalFile: VirtualFile, modifiedContent: String): VirtualFile {
        return LightVirtualFile(
            originalFile.name,
            FileTypeRegistry.getInstance().getFileTypeByFileName(originalFile.name),
            modifiedContent)
    }
}