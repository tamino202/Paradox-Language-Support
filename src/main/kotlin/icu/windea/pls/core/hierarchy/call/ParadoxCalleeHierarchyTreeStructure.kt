package icu.windea.pls.core.hierarchy.call

import com.intellij.ide.hierarchy.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.scope.type.*
import icu.windea.pls.lang.inline.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.psi.*
import java.util.*

class ParadoxCalleeHierarchyTreeStructure(
    project: Project,
    element: PsiElement,
    val rootDefinitionInfo: ParadoxDefinitionInfo?
) : HierarchyTreeStructure(project, ParadoxCallHierarchyNodeDescriptor(project, null, element, true, false)) {
    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<out HierarchyNodeDescriptor> {
        descriptor as ParadoxCallHierarchyNodeDescriptor
        val element = descriptor.psiElement ?: return HierarchyNodeDescriptor.EMPTY_ARRAY
        val descriptors = mutableMapOf<String, ParadoxCallHierarchyNodeDescriptor>()
        when {
            element is ParadoxScriptDefinitionElement -> {
                processElement(element, descriptor, descriptors)
            }
            element is ParadoxLocalisationProperty -> {
                processElement(element, descriptor, descriptors)
            }
        }
        return descriptors.values.toTypedArray()
    }
    
    private fun processElement(element: PsiElement, descriptor: HierarchyNodeDescriptor, descriptors: MutableMap<String, ParadoxCallHierarchyNodeDescriptor>) {
        val scopeType = getHierarchySettings().scopeType
        val scope = ParadoxSearchScopeTypes.get(scopeType).getGlobalSearchScope(myProject, element)
        element.acceptChildren(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                //兼容向下内联的情况
                if(element is ParadoxScriptMemberElement) {
                    val inlined = ParadoxScriptMemberElementInlineSupport.inlineElement(element, LinkedList())
                    if(inlined != null) {
                        processElement(inlined, descriptor, descriptors)
                        return
                    }
                }
                when {
                    element is ParadoxScriptedVariableReference -> {
                        addDescriptor(element) //scripted_variable
                    }
                    element is ParadoxScriptExpressionElement && element.isExpression() -> {
                        addDescriptor(element)
                    }
                    element is ParadoxLocalisationPropertyReference -> {
                        addDescriptor(element) //localisation
                    }
                    element is ParadoxLocalisationCommandField -> {
                        addDescriptor(element) //<scripted_loc>
                    }
                }
                if(element.isExpressionOrMemberContext()) super.visitElement(element)
            }
            
            private fun addDescriptor(element: PsiElement) {
                ProgressManager.checkCanceled()
                val resolved = element.reference?.resolve()
                when(resolved) {
                    is ParadoxScriptScriptedVariable -> {
                        if(!getSettings().hierarchy.showScriptedVariablesInCallHierarchy) return //不显示
                        val key = "v:${resolved.name}"
                        if(descriptors.containsKey(key)) return //去重
                        val resolvedFile = selectFile(resolved)
                        if(resolvedFile != null && scope != null && !scope.contains(resolvedFile)) return
                        descriptors.put(key, ParadoxCallHierarchyNodeDescriptor(myProject, descriptor, resolved, false, false))
                    }
                    is ParadoxScriptDefinitionElement -> {
                        if(!getSettings().hierarchy.showDefinitionsInCallHierarchy) return //不显示
                        val definitionInfo = resolved.definitionInfo ?: return
                        if(!getSettings().hierarchy.showDefinitionsInCallHierarchy(rootDefinitionInfo, definitionInfo)) return //不显示
                        val key = "d:${definitionInfo.name}: ${definitionInfo.type}"
                        if(descriptors.containsKey(key)) return //去重
                        val resolvedFile = selectFile(resolved)
                        if(resolvedFile != null && scope != null && !scope.contains(resolvedFile)) return
                        descriptors.put(key, ParadoxCallHierarchyNodeDescriptor(myProject, descriptor, resolved, false, false))
                    }
                    is ParadoxLocalisationProperty -> {
                        if(!getSettings().hierarchy.showLocalisationsInCallHierarchy) return //不显示
                        val localisationInfo = resolved.localisationInfo ?: return
                        val key = "l:${localisationInfo.name}"
                        if(descriptors.containsKey(key)) return //去重
                        val resolvedFile = selectFile(resolved)
                        if(resolvedFile != null && scope != null && !scope.contains(resolvedFile)) return
                        descriptors.put(key, ParadoxCallHierarchyNodeDescriptor(myProject, descriptor, resolved, false, false))
                    }
                }
            }
        })
    }
    
    private fun getHierarchySettings() = ParadoxCallHierarchyBrowserSettings.getInstance(myProject)
}