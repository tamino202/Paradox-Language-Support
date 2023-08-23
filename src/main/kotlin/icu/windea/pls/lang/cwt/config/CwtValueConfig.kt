package icu.windea.pls.lang.cwt.config

import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.keyFMap.*
import icu.windea.pls.core.*
import icu.windea.pls.core.annotations.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.lang.cwt.expression.*
import icu.windea.pls.model.*

sealed interface CwtValueConfig : CwtMemberConfig<CwtValue>, CwtValueAware {
    val propertyConfig: CwtPropertyConfig?
    
    companion object {
        val EmptyConfig: CwtValueConfig = CwtValueConfigImpls.ImplA(emptyPointer(), CwtConfigGroupInfo(""), "")
        
        fun resolve(
            pointer: SmartPsiElementPointer<out CwtValue>,
            info: CwtConfigGroupInfo,
            value: String,
            valueTypeId: @EnumId(CwtType::class) Byte = CwtType.String.id,
            configs: List<CwtMemberConfig<*>>? = null,
            options: List<CwtOptionMemberConfig<*>>? = null,
            documentation: String? = null,
            propertyConfig: CwtPropertyConfig? = null
        ): CwtValueConfig {
            return if(propertyConfig == null) {
                if(configs.isNullOrEmpty()) {
                    CwtValueConfigImpls.ImplA(pointer, info, value, valueTypeId, options, documentation)
                } else {
                    CwtValueConfigImpls.ImplB(pointer, info, value, valueTypeId, configs, options, documentation)
                }
            } else {
                if(configs.isNullOrEmpty()) {
                    CwtValueConfigImpls.ImplC(pointer, info, value, valueTypeId, options, documentation, propertyConfig)
                } else {
                    CwtValueConfigImpls.ImplD(pointer, info, value, valueTypeId, configs, options, documentation, propertyConfig)
                }
            }
        }
    }
}

fun CwtValueConfig.copy(
    pointer: SmartPsiElementPointer<out CwtValue> = this.pointer,
    info: CwtConfigGroupInfo = this.info,
    value: String = this.value,
    valueTypeId: @EnumId(CwtType::class) Byte = this.valueTypeId,
    configs: List<CwtMemberConfig<*>>? = this.configs,
    options: List<CwtOptionMemberConfig<*>>? = this.options,
    documentation: String? = this.documentation,
    propertyConfig: CwtPropertyConfig? = this.propertyConfig,
): CwtValueConfig {
    return CwtValueConfig.resolve(pointer, info, value, valueTypeId, configs, options, documentation, propertyConfig)
}

fun CwtValueConfig.copyDelegated(
    parent: CwtMemberConfig<*>? = this.parentConfig,
    configs: List<CwtMemberConfig<*>>? = this.configs,
    propertyConfig: CwtPropertyConfig? = this.propertyConfig,
): CwtValueConfig {
    return if(configs.isNullOrEmpty()) {
        CwtValueConfigImpls.DelegateA(this, parent, propertyConfig)
    } else {
        CwtValueConfigImpls.DelegateB(this, parent, configs, propertyConfig)
    }
}

fun CwtPropertyConfig.getValueConfig(): CwtValueConfig? {
    //this function should be enough fast because there is no pointers to be created
    val resolvedPointer = resolved().pointer
    val valuePointer = when {
        resolvedPointer is CwtPropertyPointer -> resolvedPointer.valuePointer
        else -> resolvedPointer.element?.propertyValue?.createPointer()
    } ?: return null
    return CwtValueConfigImpls.FromPropertyConfig(valuePointer, this)
}

class CwtPropertyPointer(
    private val delegate: SmartPsiElementPointer<CwtProperty>
) : SmartPsiElementPointer<CwtProperty> by delegate {
    val valuePointer: SmartPsiElementPointer<CwtValue>? = delegate.element?.propertyValue?.createPointer()
}

private object CwtValueConfigImpls {
    abstract class Impl : UserDataHolderBase(), CwtValueConfig {
        override val valueExpression: CwtValueExpression get() = if(isBlock) CwtValueExpression.BlockExpression else CwtValueExpression.resolve(value)
        override val expression: CwtValueExpression get() = valueExpression
        
        override fun resolved(): CwtValueConfig = inlineableConfig?.config?.castOrNull<CwtValueConfig>() ?: this
        override fun resolvedOrNull(): CwtValueConfig? = inlineableConfig?.config?.castOrNull<CwtValueConfig>()
    }
    
    class ImplA(
        override val pointer: SmartPsiElementPointer<out CwtValue>,
        override val info: CwtConfigGroupInfo,
        override val value: String,
        override val valueTypeId: Byte = CwtType.String.id,
        override val options: List<CwtOptionMemberConfig<*>>? = null,
        override val documentation: String? = null,
    ) : Impl(), CwtValueConfig {
        @Volatile override var parentConfig: CwtMemberConfig<*>? = null
        @Volatile override var inlineableConfig: CwtInlineableConfig<CwtValue>? = null
        
        override val propertyConfig: CwtPropertyConfig? get() = null
        override val configs: List<CwtMemberConfig<*>>? get() = if(valueTypeId == CwtType.Block.id) emptyList() else null
        
        override fun toString(): String = value
    }
    
    class ImplB(
        override val pointer: SmartPsiElementPointer<out CwtValue>,
        override val info: CwtConfigGroupInfo,
        override val value: String,
        override val valueTypeId: Byte = CwtType.String.id,
        override val configs: List<CwtMemberConfig<*>>? = null,
        override val options: List<CwtOptionMemberConfig<*>>? = null,
        override val documentation: String? = null,
    ) : Impl(), CwtValueConfig {
        @Volatile override var parentConfig: CwtMemberConfig<*>? = null
        @Volatile override var inlineableConfig: CwtInlineableConfig<CwtValue>? = null
        
        override val propertyConfig: CwtPropertyConfig? get() = null
        
        override fun toString(): String = value
    }
    
    class ImplC(
        override val pointer: SmartPsiElementPointer<out CwtValue>,
        override val info: CwtConfigGroupInfo,
        override val value: String,
        override val valueTypeId: Byte = CwtType.String.id,
        override val options: List<CwtOptionMemberConfig<*>>? = null,
        override val documentation: String? = null,
        override val propertyConfig: CwtPropertyConfig? = null,
    ) : Impl(), CwtValueConfig {
        @Volatile override var parentConfig: CwtMemberConfig<*>? = null
        @Volatile override var inlineableConfig: CwtInlineableConfig<CwtValue>? = null
        
        override val configs: List<CwtMemberConfig<*>>? get() = if(valueTypeId == CwtType.Block.id) emptyList() else null
        
        override fun toString(): String = value
    }
    
    class ImplD(
        override val pointer: SmartPsiElementPointer<out CwtValue>,
        override val info: CwtConfigGroupInfo,
        override val value: String,
        override val valueTypeId: Byte = CwtType.String.id,
        override val configs: List<CwtMemberConfig<*>>? = null,
        override val options: List<CwtOptionMemberConfig<*>>? = null,
        override val documentation: String? = null,
        override val propertyConfig: CwtPropertyConfig? = null,
    ) : Impl(), CwtValueConfig {
        @Volatile override var parentConfig: CwtMemberConfig<*>? = null
        @Volatile override var inlineableConfig: CwtInlineableConfig<CwtValue>? = null
        
        override fun toString(): String = value
    }
    
    class DelegateA(
        delegate: CwtValueConfig,
        override var parentConfig: CwtMemberConfig<*>?,
        override val propertyConfig: CwtPropertyConfig? = null,
    ) : CwtValueConfig by delegate {
        override val configs: List<CwtMemberConfig<*>>? get() = if(valueTypeId == CwtType.Block.id) emptyList() else null
        
        override fun toString(): String = value
    }
    
    class DelegateB(
        delegate: CwtValueConfig,
        override var parentConfig: CwtMemberConfig<*>?,
        override val configs: List<CwtMemberConfig<*>>? = null,
        override val propertyConfig: CwtPropertyConfig? = null,
    ) : CwtValueConfig by delegate {
        override fun toString(): String = value
    }
    
    class FromPropertyConfig(
        override val pointer: SmartPsiElementPointer<out CwtValue>,
        override val propertyConfig: CwtPropertyConfig,
    ) : Impl(), CwtValueConfig {
        override val info: CwtConfigGroupInfo get() = propertyConfig.info
        override val value: String get() = propertyConfig.value
        override val valueTypeId: Byte get() = propertyConfig.valueTypeId
        override val documentation: String? get() = propertyConfig.documentation
        override val options: List<CwtOptionMemberConfig<*>>? get() = propertyConfig.options
        override val configs: List<CwtMemberConfig<*>>? get() = propertyConfig.configs
        
        @Volatile override var parentConfig: CwtMemberConfig<*>? = null
        @Volatile override var inlineableConfig: CwtInlineableConfig<CwtValue>? = null
        
        override fun <T : Any?> getUserData(key: Key<T>): T? = propertyConfig.getUserData(key)
        override fun <T : Any?> putUserData(key: Key<T>, value: T?) = propertyConfig.putUserData(key, value)
        
        override fun toString(): String = value
    }
}
