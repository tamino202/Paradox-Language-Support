package icu.windea.pls.script.inspections.bug

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.ep.priority.*
import icu.windea.pls.lang.quickfix.*
import icu.windea.pls.lang.search.*
import icu.windea.pls.lang.search.selector.*
import icu.windea.pls.model.*
import icu.windea.pls.script.psi.*

/**
 * 检查（全局）封装变量的重载是否不正确。（覆盖规则为FIOS）
 */
class IncorrectOverriddenForDefinitionInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        val project = holder.project
        val fileInfo = file.fileInfo ?: return PsiElementVisitor.EMPTY_VISITOR
        val virtualFile = file.virtualFile
        val inProject = virtualFile != null && ProjectFileIndex.getInstance(project).isInContent(virtualFile)
        if(!inProject) return PsiElementVisitor.EMPTY_VISITOR //only for project files
        
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if(element is ParadoxScriptProperty) {
                    val definitionInfo = element.definitionInfo
                    if(definitionInfo != null) visitDefinition(element, definitionInfo)
                }
            }
            
            private fun visitDefinition(element: ParadoxScriptProperty, definitionInfo: ParadoxDefinitionInfo) {
                val priority = ParadoxPriorityProvider.getPriority(element)
                if(priority == ParadoxPriority.ORDERED) return //only for FIOS and LIOS
                val selector = definitionSelector(project, file)
                val name = definitionInfo.name
                val type = definitionInfo.type
                if(name.isParameterized()) return ////parameterized -> ignored
                val results = ParadoxDefinitionSearch.search(name, type, selector).findAll()
                if(results.size < 2) return //no override -> skip
                val firstResult = results.first()
                val firstRootInfo = firstResult.fileInfo?.rootInfo ?: return
                val rootInfo = fileInfo.rootInfo
                if(firstRootInfo.rootFile != rootInfo.rootFile) {
                    //different root file -> incorrect override
                    val locationElement = element.propertyKey
                    val message = PlsBundle.message("inspection.script.bug.incorrectOverriddenForDefinition.description", name, priority)
                    val fix = NavigateToOverriddenDefinitionsFix(name, element, results)
                    holder.registerProblem(locationElement, message, fix)
                }
            }
        }
    }
    
    private class NavigateToOverriddenDefinitionsFix(key: String, element: PsiElement, definitions: Collection<PsiElement>) : NavigateToFix(key, element, definitions) {
        override fun getText() = PlsBundle.message("inspection.script.bug.incorrectOverriddenForDefinition.quickfix.1")
        
        override fun getPopupTitle(editor: Editor) =
            PlsBundle.message("inspection.script.bug.incorrectOverriddenForDefinition.quickFix.1.popup.title", key)
        
        override fun getPopupText(editor: Editor, value: PsiElement) =
            PlsBundle.message("inspection.script.bug.incorrectOverriddenForDefinition.quickFix.1.popup.text", key, editor.document.getLineNumber(value.textOffset))
    }
}
