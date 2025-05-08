package com.github.xiaohuanxiong3.codecopilot.ui

import com.intellij.ui.scale.JBUIScale
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.layout.LayoutUtil
import net.miginfocom.swing.MigLayout

/**
 * 参考 https://github.com/YiiGuxing/TranslationPlugin/blob/master/src/main/kotlin/cn/yiiguxing/plugin/translate/ui/UI.kt
 */
object UI {

    fun migLayout(gapX: String = "0!", gapY: String = "0!", insets: String = "0") =
        MigLayout(LC().fill().gridGap(gapX, gapY).insets(insets))

    fun migLayoutVertical() =
        MigLayout(LC().flowY().fill().gridGap("0!", "0!").insets("0"))

    /**
     * In different IDE versions, the default unit of MigLayout is different.
     * Therefore, it is necessary to use this method to unify the unit as pixels.
     */
    fun migSize(size: Int, scale: Boolean = true): String = "${if (scale) JBUIScale.scale(size) else size}px"

    fun cc() = CC()

    fun spanX(cells: Int = LayoutUtil.INF): CC = CC().spanX(cells)

    fun fill(): CC = CC().grow().push()

    /**
     * growX和pushX都可以用来控制组件和布局中行的增长能力。growX设置组件在其单元格中水平方向上的增长能力，
     * 这意味着如果窗口大小增加，使用growX约束的组件将在其单元格内水平增长以填充额外的空间。
     * 而pushX则使组件所在的行具有增长能力，这意味着如果窗口大小增加，使用pushX约束的行将增长以填充额外的空间。
     * 例如，如果您希望一个文本框在窗口大小增加时水平增长以填充额外的空间，您可以使用growX约束。
     * 而如果您希望整个行都增长以填充额外的空间，您可以使用pushX约束。
     */

    /**
     * 横向填充剩余空间
     */
    fun fillX(): CC = CC().growX().pushX()

    /**
     * 纵向填充剩余空间
     */
    fun fillY(): CC = CC().growY().pushY()

    /**
     * 换行
     */
    fun wrap(): CC = CC().wrap()


}