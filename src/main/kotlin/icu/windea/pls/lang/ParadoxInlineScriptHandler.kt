package icu.windea.pls.lang

import com.intellij.openapi.application.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.search.selector.chained.*
import icu.windea.pls.lang.ParadoxConfigMatcher.Options
import icu.windea.pls.lang.cwt.config.*
import icu.windea.pls.lang.cwt.expression.*
import icu.windea.pls.lang.expression.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.script.psi.*

object ParadoxInlineScriptHandler {
    const val inlineScriptKey = "inline_script"
    
    val inlineScriptPathExpression = CwtValueExpression.resolve("filepath[common/inline_scripts/,.txt]")
    
    val cachedInlineScriptInfoKey = Key.create<CachedValue<ParadoxInlineScriptUsageInfo>>("paradox.cached.inlineScriptInfo")
    val cachedInlineScriptUsageInfoKey = Key.create<CachedValue<ParadoxInlineScriptUsageInfo1>>("paradox.cached.inlineScriptUsageInfo")
    
    fun getInfo(element: ParadoxScriptProperty): ParadoxInlineScriptUsageInfo? {
        val name = element.name.lowercase()
        if(name != inlineScriptKey) return null
        return doGetInfoFromCache(element)
    }
    
    private fun doGetInfoFromCache(element: ParadoxScriptProperty): ParadoxInlineScriptUsageInfo? {
        return CachedValuesManager.getCachedValue(element, cachedInlineScriptInfoKey) {
            ProgressManager.checkCanceled()
            val file = element.containingFile ?: return@getCachedValue null
            val value = runReadAction { doGetInfo(element, file) }
            //invalidated on file modification
            CachedValueProvider.Result.create(value, file)
        }
    }
    
    private fun doGetInfo(element: ParadoxScriptProperty, file: PsiFile = element.containingFile): ParadoxInlineScriptUsageInfo? {
        //这里不能调用ParadoxConfigHandler.getConfigs，因为需要处理内联的情况，会导致StackOverflow
        
        val fileInfo = file.fileInfo ?: return null
        val gameType = fileInfo.rootInfo.gameType
        val project = file.project
        val configGroup = getCwtConfig(project).get(gameType)
        val inlineConfigs = configGroup.inlineConfigGroup[inlineScriptKey] ?: return null
        val propertyValue = element.propertyValue ?: return null
        val matchOptions = Options.SkipIndex or Options.SkipScope or Options.Fast
        val inlineConfig = ParadoxConfigMatcher.find(inlineConfigs, matchOptions) {
            val expression = ParadoxDataExpression.resolve(propertyValue, matchOptions)
            ParadoxConfigMatcher.matches(propertyValue, expression, it.config.valueExpression, it.config, configGroup)
        }
        if(inlineConfig == null) return null
        val expression = getExpressionFromInlineConfig(propertyValue, inlineConfig) ?: return null
        if(expression.isParameterized()) return null
        val elementOffset = element.startOffset
        return ParadoxInlineScriptUsageInfo(expression, elementOffset, gameType)
    }
    
    private fun getExpressionLocation(it: CwtMemberConfig<*>): String? {
        return it.findOption { it.key == "inline_script_expression" }?.stringValue
    }
    
    fun getExpressionFromInlineConfig(propertyValue: ParadoxScriptValue, inlineConfig: CwtInlineConfig): String? {
        if(inlineConfig.name != inlineScriptKey) return null
        val expressionLocation = getExpressionLocation(inlineConfig.config) ?: return null
        val expressionElement = if(expressionLocation.isEmpty()) {
            propertyValue.castOrNull<ParadoxScriptString>()
        } else {
            propertyValue.findProperty(expressionLocation, conditional = true)?.propertyValue?.castOrNull<ParadoxScriptString>()
        }
        return expressionElement?.stringValue()
    }
    
    fun getInlineScriptFile(expression: String, contextElement: PsiElement, project: Project): ParadoxScriptFile? {
        val filePath = getInlineScriptFilePath(expression)
        val selector = fileSelector(project, contextElement).contextSensitive()
        return ParadoxFilePathSearch.search(filePath, null, selector).find()?.toPsiFile(project)?.castOrNull()
    }
    
    fun processInlineScriptFile(expression: String, contextElement: PsiElement, project: Project, onlyMostRelevant: Boolean = false, processor: (ParadoxScriptFile) -> Boolean): Boolean {
        val filePath = getInlineScriptFilePath(expression)
        val selector = fileSelector(project, contextElement).contextSensitive()
        return ParadoxFilePathSearch.search(filePath, null, selector).processQueryAsync(onlyMostRelevant) p@{
            ProgressManager.checkCanceled()
            val file = it.toPsiFile(project)?.castOrNull<ParadoxScriptFile>() ?: return@p true
            processor(file)
            true
        }
    }
    
    fun getInlineScriptFilePath(pathReference: String): String? {
        val configExpression = inlineScriptPathExpression
        return ParadoxPathReferenceExpressionSupport.get(configExpression)?.resolvePath(configExpression, pathReference.normalizePath())
    }
    
    fun getInlineScriptExpression(file: VirtualFile): String? {
        val fileInfo = file.fileInfo ?: return null
        return doGetInlineScriptExpression(fileInfo)
    }
    
    fun getInlineScriptExpression(file: PsiFile): String? {
        if(file !is ParadoxScriptFile) return null
        val fileInfo = file.fileInfo ?: return null
        return doGetInlineScriptExpression(fileInfo)
    }
    
    private fun doGetInlineScriptExpression(fileInfo: ParadoxFileInfo): String? {
        val filePath = fileInfo.pathToEntry.path
        val configExpression = inlineScriptPathExpression
        return ParadoxPathReferenceExpressionSupport.get(configExpression)?.extract(configExpression, null, filePath)?.takeIfNotEmpty()
    }
    
    /**
     * 得到内联脚本的使用位置对应的属性信息，包括是否存在冲突等。
     */
    fun getInlineScriptUsageInfo(file: ParadoxScriptFile): ParadoxInlineScriptUsageInfo1? {
        ProgressManager.checkCanceled()
        return getUsageInfoFromCache(file)
    }
    
    private fun getUsageInfoFromCache(file: ParadoxScriptFile): ParadoxInlineScriptUsageInfo1? {
        return CachedValuesManager.getCachedValue(file, cachedInlineScriptUsageInfoKey) {
            ProgressManager.checkCanceled()
            val value = doGetInlineScriptUsageInfo(file)
            //invalidated on file modification or ScriptFileTracker
            val tracker = ParadoxPsiModificationTracker.getInstance(file.project).ScriptFileTracker
            CachedValueProvider.Result.create(value, file, tracker)
        }
    }
    
    private fun doGetInlineScriptUsageInfo(file: ParadoxScriptFile): ParadoxInlineScriptUsageInfo1? {
        val fileInfo = file.fileInfo ?: return null
        val expression = getInlineScriptExpression(file) ?: return null
        val project = file.project
        var element: ParadoxScriptProperty? = null
        var hasConflict = false
        var hasRecursion = false
        val configs: MutableList<CwtMemberConfig<*>> = mutableListOf()
        withRecursionGuard("icu.windea.pls.lang.ParadoxInlineScriptHandler.doGetInlineScriptUsageInfo") {
            stackTrace.addLast(fileInfo.pathToEntry.path)
            val selector = inlineScriptSelector(project, file)
            ParadoxInlineScriptSearch.search(expression, selector).processQueryAsync p@{ info ->
                ProgressManager.checkCanceled()
                val key = info.file?.fileInfo?.pathToEntry?.path ?: return@p true
                val e = info.file?.findElementAt(info.elementOffset) ?: return@p true
                val p = e.parentOfType<ParadoxScriptProperty>() ?: return@p true
                if(p.name.lowercase() != inlineScriptKey) return@p true
                if(element == null) {
                    element = p
                }
                onRecursion(key) {
                    //如果发生SOF，采用的element不能是当前正在遍历的p（此时标为存在递归并结束检查）
                    hasConflict = false
                    hasRecursion = true
                    if(element === p) {
                        element = null
                    }
                    return@p true
                }
                withCheckRecursion(key) {
                    //尝试检查内联脚本使用所在的规则上下文是否匹配（如果可以得到，它们需要全部匹配，否则标为存在冲突并结束检查）
                    if(hasConflict) return@p false
                    ProgressManager.checkCanceled()
                    val eConfigs = ParadoxConfigHandler.getConfigs(p)
                    if(eConfigs.isNotEmpty()) {
                        val configsToAdd = eConfigs.mapNotNull { it.parent }
                        if(configs.isEmpty()) {
                            configs.addAll(configsToAdd)
                            true
                        } else {
                            if(configsToAdd.any { c1 -> configs.any { c2 -> c1 pointerEquals c2 } }) {
                                true
                            } else {
                                hasConflict = true
                                false //快速判断
                            }
                        }
                    } else {
                        true
                    }
                } ?: true
            }
        }
        val usageElement = element ?: return null
        return ParadoxInlineScriptUsageInfo1(usageElement.createPointer(), hasConflict, hasRecursion)
    }
}