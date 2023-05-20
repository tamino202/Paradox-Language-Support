package icu.windea.pls.lang.scope.impl

import com.intellij.openapi.application.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.search.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.core.*
import icu.windea.pls.core.index.hierarchy.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.scope.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.lang.scope.*
import icu.windea.pls.script.*
import icu.windea.pls.script.psi.*

/**
 * 如果某个`event`在另一个`event`中被调用，
 * 则将此另一个`event`的root作用域推断为此`event`的from作用域，
 * 此调用此另一个`event`的`event`的root作用域推断为此`event`的fromfrom作用域，依此类推直到fromfromfromfrom作用域。
 */
class ParadoxEventFromEventInferredScopeContextProvider : ParadoxDefinitionInferredScopeContextProvider {
    companion object {
        val cachedScopeContextInferenceInfoKey = Key.create<CachedValue<ParadoxScopeContextInferenceInfo>>("paradox.cached.scopeContextInferenceInfo.fromEvent")
    }
    
    override fun getScopeContext(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo): ParadoxScopeContextInferenceInfo? {
        if(!getSettings().inference.eventScopeContext) return null
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
        eventName: String,
        searchScope: GlobalSearchScope,
        scopeContextMap: MutableMap<String, String?>,
        configGroup: CwtConfigGroup,
        depth: Int = 1
    ): Boolean {
        val project = configGroup.project
        return withRecursionGuard("icu.windea.pls.lang.scope.impl.ParadoxEventFromEventInferredScopeContextProvider.doProcessQuery") {
            if(depth == 1) stackTrace.addLast(eventName) 
            
            val toRef = "from".repeat(depth)
            ProgressManager.checkCanceled()
            FileTypeIndex.processFiles(ParadoxScriptFileType, p@{ file ->
                ProgressManager.checkCanceled()
                val data = ParadoxEventHierarchyIndex.getData(file, project) ?: return@p true
                val eventInfos = data.eventToEventInfosMap[eventName] ?: return@p true
                eventInfos.forEach { eventInfo ->
                    withCheckRecursion(eventInfo.name) {
                        val newRefScope = eventInfo.scope
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
                        doProcessQuery(eventInfo.name, searchScope, scopeContextMap, configGroup)
                    } ?: return@p false
                }
                true
            }, searchScope)
        } ?: false
    }
    
    override fun getMessage(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, info: ParadoxScopeContextInferenceInfo): String {
        val eventId = definitionInfo.name
        return PlsBundle.message("script.annotator.scopeContext.2", eventId)
    }
    
    override fun getErrorMessage(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, info: ParadoxScopeContextInferenceInfo): String {
        val eventId = definitionInfo.name
        return PlsBundle.message("script.annotator.scopeContext.2.conflict", eventId)
    }
}