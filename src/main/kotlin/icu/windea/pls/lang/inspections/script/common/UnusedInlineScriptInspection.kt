package icu.windea.pls.lang.inspections.script.common

import com.intellij.codeInspection.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.lang.quickfix.*
import icu.windea.pls.lang.search.*
import icu.windea.pls.lang.search.selector.*
import icu.windea.pls.lang.util.*

/**
 * 未使用的内联脚本的检查。
 */
class UnusedInlineScriptInspection : LocalInspectionTool() {
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        //still check if inference.inlineScriptConfig is not checked
        //if(!getSettings().inference.inlineScriptConfig) return null
        val inlineScriptExpression = ParadoxInlineScriptHandler.getInlineScriptExpression(file) ?: return null
        val selector = inlineScriptSelector(file.project, file)
        val hasUsages = ParadoxInlineScriptUsageSearch.search(inlineScriptExpression, selector).find() != null
        if(hasUsages) return null
        
        val holder = ProblemsHolder(manager, file, isOnTheFly)
        val description = PlsBundle.message("inspection.script.unusedInlineScript.description", inlineScriptExpression)
        holder.registerProblem(file, description, ProblemHighlightType.LIKE_UNUSED_SYMBOL)
        return holder.resultsArray
    }
}