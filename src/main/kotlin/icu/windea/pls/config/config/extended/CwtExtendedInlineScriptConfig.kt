package icu.windea.pls.config.config.extended

import icu.windea.pls.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.util.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.model.*

/**
 * @property name template_expression
 * @property contextConfigsType (option) context_configs_type: string = "single"
 */
interface CwtExtendedInlineScriptConfig : CwtExtendedConfig {
    val name: String
    val contextConfigsType: String
    
    /**
     * 得到由其声明的上下文CWT规则列表。
     */
    fun getContextConfigs(): List<CwtMemberConfig<*>>
    
    companion object Resolver {
        fun resolve(config: CwtMemberConfig<*>): CwtExtendedInlineScriptConfig = doResolve(config)
    }
}

//Implementations (interned)

private fun doResolve(config: CwtMemberConfig<*>): CwtExtendedInlineScriptConfig {
    val name = when(config) {
        is CwtPropertyConfig -> config.key
        is CwtValueConfig -> config.value
    }
    val contextConfigsType = config.findOption("context_configs_type")?.stringValue ?: "single"
    return CwtExtendedInlineScriptConfigImpl(config, name, contextConfigsType)
}

private class CwtExtendedInlineScriptConfigImpl(
    override val config: CwtMemberConfig<*>,
    override val name: String,
    override val contextConfigsType: String,
) : CwtExtendedInlineScriptConfig {
    override fun getContextConfigs(): List<CwtMemberConfig<*>> {
        if(config !is CwtPropertyConfig) return emptyList()
        val config = CwtConfigManipulator.inlineSingleAlias(config) ?: config // #76
        val r = when(contextConfigsType) {
            "multiple" -> config.configs.orEmpty()
            else -> config.valueConfig.toSingletonListOrEmpty()
        }
        if(r.isEmpty()) return emptyList()
        val containerConfig = CwtValueConfig.resolve(
            pointer = emptyPointer(),
            configGroup = r.first().configGroup,
            value = PlsConstants.blockFolder,
            valueTypeId = CwtType.Block.id,
            configs = r,
            options = config.options,
            documentation = config.documentation
        )
        return listOf(containerConfig)
    }
}
