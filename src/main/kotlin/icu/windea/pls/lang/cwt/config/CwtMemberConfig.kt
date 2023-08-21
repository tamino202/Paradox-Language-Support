package icu.windea.pls.lang.cwt.config

import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.config.*
import icu.windea.pls.lang.cwt.expression.*
import icu.windea.pls.lang.cwt.expression.CwtDataType.*
import icu.windea.pls.model.*

sealed interface CwtMemberConfig<out T : PsiElement> : UserDataHolder, CwtConfig<T>, CwtValueAware, CwtOptionsAware, CwtDocumentationAware {
    val configs: List<CwtMemberConfig<*>>?
    
    var parent: CwtMemberConfig<*>?
    var inlineableConfig: CwtInlineableConfig<@UnsafeVariance T>?
    
    val values: List<CwtValueConfig>?
    val properties: List<CwtPropertyConfig>?
    
    val valueExpression: CwtValueExpression
    override val expression: CwtDataExpression
    
    override fun resolved(): CwtMemberConfig<T> = inlineableConfig?.config?.castOrNull<CwtMemberConfig<T>>() ?: this
    
    override fun resolvedOrNull(): CwtMemberConfig<T>? = inlineableConfig?.config?.castOrNull<CwtMemberConfig<T>>()
    
    object Keys: KeyAware
}

val <T : PsiElement> CwtMemberConfig<T>.isBlock: Boolean
    get() = configs != null

val CwtMemberConfig<*>.isRoot: Boolean
    get() = when {
        this is CwtPropertyConfig -> this.parent == null
        this is CwtValueConfig -> this.parent == null && this.propertyConfig == null
        else -> false
    }

val CwtMemberConfig<*>.memberConfig: CwtMemberConfig<PsiElement>
    get() = when {
        this is CwtPropertyConfig -> this
        this is CwtValueConfig -> propertyConfig ?: this
        else -> this
    }

val CwtMemberConfig.Keys.cardinality by lazy { Key.create<CwtCardinalityExpression>("cwt.dataConfig.cardinality") }
val CwtMemberConfig.Keys.cardinalityMinDefine by lazy { Key.create<String>("cwt.dataConfig.cardinalityMinDefine") }
val CwtMemberConfig.Keys.cardinalityMaxDefine by lazy { Key.create<String>("cwt.dataConfig.cardinalityMaxDefine") }
val CwtMemberConfig.Keys.hasScopeOption by lazy { Key.create<Boolean>("cwt.dataConfig.hasScopeOption") }
val CwtMemberConfig.Keys.scopeContext by lazy { Key.create<ParadoxScopeContext>("cwt.dataConfig.scopeContext") }
val CwtMemberConfig.Keys.replaceScopes by lazy { Key.create<Map<String, String?>>("cwt.dataConfig.replaceScopes") }
val CwtMemberConfig.Keys.pushScope by lazy { Key.create<String>("cwt.dataConfig.pushScope") }
val CwtMemberConfig.Keys.supportedScopes by lazy { Key.create<Set<String>>("cwt.dataConfig.supportedScopes") }
val CwtMemberConfig.Keys.overriddenProvider by lazy { Key.create<ParadoxOverriddenConfigProvider>("cwt.DataConfig.overriddenProvider") }

//may on:
// * a config expression in declaration config
// * a config expression in subtype structure config
val CwtMemberConfig<*>.cardinality
    get() = getOrPutUserData(CwtMemberConfig.Keys.cardinality, CwtCardinalityExpression.EmptyExpression) action@{
        val option = findOption("cardinality")
        if(option == null) {
            //如果没有注明且类型是常量，则推断为 1..1
            if(expression.type == Constant) {
                return@action CwtCardinalityExpression.resolve("1..1")
            }
        }
        option?.stringValue?.let { s -> CwtCardinalityExpression.resolve(s) }
    }
val CwtMemberConfig<*>.cardinalityMinDefine
    get() = getOrPutUserData(CwtMemberConfig.Keys.cardinalityMinDefine, "") action@{
        val option = findOption("cardinality_min_define")
        option?.stringValue
    }
val CwtMemberConfig<*>.cardinalityMaxDefine
    get() = getOrPutUserData(CwtMemberConfig.Keys.cardinalityMaxDefine, "") action@{
        val option = findOption("cardinality_max_define")
        option?.stringValue
    }

val CwtMemberConfig<*>.hasScopeOption
    get() = getOrPutUserData(CwtMemberConfig.Keys.hasScopeOption) action@{
        val option = findOption { it.key == "replace_scope" || it.key == "replace_scopes" || it.key == "push_scope" || it.key == "scope" || it.key == "scopes" }
        option != null
    }
val CwtMemberConfig<*>.scopeContext
    get() = getOrPutUserData(CwtMemberConfig.Keys.scopeContext, ParadoxScopeContext.EMPTY) action@{
        val map = replaceScopes ?: return@action null
        ParadoxScopeContext.resolve(map)
    }
//may on:
// * a config expression in declaration config (include root expression, e.g. "army = { ... }")
// * a type config (e.g. "type[xxx] = { ... }")
// * a subtype config (e.g. "subtype[xxx] = { ... }")
val CwtMemberConfig<*>.replaceScopes
    get() = getOrPutUserData(CwtMemberConfig.Keys.replaceScopes, emptyMap()) action@{
        val option = findOption { it.key == "replace_scope" || it.key == "replace_scopes" }
        if(option == null) return@action null
        val options = option.findOptions() ?: return@action null
        options.associateBy({ it.key.lowercase() }, { it.stringValue?.let { v -> ParadoxScopeHandler.getScopeId(v) } })
    }
//may on:
// * a config expression in declaration config (include root expression, e.g. "army = { ... }")
// * a type config (e.g. "type[xxx] = { ... }")
// * a subtype config (e.g. "subtype[xxx] = { ... }")
val CwtMemberConfig<*>.pushScope
    get() = getOrPutUserData(CwtMemberConfig.Keys.pushScope, "") action@{
        val option = findOption { it.key == "push_scope" }
        option?.getOptionValue()?.let { v -> ParadoxScopeHandler.getScopeId(v) }
    }
//may on:
// * a config expression in declaration config
val CwtMemberConfig<*>.supportedScopes
    get() = getOrPutUserData(CwtMemberConfig.Keys.supportedScopes) action@{
        val option = findOption { it.key == "scope" || it.key == "scopes" }
        val r = option?.getOptionValueOrValues()?.mapTo(mutableSetOf()) { ParadoxScopeHandler.getScopeId(it) }
        if(r.isNullOrEmpty()) ParadoxScopeHandler.anyScopeIdSet else r
    }

fun <T : PsiElement> CwtMemberConfig<T>.toOccurrence(contextElement: PsiElement, project: Project): Occurrence {
    val cardinality = this.cardinality ?: return Occurrence(0, null, null, false)
    val cardinalityMinDefine = this.cardinalityMinDefine
    val cardinalityMaxDefine = this.cardinalityMaxDefine
    val occurrence = Occurrence(0, cardinality.min, cardinality.max, cardinality.relaxMin)
    if(cardinalityMinDefine != null) {
        val defineValue = ParadoxDefineHandler.getDefineValue(contextElement, project, cardinalityMinDefine, Int::class.java)
        if(defineValue != null) {
            occurrence.min = defineValue
            occurrence.minDefine = cardinalityMinDefine
        }
    }
    if(cardinalityMaxDefine != null) {
        val defineValue = ParadoxDefineHandler.getDefineValue(contextElement, project, cardinalityMaxDefine, Int::class.java)
        if(defineValue != null) {
            occurrence.max = defineValue
            occurrence.maxDefine = cardinalityMaxDefine
        }
    }
    return occurrence
}

var CwtMemberConfig<*>.overriddenProvider by CwtMemberConfig.Keys.overriddenProvider
