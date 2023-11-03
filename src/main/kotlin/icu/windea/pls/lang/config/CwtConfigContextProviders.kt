package icu.windea.pls.lang.config

import com.intellij.lang.injection.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.pls.config.config.*
import icu.windea.pls.core.*
import icu.windea.pls.core.util.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.search.selector.*
import icu.windea.pls.core.util.*
import icu.windea.pls.lang.*
import icu.windea.pls.model.*
import icu.windea.pls.script.injection.*
import icu.windea.pls.script.psi.*

//region Extensions

var CwtConfigContext.inlineScriptRootConfigContext: CwtConfigContext? by createKeyDelegate(CwtConfigContext.Keys)
var CwtConfigContext.inlineScriptExpression: String? by createKeyDelegate(CwtConfigContext.Keys)
var CwtConfigContext.inlineScriptHasConflict: Boolean? by createKeyDelegate(CwtConfigContext.Keys)
var CwtConfigContext.inlineScriptHasRecursion: Boolean? by createKeyDelegate(CwtConfigContext.Keys)
var CwtConfigContext.parameterValueRootConfigContext: CwtConfigContext? by createKeyDelegate(CwtConfigContext.Keys)
var CwtConfigContext.parameterElement: ParadoxParameterElement? by createKeyDelegate(CwtConfigContext.Keys)

//endregion

/**
 * 用于获取直接的CWT规则上下文。
 */
class CwtBaseConfigContextProvider : CwtConfigContextProvider {
    override fun getContext(element: ParadoxScriptMemberElement, elementPath: ParadoxElementPath, file: PsiFile): CwtConfigContext? {
        val vFile = selectFile(file) ?: return null
        if(ParadoxFileManager.isInjectedFile(vFile)) return null //ignored for injected psi
        
        ProgressManager.checkCanceled()
        val fileInfo = vFile.fileInfo ?: return null
        val gameType = fileInfo.rootInfo.gameType
        val definition = element.findParentDefinition()
        if(definition == null) {
            val configGroup = getConfigGroups(file.project).get(gameType)
            val configContext = CwtConfigContext(element, fileInfo, elementPath, gameType, configGroup)
            return configContext
        } else {
            val definitionInfo = definition.definitionInfo ?: return null
            val definitionElementPath = definitionInfo.elementPath
            val elementPathFromRoot = definitionElementPath.relativeTo(elementPath) ?: return null
            val configGroup = getConfigGroups(file.project).get(gameType)
            val configContext = CwtConfigContext(element, fileInfo, elementPath, gameType, configGroup)
            configContext.definitionInfo = definitionInfo
            configContext.elementPathFromRoot = elementPathFromRoot
            return configContext
        }
    }
    
    override fun getCacheKey(context: CwtConfigContext, matchOptions: Int): String? {
        val gameTypeId = context.gameType.id
        val definitionInfo = context.definitionInfo ?: return null
        val declarationConfig = definitionInfo.getDeclaration(matchOptions) ?: return null
        val declarationConfigContextCacheKey = declarationConfig.declarationConfigCacheKey ?: return null // null -> unexpected
        val elementPathFromRoot = context.elementPathFromRoot ?: return null // null -> unexpected
        val isPropertyValue = context.element is ParadoxScriptValue && context.element.isPropertyValue()
        return "b@$gameTypeId:${matchOptions}#${isPropertyValue.toInt()}#${declarationConfigContextCacheKey.substringAfterLast('#')}\n${elementPathFromRoot}"
    }
    
    override fun getConfigs(context: CwtConfigContext, matchOptions: Int): List<CwtMemberConfig<*>>? {
        ProgressManager.checkCanceled()
        val elementPathFromRoot = context.elementPathFromRoot ?: return null
        val definitionInfo = context.definitionInfo ?: return null
        val declarationConfig = definitionInfo.getDeclaration(matchOptions) ?: return null
        val rootConfigs = declarationConfig.toSingletonList()
        val configGroup = context.configGroup
        val element = context.element
        return CwtConfigHandler.getConfigsForConfigContext(element, rootConfigs, elementPathFromRoot, configGroup, matchOptions)
    }
}

/**
 * 用于获取内联脚本调用中的CWT规则上下文。
 *
 * * 正常提供代码高亮、引用解析、代码补全等高级语言功能。
 */
class CwtInlineScriptUsageConfigContextProvider : CwtConfigContextProvider {
    //注意：内联脚本调用可以在定义声明之外
    
    override fun getContext(element: ParadoxScriptMemberElement, elementPath: ParadoxElementPath, file: PsiFile): CwtConfigContext? {
        val vFile = selectFile(file) ?: return null
        
        //要求当前位置相对于文件的元素路径中包含子路径"inline_script"
        val rootIndex = elementPath.indexOfFirst { it.subPath.equals(ParadoxInlineScriptHandler.inlineScriptKey, true) }
        if(rootIndex == -1) return null
        
        val fileInfo = vFile.fileInfo ?: return null
        val gameType = fileInfo.rootInfo.gameType
        val elementPathFromRoot = ParadoxElementPath.resolve(elementPath.rawSubPaths.let { it.subList(rootIndex + 1, it.size) })
        val configGroup = getConfigGroups(file.project).get(gameType)
        val configContext = CwtConfigContext(element, fileInfo, elementPath, gameType, configGroup)
        configContext.elementPathFromRoot = elementPathFromRoot
        return configContext
    }
    
    override fun getCacheKey(context: CwtConfigContext, matchOptions: Int): String? {
        val gameTypeId = context.gameType.id
        val path = context.fileInfo?.path ?: return null // null -> unexpected
        val elementPathFromRoot = context.elementPathFromRoot ?: return null // null -> unexpected
        val isPropertyValue = context.element is ParadoxScriptValue && context.element.isPropertyValue()
        return "isu@$gameTypeId:${matchOptions}#${isPropertyValue.toInt()}#${path.path}\n${elementPathFromRoot.path}"
    }
    
    override fun getConfigs(context: CwtConfigContext, matchOptions: Int): List<CwtMemberConfig<*>>? {
        val elementPathFromRoot = context.elementPathFromRoot ?: return null
        val configGroup = context.configGroup
        val inlineConfigs = configGroup.inlineConfigGroup[ParadoxInlineScriptHandler.inlineScriptKey] ?: return null
        val element = context.element
        val rootConfigs = inlineConfigs.map { it.inline() }
        return CwtConfigHandler.getConfigsForConfigContext(element, rootConfigs, elementPathFromRoot, configGroup, matchOptions)
    }
}

/**
 * 用于获取内联脚本中的CWT规则上下文。
 *
 * * 正常提供代码高亮、引用解析、代码补全等高级语言功能。
 * * 对于顶级成员，禁用以下代码检查：`MissingExpressionInspection`和`TooManyExpressionInspection`。
 * * 会将内联脚本内容内联到对应的调用处，然后再进行相关代码检查。
 */
class CwtInlineScriptConfigContextProvider : CwtConfigContextProvider {
    //注意：内联脚本调用可以在定义声明之外
    
    //TODO 1.1.0+ 支持解析内联脚本文件中的定义声明
    
    //首先推断内联脚本文件的CWT规则上下文：汇总内联脚本调用处的上下文，然后合并得到最终的CWT规则上下文
    //然后再得到当前位置的CWT规则上下文
    
    override fun getContext(element: ParadoxScriptMemberElement, elementPath: ParadoxElementPath, file: PsiFile): CwtConfigContext? {
        if(!getSettings().inference.inlineScriptConfig) return null
        
        val vFile = selectFile(file) ?: return null
        if(ParadoxFileManager.isInjectedFile(vFile)) return null //ignored for injected psi
        
        val inlineScriptExpression = ParadoxInlineScriptHandler.getInlineScriptExpression(vFile)
        if(inlineScriptExpression == null) return null
        
        ProgressManager.checkCanceled()
        val fileInfo = vFile.fileInfo ?: return null
        val gameType = fileInfo.rootInfo.gameType
        val elementPathFromRoot = elementPath
        val configGroup = getConfigGroups(file.project).get(gameType)
        val configContext = CwtConfigContext(element, fileInfo, elementPath, gameType, configGroup)
        if(elementPathFromRoot.isNotEmpty()) {
            configContext.inlineScriptRootConfigContext = CwtConfigHandler.getConfigContext(file) ?: return null
        }
        configContext.inlineScriptExpression = inlineScriptExpression
        configContext.elementPathFromRoot = elementPathFromRoot
        return configContext
    }
    
    override fun getCacheKey(context: CwtConfigContext, matchOptions: Int): String? {
        val gameTypeId = context.gameType.id
        val inlineScriptExpression = context.inlineScriptExpression ?: return null // null -> unexpected
        val elementPathFromRoot = context.elementPathFromRoot ?: return null // null -> unexpected
        val isPropertyValue = context.element is ParadoxScriptValue && context.element.isPropertyValue()
        return "is@$gameTypeId:${matchOptions}#${isPropertyValue.toInt()}#${inlineScriptExpression}\n${elementPathFromRoot.path}"
    }
    
    //获取CWT规则后才能确定是否存在冲突以及是否存在递归
    
    override fun getConfigs(context: CwtConfigContext, matchOptions: Int): List<CwtMemberConfig<*>>? {
        ProgressManager.checkCanceled()
        val elementPathFromRoot = context.elementPathFromRoot ?: return null
        
        if(elementPathFromRoot.isNotEmpty()) {
            val rootConfigContext = context.inlineScriptRootConfigContext ?: return null
            val element = context.element
            val rootConfigs = rootConfigContext.getConfigs(matchOptions)
            val configGroup = context.configGroup
            return CwtConfigHandler.getConfigsForConfigContext(element, rootConfigs, elementPathFromRoot, configGroup, matchOptions)
        }
        
        val inlineScriptExpression = context.inlineScriptExpression ?: return null
        
        // infer & merge
        val result = Ref.create<List<CwtMemberConfig<*>>>()
        context.inlineScriptHasConflict = false
        context.inlineScriptHasRecursion = false
        withRecursionGuard("icu.windea.pls.lang.config.CwtInlineScriptConfigContextProvider.getConfigsForConfigContext") {
            withCheckRecursion(inlineScriptExpression) {
                val project = context.configGroup.project
                val selector = inlineScriptSelector(project, context.element)
                ParadoxInlineScriptUsageSearch.search(inlineScriptExpression, selector).processQueryAsync p@{ info ->
                    ProgressManager.checkCanceled()
                    val file = info.virtualFile?.toPsiFile(project) ?: return@p true
                    val e = file.findElementAt(info.elementOffset) ?: return@p true
                    val p = e.parentOfType<ParadoxScriptProperty>() ?: return@p true
                    if(!p.name.equals(ParadoxInlineScriptHandler.inlineScriptKey, true)) return@p true
                    val memberElement = p.parentOfType<ParadoxScriptMemberElement>() ?: return@p true
                    val usageConfigContext = CwtConfigHandler.getConfigContext(memberElement) ?: return@p true
                    val usageConfigs = usageConfigContext.getConfigs(matchOptions).orNull()
                    // merge
                    result.mergeValue(usageConfigs) { v1, v2 -> CwtConfigManipulator.mergeConfigs(v1, v2) }.also {
                        if(it) return@also
                        context.inlineScriptHasConflict = true
                        result.set(null)
                    }
                }
            } ?: run {
                context.inlineScriptHasRecursion = true
                result.set(null)
            }
        }
        return result.get()
    }
    
    //skip MissingExpressionInspection and TooManyExpressionInspection at root level
    
    override fun skipMissingExpressionCheck(context: CwtConfigContext): Boolean {
        val elementPathFromRoot = context.elementPathFromRoot ?: return false
        return elementPathFromRoot.isEmpty()
    }
    
    override fun skipTooManyExpressionCheck(context: CwtConfigContext): Boolean {
        val elementPathFromRoot = context.elementPathFromRoot ?: return false
        return elementPathFromRoot.isEmpty()
    }
}

/**
 * 用于获取脚本参数的传入值和默认值中的CWT规则上下文。
 *
 * * 基于语言注入功能实现。
 * * 正常提供代码高亮、引用解析、代码补全等高级语言功能。
 * * 对于顶级成员，禁用以下代码检查：`MissingExpressionInspection`和`TooManyExpressionInspection`。
 * * 不会将参数值内容内联到对应的调用处，然后再进行相关代码检查。
 * * 不会将参数值内容内联到对应的调用处，然后检查语法是否合法。
 *
 * @see ParadoxScriptLanguageInjector
 */
class CwtParameterValueConfigContextProvider : CwtConfigContextProvider {
    override fun getContext(element: ParadoxScriptMemberElement, elementPath: ParadoxElementPath, file: PsiFile): CwtConfigContext? {
        if(!getSettings().inference.parameterConfig) return null
        
        //unnecessary check
        //val vFile = selectFile(file) ?: return null
        //if(ParadoxFileManager.isInjectedFile(vFile)) return null //ignored for injected psi
        
        val host = InjectedLanguageManager.getInstance(file.project).getInjectionHost(file)
        if(host == null) return null
        val parameterElement = getParameterElement(file, host)
        if(parameterElement == null) return null
        
        ProgressManager.checkCanceled()
        val gameType = parameterElement.gameType
        val elementPathFromRoot = elementPath
        val configGroup = getConfigGroups(file.project).get(gameType)
        val configContext = CwtConfigContext(element, null, elementPath, gameType, configGroup)
        if(elementPathFromRoot.isNotEmpty()) {
            configContext.parameterValueRootConfigContext = CwtConfigHandler.getConfigContext(file) ?: return null
        }
        configContext.elementPathFromRoot = elementPathFromRoot
        configContext.parameterElement = parameterElement
        return configContext
    }
    
    private fun getParameterElement(file: PsiFile, host: PsiElement): ParadoxParameterElement? {
        val injectionInfos = host.getUserData(ParadoxScriptLanguageInjector.Keys.parameterValueInjectionInfos)
        if(injectionInfos.isNullOrEmpty()) return null
        return when {
            host is ParadoxScriptStringExpressionElement -> {
                val shreds = file.getShreds()
                val shred = shreds?.singleOrNull()
                val rangeInsideHost = shred?.rangeInsideHost ?: return null
                val injectionInfo = injectionInfos.find { it.rangeInsideHost == rangeInsideHost } ?: return null
                injectionInfo.parameterElement
            }
            host is ParadoxParameter -> {
                //just use the only one
                val injectionInfo = injectionInfos.singleOrNull() ?: return null
                injectionInfo.parameterElement
            }
            else -> null
        }
    }
    
    override fun getCacheKey(context: CwtConfigContext, matchOptions: Int): String? {
        val gameTypeId = context.gameType.id
        val parameterElement = context.parameterElement ?: return null // null -> unexpected
        val elementPathFromRoot = context.elementPathFromRoot ?: return null // null -> unexpected
        val isPropertyValue = context.element is ParadoxScriptValue && context.element.isPropertyValue()
        return "is@$gameTypeId:${matchOptions}#${isPropertyValue.toInt()}#${parameterElement.contextKey}@${parameterElement.name}\n${elementPathFromRoot.path}"
    }
    
    override fun getConfigs(context: CwtConfigContext, matchOptions: Int): List<CwtMemberConfig<*>>? {
        ProgressManager.checkCanceled()
        val elementPathFromRoot = context.elementPathFromRoot ?: return null
        
        if(elementPathFromRoot.isNotEmpty()) {
            val rootConfigContext = context.parameterValueRootConfigContext ?: return null
            val element = context.element
            val rootConfigs = rootConfigContext.getConfigs(matchOptions)
            val configGroup = context.configGroup
            return CwtConfigHandler.getConfigsForConfigContext(element, rootConfigs, elementPathFromRoot, configGroup, matchOptions)
        }
        
        val parameterElement = context.parameterElement ?: return null
        
        return ParadoxParameterHandler.getInferredContextConfigs(parameterElement)
    }
    
    //skip MissingExpressionInspection and TooManyExpressionInspection at root level
    
    override fun skipMissingExpressionCheck(context: CwtConfigContext): Boolean {
        val elementPathFromRoot = context.elementPathFromRoot ?: return false
        return elementPathFromRoot.isEmpty()
    }
    
    override fun skipTooManyExpressionCheck(context: CwtConfigContext): Boolean {
        val elementPathFromRoot = context.elementPathFromRoot ?: return false
        return elementPathFromRoot.isEmpty()
    }
}