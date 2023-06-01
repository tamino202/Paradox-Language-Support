package icu.windea.pls.lang.scope.impl

import com.intellij.openapi.application.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.search.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.core.*
import icu.windea.pls.core.annotations.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.scope.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.lang.scope.*
import icu.windea.pls.script.psi.*

/**
 * 如果某个event在另一个event中被调用，
 * 则将此另一个event的root作用域推断为此event的from作用域，
 * 将调用此另一个event的event的root作用域推断为此event的fromfrom作用域，
 * 依此类推直到fromfromfromfrom作用域。
 * 如果有声明scopes = { from = ... }，则将此event的from作用域推断为这个声明中from对应的作用域，
 * 依此类推直到fromfromfromfrom作用域。
 */
@SlowApi
class ParadoxEventFromEventInferredScopeContextProvider : ParadoxDefinitionInferredScopeContextProvider {
    companion object {
        val cachedScopeContextInferenceInfoKey = Key.create<CachedValue<ParadoxScopeContextInferenceInfo>>("paradox.cached.scopeContextInferenceInfo.event.fromEvent")
    }
    
    override fun getScopeContext(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo): ParadoxScopeContextInferenceInfo? {
        if(!getSettings().inference.eventScopeContextFromEvent) return null
        if(definitionInfo.type != "event") return null
        return doGetScopeContextFromCache(definition)
    }
    
    private fun doGetScopeContextFromCache(definition: ParadoxScriptDefinitionElement): ParadoxScopeContextInferenceInfo? {
        return CachedValuesManager.getCachedValue(definition, cachedScopeContextInferenceInfoKey) {
            ProgressManager.checkCanceled()
            val value = doGetScopeContext(definition)
            val tracker0 = ParadoxPsiModificationTracker.DefinitionScopeContextInferenceTracker
            val tracker = ParadoxPsiModificationTracker.getInstance(definition.project).ScriptFileTracker("common/events:txt")
            CachedValueProvider.Result.create(value, tracker0, tracker)
        }
    }
    
    private fun doGetScopeContext(definition: ParadoxScriptDefinitionElement): ParadoxScopeContextInferenceInfo? {
        val definitionInfo = definition.definitionInfo ?: return null
        //optimize search scope
        val searchScope = runReadAction { ParadoxSearchScope.fromElement(definition) }
            ?.withFilePath("events", "txt")
            ?: return null
        val configGroup = definitionInfo.configGroup
        val thisEventName = definitionInfo.name
        val thisEventScope = ParadoxEventHandler.getScope(definitionInfo)
        val scopeContextMap = mutableMapOf<String, String?>()
        scopeContextMap.put("this", thisEventScope)
        scopeContextMap.put("root", thisEventScope)
        var hasConflict = false
        val r = doProcessQuery(thisEventName, searchScope, scopeContextMap, configGroup)
        if(!r) hasConflict = true
        val resultScopeContextMap = scopeContextMap.takeIf { it.size > 2 } ?: return null
        return ParadoxScopeContextInferenceInfo(resultScopeContextMap, hasConflict)
    }
    
    private fun doProcessQuery(
        thisEventName: String,
        searchScope: GlobalSearchScope,
        scopeContextMap: MutableMap<String, String?>,
        configGroup: CwtConfigGroup,
        depth: Int = 1
    ): Boolean {
        val gameType = configGroup.gameType ?: return true
        return withRecursionGuard("icu.windea.pls.lang.scope.impl.ParadoxEventFromEventInferredScopeContextProvider.doProcessQuery") {
            if(depth == 1) stackTrace.addLast(thisEventName) 
            
            val toRef = "from".repeat(depth)
            ParadoxDefinitionHierarchyHandler.processEventsInEvent(gameType, searchScope) p@{ file, infos ->
                infos.forEachFast f@{ info ->
                    val eventName = info.expression
                    if(eventName != thisEventName) return@f
                    val containingEventName = info.definitionName
                    withCheckRecursion(containingEventName) {
                        val scopesElementOffset = info.scopesElementOffset!!
                        if(scopesElementOffset != -1) {
                            //从scopes = { ... }中推断
                            val psiFile = file.toPsiFile(configGroup.project) ?: return@p false
                            val scopesElement = psiFile.findElementAt(scopesElementOffset)?.parentOfType<ParadoxScriptProperty>() ?: return@p false
                            val scopesBlockElement = scopesElement.block ?: return@p false
                            val scopeContextOfScopesElement = ParadoxScopeHandler.getScopeContext(scopesElement)
                            scopesBlockElement.processProperty(inline = true) pp@{
                                if(scopeContextOfScopesElement == null) {
                                    val n = it.name.lowercase()
                                    if(configGroup.systemLinks.get(n)?.baseId != "from") return@pp true
                                    scopeContextMap.put(n, ParadoxScopeHandler.anyScopeId)
                                    return@pp true
                                }
                                
                                val pv = it.propertyValue ?: return@pp true
                                val scopeField = pv.text
                                if(scopeField.isLeftQuoted()) return@pp true
                                val textRange = TextRange.create(0, scopeField.length)
                                val scopeFieldExpression = ParadoxScopeFieldExpression.resolve(scopeField, textRange, configGroup, true) ?: return@pp true
                                val scopeContextOfEachScope = ParadoxScopeHandler.getScopeContext(scopeFieldExpression, scopeContextOfScopesElement)
                                
                                val n = it.name.lowercase()
                                if(configGroup.systemLinks.get(n)?.baseId != "from") return@pp true
                                scopeContextMap.put(n, scopeContextOfEachScope.scope.id)
                                
                                true
                            }
                            return@f
                        }
                        
                        val newRefScope = info.containingEventScope!!
                        val oldRefScope = scopeContextMap.get(toRef)
                        if(oldRefScope == null) {
                            scopeContextMap.put(toRef, newRefScope)
                        } else {
                            val refScope = ParadoxScopeHandler.mergeScopeId(oldRefScope, newRefScope)
                            if(refScope == null) {
                                return@p false
                            }
                            scopeContextMap.put(toRef, refScope)
                        }
                        if(depth >= 4) return@p true
                        doProcessQuery(containingEventName, searchScope, scopeContextMap, configGroup)
                    }
                }
                true
            }
        } ?: false
    }
    
    override fun getMessage(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, info: ParadoxScopeContextInferenceInfo): String {
        return PlsBundle.message("script.annotator.scopeContext.2", definitionInfo.name)
    }
    
    override fun getErrorMessage(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, info: ParadoxScopeContextInferenceInfo): String {
        return PlsBundle.message("script.annotator.scopeContext.2.conflict", definitionInfo.name)
    }
}