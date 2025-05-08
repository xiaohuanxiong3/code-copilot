package com.github.xiaohuanxiong3.codecopilot.toolWindow

import com.github.xiaohuanxiong3.codecopilot.ui.UI
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NullableComponent
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBPanel
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * @Author Handsome Young
 * @Date 2024/9/11 20:33
 */
class AIChatToolWindow(project: Project) : JBPanel<AIChatToolWindow>(MigLayout("fill,insets 0")), NullableComponent {

    // 中间内容页面 + 底部输入页面
    private val contentPane = AIChatPanel(project)

    init {
        paint()
    }

    fun paint() {
        add(contentPane, UI.fill().alignY("center").wrap())
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    companion object {

        private fun titledPanel(title: String, fill: Boolean = false, body: JPanel.() -> Unit): JComponent {
            val innerPanel = JPanel(UI.migLayout(UI.migSize(4)))
            innerPanel.body()
            return JPanel(UI.migLayout()).apply {
                border = IdeBorderFactory.createTitledBorder(title)
                if (fill) {
                    add(innerPanel, UI.fillX())
                } else {
                    add(innerPanel)
                    add(JPanel(), UI.fillX())
                }
            }
        }

        private fun JPanel.addVertically(vararg components: JComponent) {
            layout = UI.migLayoutVertical()
            components.forEach {
                add(it, UI.fillX())
            }
            add(JPanel(), UI.fillY())
        }

    }
}