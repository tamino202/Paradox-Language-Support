package icu.windea.pls.model

import com.google.common.cache.*
import com.intellij.openapi.util.*
import icu.windea.pls.core.*
import icu.windea.pls.core.util.*
import icu.windea.pls.lang.ParadoxConfigMatcher.Options
import icu.windea.pls.lang.config.*
import icu.windea.pls.lang.cwt.*
import icu.windea.pls.lang.cwt.config.*
import icu.windea.pls.script.psi.*

/**
 * CWT规则上下文。
 *
 * 用于后续获取对应的所有可能的CWT规则以及匹配的CWT规则，从而提供高级语言功能。例如代码高亮、引用解析、代码补全。
 *
 * 可以获取CWT规则上下文不意味着可以获取对应的所有可能的CWT规则。
 *
 * @property fileInfo 所在文件的文件信息。
 * @property definitionInfo 所在定义的定义信息。
 *
 * @see ParadoxConfigContextProvider
 */
class ParadoxConfigContext(
    val element: ParadoxScriptMemberElement,
    val fileInfo: ParadoxFileInfo?,
    val elementPath: ParadoxElementPath?,
    val gameType: ParadoxGameType,
    val configGroup: CwtConfigGroup
) : UserDataHolderBase() {
    companion object {
        //use soft values to optimize memory
        private val configsCache: Cache<String, List<CwtMemberConfig<*>>> = CacheBuilder.newBuilder().softValues().buildCache()
    }
    
    fun getConfigs(matchOptions: Int = Options.Default): List<CwtMemberConfig<*>> {
        val cachedKey = doGetCachedKey(matchOptions) ?: return emptyList()
        return configsCache.getOrPut(cachedKey) { doGetConfigs(matchOptions) }
    }
    
    private fun doGetCachedKey(matchOptions: Int): String? {
        return provider?.getCacheKey(this, matchOptions)
    }
    
    private fun doGetConfigs(matchOptions: Int): List<CwtMemberConfig<*>> {
        return provider?.getConfigs(this, matchOptions).orEmpty()
    }
    
    fun skipMissingExpressionCheck(): Boolean {
        return provider?.skipMissingExpressionCheck(this) ?: false
    }
    
    fun skipTooManyExpressionCheck(): Boolean {
        return provider?.skipTooManyExpressionCheck(this) ?: false
    }
    
    object Keys: KeyAware
}

val ParadoxConfigContext.Keys.definitionInfo by createKey<ParadoxDefinitionInfo>("paradox.configContext.definitionInfo")
val ParadoxConfigContext.Keys.elementPathFromRoot by createKey<ParadoxElementPath>("paradox.configContext.elementPathFromRoot")
val ParadoxConfigContext.Keys.provider by createKey<ParadoxConfigContextProvider>("paradox.configContext.provider")

var ParadoxConfigContext.definitionInfo by ParadoxConfigContext.Keys.definitionInfo
var ParadoxConfigContext.elementPathFromRoot by ParadoxConfigContext.Keys.elementPathFromRoot
var ParadoxConfigContext.provider by ParadoxConfigContext.Keys.provider

fun ParadoxConfigContext.isDefinition(): Boolean {
    return definitionInfo != null && elementPathFromRoot.let { it != null && it.isEmpty() }
}

fun ParadoxConfigContext.isRootOrMember(): Boolean {
    return elementPathFromRoot != null
}
