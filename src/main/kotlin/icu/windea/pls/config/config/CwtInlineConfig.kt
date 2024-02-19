package icu.windea.pls.config.config

import com.intellij.psi.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.lang.*

class CwtInlineConfig private constructor(
    override val pointer: SmartPsiElementPointer<out CwtProperty>,
    override val info: CwtConfigGroupInfo,
    override val config: CwtPropertyConfig,
    override val name: String
) : CwtInlineableConfig<CwtProperty> {
    fun inline(): CwtPropertyConfig {
        val other = config
        val inlined = other.copy(
            key = name,
            configs = CwtConfigManipulator.deepCopyConfigs(other)
        )
        inlined.configs?.forEachFast { it.parentConfig = inlined }
        inlined.inlineableConfig = this
        return inlined
    }
    
    companion object Resolver {
        fun resolve(config: CwtPropertyConfig, name: String): CwtInlineConfig {
            return CwtInlineConfig(config.pointer, config.info, config, name)
        }
    }
}