package icu.windea.pls.config.config

import com.google.common.cache.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.core.*
import icu.windea.pls.core.util.*
import icu.windea.pls.core.util.*
import icu.windea.pls.lang.config.*
import icu.windea.pls.model.*

/**
 * CWT声明规则上下文。
 */
class CwtDeclarationConfigContext(
    val element: PsiElement,
    val definitionName: String?,
    val definitionType: String,
    val definitionSubtypes: List<String>?,
    val gameType: ParadoxGameType,
    val configGroup: CwtConfigGroup,
) : UserDataHolderBase() {
    /**
     * 得到根据子类型列表进行合并后的CWT声明规则。
     */
    fun getConfig(declarationConfig: CwtDeclarationConfig): CwtPropertyConfig {
        val project = declarationConfig.info.configGroup.project
        val cache = project.declarationConfigCache.value
        val cacheKey = ooGetCacheKey(declarationConfig)
        return cache.getCancelable(cacheKey) {
            val config = doGetConfig(declarationConfig)
            config.declarationConfigCacheKey = cacheKey
            config
        }
    }
    
    private fun ooGetCacheKey(declarationConfig: CwtDeclarationConfig): String {
        return provider!!.getCacheKey(this, declarationConfig)
    }
    
    private fun doGetConfig(declarationConfig: CwtDeclarationConfig): CwtPropertyConfig {
        return provider!!.getConfig(this, declarationConfig)
    }
    
    object Keys: KeyRegistry("CwtDeclarationConfigContext")
}

//project -> cacheKey -> declarationConfig
//use soft values to optimize memory
private val PlsKeys.declarationConfigCache by createCachedValueKey("cwt.declarationConfig.cache") {
    CacheBuilder.newBuilder().softValues().buildCache<String, CwtPropertyConfig>()
        .withDependencyItems()
}
private val Project.declarationConfigCache by PlsKeys.declarationConfigCache

var CwtDeclarationConfigContext.provider: CwtDeclarationConfigContextProvider? by createKeyDelegate(CwtDeclarationConfigContext.Keys)

var CwtMemberConfig<*>.declarationConfigCacheKey: String? by createKeyDelegate(CwtMemberConfig.Keys)