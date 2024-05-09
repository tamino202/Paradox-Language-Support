package icu.windea.pls.lang.inspections.localisation.common

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.ui.dsl.builder.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.localisation.psi.*
import javax.swing.*

/**
 * 无法解析的图标的检查。
 *
 * @property ignoredIconNames （配置项）需要忽略的图标名的模式。使用GLOB模式。忽略大小写。
 */
class UnresolvedIconInspection : LocalInspectionTool() {
    @JvmField var ignoredIconNames = ""
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                ProgressManager.checkCanceled()
                if(element is ParadoxLocalisationIcon) visitIcon(element)
            }
            
            private fun visitIcon(element: ParadoxLocalisationIcon) {
                val iconName = element.name ?: return
                ignoredIconNames.splitOptimized(';').forEach {
                    if(iconName.matchesGlobPattern(it, true)) return //忽略
                }
                val reference = element.reference
                if(reference == null || reference.resolve() != null) return
                val location = element.iconId ?: return
                holder.registerProblem(location, PlsBundle.message("inspection.localisation.unresolvedIcon.description", iconName), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            }
        }
    }
    
    override fun createOptionsPanel(): JComponent {
        return panel {
            row {
                label(PlsBundle.message("inspection.localisation.unresolvedIcon.option.ignoredIconNames"))
            }
            row {
                textField()
                    .bindText(::ignoredIconNames)
                    .bindWhenTextChanged(::ignoredIconNames)
                    .comment(PlsBundle.message("inspection.localisation.unresolvedIcon.option.ignoredIconNames.comment"))
                    .align(Align.FILL)
                    .resizableColumn()
            }
        }
    }
} 
