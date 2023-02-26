package icu.windea.pls.lang.model

import com.intellij.openapi.progress.*
import com.intellij.util.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.script.psi.*
import java.util.concurrent.*

/**
 * @property elementPath 相对于所属定义的定义成员路径。
 */
class ParadoxDefinitionMemberInfo(
    val elementPath: ParadoxElementPath,
    val gameType: ParadoxGameType,
    val definitionInfo: ParadoxDefinitionInfo,
    val configGroup: CwtConfigGroup,
    val element: ParadoxScriptMemberElement //直接作为属性的话可能会有些问题，不过这个缓存会在所在脚本文件变更时被清除，应当问题不大
) {
    val isDefinition = element is ParadoxScriptDefinitionElement && elementPath.isEmpty()
    val isParameterAware = elementPath.isParameterAware
    
    private val cache: MutableMap<String, List<CwtDataConfig<*>>> = ConcurrentHashMap()
    
    /**
     * 对应的配置列表。
     */
    fun getConfigs(matchType: Int = CwtConfigMatchType.DEFAULT): List<CwtDataConfig<*>> {
        val key = "$matchType"
        return cache.getOrPut(key) { doGetConfigs(definitionInfo, this, matchType) }
    }
    
    /**
     * 对应的子配置列表。（得到的是合并后的规则列表，且过滤重复的）
     */
    fun getChildConfigs(matchType: Int = CwtConfigMatchType.DEFAULT): List<CwtDataConfig<*>> {
        val key = "child#$matchType"
        return cache.getOrPut(key) { doGetChildConfigs(definitionInfo, this, matchType) }
    }
}

/**
 * 根据路径解析对应的属性/值配置列表。
 */
private fun doGetConfigs(definitionInfo: ParadoxDefinitionInfo, definitionMemberInfo: ParadoxDefinitionMemberInfo, matchType: Int): List<CwtDataConfig<*>> {
    val element = definitionMemberInfo.element
    //基于keyExpression，valueExpression可能不同
    val declaration = definitionInfo.declaration ?: return emptyList()
    //如果路径中可能待遇参数，则不进行解析
    val elementPath = definitionMemberInfo.elementPath
    if(elementPath.isParameterAware) return emptyList()
    val configGroup = definitionMemberInfo.configGroup
    
    var result: List<CwtDataConfig<*>> = declaration.toSingletonList()
    if(elementPath.isEmpty()) return result
    
    var isInlined = false
    
    for((key, isQuoted, isKey) in elementPath) {
        //如果整个过程中得到的某个propertyConfig的valueExpressionType是single_alias_right或alias_matches_left，则需要内联子规则
        //如果整个过程中的某个key匹配内联规则的名字（如，inline_script），则内联此内联规则
        
        val expression = ParadoxDataExpression.resolve(key, isQuoted, true)
        val nextResult = SmartList<CwtDataConfig<*>>()
        for(parentConfig in result) {
            //处理内联规则
            if(!isInlined && isKey && parentConfig is CwtPropertyConfig) {
                isInlined = CwtConfigHandler.inlineConfigAsChild(key, isQuoted, parentConfig, configGroup, nextResult)
                if(isInlined) continue
            }
            
            val configs = parentConfig.configs
            if(configs.isNullOrEmpty()) continue
            for(config in configs) {
                if(isKey && config is CwtPropertyConfig) {
                    if(CwtConfigHandler.matchesScriptExpression(element, expression, config.keyExpression, config, configGroup, matchType)) {
                        CwtConfigHandler.inlineConfig(element, key, isQuoted, config, configGroup, nextResult, matchType)
                    }
                } else if(!isKey && config is CwtValueConfig) {
                    nextResult.add(config)
                }
            }
        }
        
        //如果存在可以静态匹配（CwtConfigMatchType.STATIC）的规则，则仅选用可以静态匹配的规则
        result = nextResult
            .filter { CwtConfigHandler.matchesScriptExpression(element, expression, it.expression, it, configGroup, CwtConfigMatchType.STATIC) }
            .ifEmpty { nextResult }
    }
    
    return result.sortedByPriority(configGroup) { it.expression }
}

/**
 * 根据路径解析对应的子属性规则列表。（得到的是合并后的规则列表，且过滤重复的）
 */
private fun doGetChildConfigs(definitionInfo: ParadoxDefinitionInfo, definitionMemberInfo: ParadoxDefinitionMemberInfo, matchType: Int): List<CwtDataConfig<*>> {
    //基于上一级keyExpression，keyExpression一定唯一
    if(definitionInfo.declaration?.configs.isNullOrEmpty()) return emptyList()
    //如果路径中可能待遇参数，则不进行解析
    val elementPath = definitionMemberInfo.elementPath
    if(elementPath.isParameterAware) return emptyList()
    //parentPath可以对应property或者value
    return when {
        //这里的属性路径可以为空，这时得到的就是顶级属性列表（定义的代码块类型的值中的属性列表）
        elementPath.isEmpty() -> definitionInfo.declaration?.configs.orEmpty()
        else -> {
            //打平propertyConfigs中的每一个properties
            val configs = doGetConfigs(definitionInfo, definitionMemberInfo, matchType)
            val result = SmartList<CwtDataConfig<*>>()
            for(config in configs) {
                val childConfigs = config.configs
                if(childConfigs != null && childConfigs.isNotEmpty()) result.addAll(childConfigs)
            }
            result
        }
    }
}
