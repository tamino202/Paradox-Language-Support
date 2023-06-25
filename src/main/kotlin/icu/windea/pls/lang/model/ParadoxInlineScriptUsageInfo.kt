package icu.windea.pls.lang.model

import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.script.psi.*

/**
 * 内联脚本调用信息。
 * @property expression 内联脚本的路径表达式。
 * @property elementOffset 对应的[ParadoxScriptExpressionElement]在文件中的起始位置。
 * @property gameType 对应的游戏类型。
 * @property file 对应的文件。使用[QueryExecutor]进行查询时才能获取。
 */
data class ParadoxInlineScriptUsageInfo(
    val expression: String,
    override val elementOffset: Int,
    override val gameType: ParadoxGameType
) : ParadoxExpressionInfo {
    @Volatile override var file: PsiFile? = null
}
