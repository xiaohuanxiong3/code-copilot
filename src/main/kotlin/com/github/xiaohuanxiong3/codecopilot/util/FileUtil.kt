package com.github.xiaohuanxiong3.codecopilot.util

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon


/**
 * @Author Handsome Young
 * @Date 2024/10/7 14:34
 */
object FileUtil {

    data class FileInfo(val icon: String?, val fileName: String, val fileContent: String)

    fun getCurrentFile(project: Project): VirtualFile? {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val selectedFiles = fileEditorManager.selectedFiles
        return if (selectedFiles.isNotEmpty()) selectedFiles[0] else null
    }

    fun getCurrentFileInfo(project: Project) : FileInfo? {
        getCurrentFile(project)?.let { file ->
            val icon = imageToBase64(iconToBufferedImage(IconUtil.getIcon(file,0 ,project)))
            val fileName = file.name
            val fileContent = FileDocumentManager.getInstance().getDocument(file)?.text
            return fileContent?.let { FileInfo(icon, fileName, it) }
        }
        return null
    }

    // 将 Icon 转换为 BufferedImage
    private fun iconToBufferedImage(icon: Icon): BufferedImage {
        if (icon is ImageIcon) {
            // 如果是 ImageIcon，可以直接获取 Image
            val imageIcon = icon
            val bufferedImage = BufferedImage(imageIcon.iconWidth, imageIcon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g: Graphics = bufferedImage.createGraphics()
            g.drawImage(imageIcon.image, 0, 0, null)
            g.dispose()
            return bufferedImage
        } else {
            // 对于非 ImageIcon 的 Icon，需要手动绘制
//            val bufferedImage = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val bufferedImage = ImageUtil.createImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            val g: Graphics2D = bufferedImage.createGraphics()
            // 开启抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            icon.paintIcon(null, g, 0, 0)
            g.dispose()
            return bufferedImage
        }
    }

    // 将 BufferedImage 转换为 Base64 字符串
    private fun imageToBase64(image: BufferedImage?): String? {
        try {
            val baos: ByteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(image, "png", baos) // 这里可以选择 jpg 或 png
            val bytes: ByteArray = baos.toByteArray()
            return Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}