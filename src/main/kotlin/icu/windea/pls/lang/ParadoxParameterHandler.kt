package icu.windea.pls.lang

import com.google.common.cache.*
import com.intellij.application.options.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.util.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.codeInsight.completion.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.util.*
import icu.windea.pls.lang.cwt.config.*
import icu.windea.pls.lang.cwt.expression.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.lang.parameter.*
import icu.windea.pls.script.codeStyle.*
import icu.windea.pls.script.psi.*
import java.util.*

@Suppress("UNUSED_PARAMETER")
object ParadoxParameterHandler {
    val supportKey = Key.create<ParadoxParameterSupport>("paradox.parameter.support")
    val inferredConfigKey = Key.create<CwtValueConfig>("paradox.parameter.inferredConfig")
    val inferredContextConfigsKey = Key.create<List<CwtMemberConfig<*>>>("paradox.parameter.inferredContextConfigs")
    val parameterCacheKey = KeyWithDefaultValue.create<Cache<String, ParadoxParameterElement>>("paradox.parameter.cache") {
        CacheBuilder.newBuilder().recordStats().buildCache()
    }
    val parameterModificationTrackerKey = Key.create<ModificationTracker>("paradox.parameter.modificationTracker")
    val parameterModificationCountKey = Key.create<Long>("paradox.parameter.modificationCount")
    
    fun getContextInfo(context: ParadoxScriptDefinitionElement): ParadoxParameterContextInfo? {
        if(!ParadoxParameterSupport.isContext(context)) return null
        return CachedValuesManager.getCachedValue(context, PlsKeys.cachedParametersKey) {
            val value = doGetContextInfo(context)
            CachedValueProvider.Result(value, context)
        }
    }
    
    private fun doGetContextInfo(context: ParadoxScriptDefinitionElement): ParadoxParameterContextInfo? {
        val file = context.containingFile
        val gameType = selectGameType(file) ?: return null
        val parameters = sortedMapOf<String, MutableList<ParadoxParameterInfo>>() //按名字进行排序
        val fileConditionStack = LinkedList<ReversibleValue<String>>()
        context.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if(element is ParadoxParameter) visitParameter(element)
                if(element is ParadoxScriptParameterConditionExpression) visitParadoxConditionExpression(element)
                super.visitElement(element)
            }
            
            private fun visitParadoxConditionExpression(element: ParadoxScriptParameterConditionExpression) {
                ProgressManager.checkCanceled()
                var operator = true
                var value = ""
                element.processChild p@{
                    val elementType = it.elementType
                    when(elementType) {
                        ParadoxScriptElementTypes.NOT_SIGN -> operator = false
                        ParadoxScriptElementTypes.PARAMETER_CONDITION_PARAMETER -> value = it.text
                    }
                    true
                }
                //value may be empty (invalid condition expression)
                fileConditionStack.addLast(ReversibleValue(operator, value))
                super.visitElement(element)
            }
            
            private fun visitParameter(element: ParadoxParameter) {
                ProgressManager.checkCanceled()
                val name = element.name ?: return
                val defaultValue = element.defaultValue
                val conditionalStack = if(fileConditionStack.isEmpty()) null else LinkedList(fileConditionStack)
                val info = ParadoxParameterInfo(element.createPointer(file), name, defaultValue, conditionalStack)
                parameters.getOrPut(name) { mutableListOf() }.add(info)
                //不需要继续向下遍历
            }
            
            override fun elementFinished(element: PsiElement?) {
                if(element is ParadoxScriptParameterCondition) finishParadoxCondition()
            }
            
            private fun finishParadoxCondition() {
                fileConditionStack.removeLast()
            }
        })
        return ParadoxParameterContextInfo(file.project, gameType, parameters)
    }
    
    fun completeParameters(element: PsiElement, context: ProcessingContext, result: CompletionResultSet) {
        ProgressManager.checkCanceled()
        //向上找到参数上下文
        val parameterContext = ParadoxParameterSupport.findContext(element) ?: return
        val parameterContextInfo = getContextInfo(parameterContext) ?: return
        if(parameterContextInfo.parameters.isEmpty()) return
        for((parameterName, parameterInfos) in parameterContextInfo.parameters) {
            ProgressManager.checkCanceled()
            val parameter = parameterInfos.firstNotNullOfOrNull { it.element } ?: continue
            //排除当前正在输入的那个
            if(parameterInfos.size == 1 && element isSamePosition parameter) continue
            val parameterElement = ParadoxParameterSupport.resolveParameter(parameter)
                ?: continue
            val lookupElement = LookupElementBuilder.create(parameterElement, parameterName)
                .withIcon(PlsIcons.Parameter)
                .withTypeText(parameterElement.contextName, parameterContext.icon, true)
            result.addElement(lookupElement)
        }
    }
    
    fun completeArguments(element: PsiElement, context: ProcessingContext, result: CompletionResultSet) {
        ProgressManager.checkCanceled()
        if(context.quoted) return //输入参数不允许用引号括起
        val from = ParadoxParameterContextReferenceInfo.From.Argument
        val config = context.config ?: return
        val completionOffset = context.parameters?.offset ?: return
        val contextReferenceInfo = ParadoxParameterSupport.getContextReferenceInfo(element, from, config, completionOffset) ?: return
        val argumentNames = contextReferenceInfo.argumentNames.toMutableSet()
        val namesToDistinct = mutableSetOf<String>().synced()
        //整合查找到的所有参数上下文
        val insertSeparator = context.isKey == true && context.contextElement !is ParadoxScriptPropertyKey
        ParadoxParameterSupport.processContext(element, contextReferenceInfo, true) p@{ parameterContext ->
            ProgressManager.checkCanceled()
            val parameterContextInfo = getContextInfo(parameterContext) ?: return@p true
            if(parameterContextInfo.parameters.isEmpty()) return@p true
            for((parameterName, parameterInfos) in parameterContextInfo.parameters) {
                //排除已输入的
                if(parameterName in argumentNames) continue
                if(!namesToDistinct.add(parameterName)) continue
                
                val parameter = parameterInfos.firstNotNullOfOrNull { it.element } ?: continue
                val parameterElement = ParadoxParameterSupport.resolveParameter(parameter)
                    ?: continue
                val lookupElement = LookupElementBuilder.create(parameterElement, parameterName)
                    .withIcon(PlsIcons.Parameter)
                    .withTypeText(parameterElement.contextName, parameterContext.icon, true)
                    .letIf(insertSeparator) {
                        it.withInsertHandler { c, _ ->
                            val editor = c.editor
                            val customSettings = CodeStyle.getCustomSettings(c.file, ParadoxScriptCodeStyleSettings::class.java)
                            val text = if(customSettings.SPACE_AROUND_PROPERTY_SEPARATOR) " = " else "="
                            EditorModificationUtil.insertStringAtCaret(editor, text, false, true)
                        }
                    }
                result.addElement(lookupElement)
            }
            true
        }
    }
    
    /**
     * 尝试推断得到参数对应的CWT规则。
     */
    fun getInferredConfig(parameterElement: ParadoxParameterElement): CwtValueConfig? {
        val cacheKey = parameterElement.contextKey + "@" + parameterElement.name
        val parameterCache = selectRootFile(parameterElement.parent)?.getUserData(parameterCacheKey) ?: return null
        val cached = parameterCache.get(cacheKey)
        if(cached != null) {
            val modificationTracker = cached.getUserData(parameterModificationTrackerKey)
            if(modificationTracker != null) {
                val modificationCount = cached.getUserData(parameterModificationCountKey) ?: 0
                if(modificationCount == modificationTracker.modificationCount) {
                    val resolved = cached.getUserData(inferredConfigKey)
                    if(resolved != null) {
                        return resolved.takeIf { it !== CwtValueConfig.EmptyConfig }
                    }
                }
            }
        }
        
        val resolved = doGetInferredConfig(parameterElement)
        
        val ep = parameterElement.getUserData(supportKey)
        if(ep != null) {
            val modificationTracker = ep.getModificationTracker(parameterElement)
            if(modificationTracker != null) {
                parameterElement.putUserData(inferredConfigKey, resolved ?: CwtValueConfig.EmptyConfig)
                parameterElement.putUserData(parameterModificationTrackerKey, modificationTracker)
                parameterElement.putUserData(parameterModificationCountKey, modificationTracker.modificationCount)
                parameterCache.put(cacheKey, parameterElement)
            }
        }
        
        return resolved
    }
    
    private fun doGetInferredConfig(parameterElement: ParadoxParameterElement): CwtValueConfig? {
        var result: CwtValueConfig? = null
        ParadoxParameterSupport.processContext(parameterElement, true) p@{ context ->
            ProgressManager.checkCanceled()
            val contextInfo = getContextInfo(context) ?: return@p true
            val config = getInferredConfig(parameterElement.name, contextInfo)
            if(config == null) return@p true
            if(result == null) {
                result = config
            } else {
                result = ParadoxConfigMergeHandler.shallowMergeValueConfig(result!!, config)
                if(result == null) return@p false //存在冲突
            }
            true
        }
        return result
    }
    
    fun getInferredConfig(parameterName: String, parameterContextInfo: ParadoxParameterContextInfo): CwtValueConfig? {
        //如果推断得到的规则不唯一，则返回null
        val parameterInfos = parameterContextInfo.parameters.get(parameterName)
        if(parameterInfos.isNullOrEmpty()) return null
        var result: CwtValueConfig? = null
        parameterInfos.forEach f@{ parameterInfo ->
            ProgressManager.checkCanceled()
            val config = ParadoxParameterInferredConfigProvider.getConfig(parameterInfo, parameterContextInfo)
            if(config == null) return@f
            if(result == null) {
                result = config
            } else {
                result = ParadoxConfigMergeHandler.shallowMergeValueConfig(result!!, config)
                if(result == null) return@f //存在冲突
            }
        }
        return result
    }
    
    /**
     * 尝试推断得到参数对应的上下文CWT规则。
     */
    fun getInferredContextConfigs(parameterElement: ParadoxParameterElement): List<CwtMemberConfig<*>> {
        val cacheKey = parameterElement.contextKey + "@" + parameterElement.name
        val parameterCache = selectRootFile(parameterElement.parent)?.getUserData(parameterCacheKey) ?: return emptyList()
        val cached = parameterCache.get(cacheKey)
        if(cached != null) {
            val modificationTracker = cached.getUserData(parameterModificationTrackerKey)
            if(modificationTracker != null) {
                val modificationCount = cached.getUserData(parameterModificationCountKey) ?: 0
                if(modificationCount == modificationTracker.modificationCount) {
                    val resolved = cached.getUserData(inferredContextConfigsKey)
                    if(resolved != null) {
                        return resolved
                    }
                }
            }
        }
        
        val resolved = try {
            doGetInferredContextConfigs(parameterElement)
        } catch(e: UnsupportedOperationException) {
            listOf(CwtValueConfig.EmptyConfig)
        }
        
        val ep = parameterElement.getUserData(supportKey)
        if(ep != null) {
            val modificationTracker = ep.getModificationTracker(parameterElement)
            if(modificationTracker != null) {
                parameterElement.putUserData(inferredContextConfigsKey, resolved)
                parameterElement.putUserData(parameterModificationTrackerKey, modificationTracker)
                parameterElement.putUserData(parameterModificationCountKey, modificationTracker.modificationCount)
                parameterCache.put(cacheKey, parameterElement)
            }
        }
        
        return resolved
    }
    
    private fun doGetInferredContextConfigs(parameterElement: ParadoxParameterElement): List<CwtMemberConfig<*>> {
        var resultConfigs: List<CwtMemberConfig<*>>? = null
        ParadoxParameterSupport.processContext(parameterElement, true) p@{ context ->
            ProgressManager.checkCanceled()
            val contextInfo = getContextInfo(context) ?: return@p true
            val configs = getInferredContextConfigs(parameterElement.name, contextInfo)
            if(configs.isEmpty()) return@p true
            if(resultConfigs == null) {
                resultConfigs = configs
            } else {
                val mergedConfigs = ParadoxConfigMergeHandler.mergeConfigs(resultConfigs!!, configs)
                if(mergedConfigs.isEmpty() && resultConfigs!!.isNotEmpty()) {
                    //存在冲突
                    resultConfigs = null
                    return@p false
                } else {
                    resultConfigs = mergedConfigs
                }
            }
            true
        }
        return resultConfigs.orEmpty()
    }
    
    fun getInferredContextConfigs(parameterName: String, parameterContextInfo: ParadoxParameterContextInfo): List<CwtMemberConfig<*>> {
        val parameterInfos = parameterContextInfo.parameters.get(parameterName)
        if(parameterInfos.isNullOrEmpty()) return emptyList()
        var resultConfigs: List<CwtMemberConfig<*>>? = null
        parameterInfos.forEach f@{ parameterInfo ->
            ProgressManager.checkCanceled()
            val configs = ParadoxParameterInferredConfigProvider.getContextConfigs(parameterInfo, parameterContextInfo)
            if(configs.isNullOrEmpty()) return@f
            if(resultConfigs == null) {
                resultConfigs = configs
            } else {
                val mergedConfigs = ParadoxConfigMergeHandler.mergeConfigs(resultConfigs!!, configs)
                if(mergedConfigs.isEmpty() && resultConfigs!!.isNotEmpty()) {
                    //存在冲突
                    resultConfigs = null
                    return@f
                } else {
                    resultConfigs = mergedConfigs
                }
            }
        }
        return resultConfigs.orEmpty()
    }
    
    fun isIgnoredInferredConfig(config: CwtValueConfig): Boolean {
        return when(config.expression.type) {
            CwtDataType.Any, CwtDataType.Other -> true
            else -> false
        }
    }
}