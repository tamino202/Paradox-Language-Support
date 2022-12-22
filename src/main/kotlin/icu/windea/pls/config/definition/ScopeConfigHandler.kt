package icu.windea.pls.config.definition

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.config.definition.config.*
import icu.windea.pls.core.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.core.expression.nodes.*
import icu.windea.pls.core.handler.*
import icu.windea.pls.script.psi.*

/**
 * 用于处理作用域。
 */
object ScopeConfigHandler {
	const val unknownScopeId = "?"
	const val anyScopeId = "any"
	const val allScopeId = "all"
	
	/**
	 * 得到作用域的ID（用于显示在内嵌提示中，全小写+下划线）。
	 */
	fun getScopeId(scope: String) : String {
		return scope.lowercase().replace(' ', '_')
	}
	
	/**
	 * 得到作用域的名字（用于显示在快速文档中）。
	 */
	@JvmStatic
	fun getScopeName(scope: String, configGroup: CwtConfigGroup): String {
		//handle "any" and "all" scope 
		if(scope.equals(anyScopeId, true)) return "Any"
		if(scope.equals(allScopeId, true)) return "All"
		//a scope may not have aliases, or not defined in scopes.cwt
		return configGroup.scopes[scope]?.name
			?: configGroup.scopeAliasMap[scope]?.name
			?: scope.toCapitalizedWords()
	}
	
	fun matchesScope(scopeId: String?, scopesToMatch: Collection<String>?, configGroup: CwtConfigGroup): Boolean {
		if(scopeId == null) return true
		if(scopeId.equals(anyScopeId, true) || scopeId.equals(allScopeId, true)) return true
		if(scopeId.equals(unknownScopeId, true)) return true //ignore
		if(scopesToMatch.isNullOrEmpty()) return true
		return scopesToMatch.any { s ->
			if(s.equals(anyScopeId, true) || s.equals(allScopeId, true)) return@any true
			scopeId.equals(s, true) || configGroup.scopeAliasMap[scopeId]?.aliases?.contains(s) == true
		}
	}
	
	//fun matchScope(scopes: Collection<String>?, scopesToMatch: Collection<String>?, configGroup: CwtConfigGroup): Boolean {
	//	if(scopes.isNullOrEmpty()) return true
	//	return scopes.any { scope -> matchScope(scope, scopesToMatch, configGroup) }
	//}
	
	fun findParentMember(element: ParadoxScriptMemberElement): ParadoxScriptMemberElement? {
		return element.parents(withSelf = false)
			.find { it is ParadoxScriptDefinitionElement || (it is ParadoxScriptBlock && it.isBlockValue()) }
			.castOrNull<ParadoxScriptMemberElement>()
	}
	
	fun isScopeContextChanged(element: ParadoxScriptMemberElement, scopeContext: ParadoxScopeConfig, file: PsiFile = element.containingFile) :Boolean {
		//does not have scope context > changed always
		val parentMember = findParentMember(element)
		if(parentMember == null) return true
		val parentScopeContext = getScopeContext(parentMember, file)
		if(parentScopeContext == null) return true
		if(parentScopeContext !== scopeContext) return true
		if(!hasScopeContext(parentMember, parentScopeContext)) return true
		return false
	}
	
	fun hasScopeContext(element: ParadoxScriptMemberElement, scopeContext: ParadoxScopeConfig): Boolean {
		val isDefinition = element is ParadoxScriptDefinitionElement && element.definitionInfo != null
		return if(isDefinition) scopeContext.fromTypeConfig else true
	}
	
	fun getScopeContext(element: ParadoxScriptMemberElement, file: PsiFile = element.containingFile) : ParadoxScopeConfig? {
		return CachedValuesManager.getCachedValue(element, PlsKeys.cachedScopeContextKey) {
			val value = resolveScopeContext(element)
			CachedValueProvider.Result.create(value, file)
		}
	}
	
	private fun resolveScopeContext(element: ParadoxScriptMemberElement): ParadoxScopeConfig? {
		//should be a definition
		val definitionInfo = element.castOrNull<ParadoxScriptDefinitionElement>()?.definitionInfo
		if(definitionInfo != null) {
			val declarationConfig = definitionInfo.declarationConfig?.propertyConfig ?: return null
			val subtypeConfigs = definitionInfo.subtypeConfigs
			val typeConfig = definitionInfo.typeConfig
			val replaceScopeOnType = subtypeConfigs.firstNotNullOfOrNull { it.config.replaceScope }
				?: typeConfig.config.replaceScope
			val replaceScope = replaceScopeOnType
				?: declarationConfig.replaceScope
			val pushScopeOnType = (subtypeConfigs.firstNotNullOfOrNull { it.config.pushScope }
				?: typeConfig.config.pushScope)
			val pushScope = pushScopeOnType
				?: declarationConfig.pushScope
			return replaceScope?.resolve(pushScope)?.apply {
				this.fromTypeConfig = replaceScopeOnType != null || pushScopeOnType != null
			}
		}
		
		//should be a definition member
		val parentMember = findParentMember(element) ?: return null
		val parentScopeContext = getScopeContext(parentMember)
		val configs = ParadoxCwtConfigHandler.resolveConfigs(element, allowDefinitionSelf = true)
		val config = configs.firstOrNull()
		if(config == null) return null
		if(config is CwtPropertyConfig && config.expression.type == CwtDataTypes.ScopeField) {
			if(parentScopeContext == null) return null
			val scopeField = element.castOrNull<ParadoxScriptProperty>()?.propertyKey?.text ?: return null
			return resolveScopeContextFromScopeField(scopeField, config, parentScopeContext)
				?: resolveUnknownScopeContext(parentScopeContext)
		}
		val replaceScope = config.replaceScope ?: parentScopeContext
		val pushScope = config.pushScope
		return replaceScope?.resolve(pushScope)
	}
	
	private fun resolveUnknownScopeContext(parentScopeContext: ParadoxScopeConfig): ParadoxScopeConfig {
		return parentScopeContext.resolve(unknownScopeId)
	}
	
	private fun resolveScopeContextFromScopeField(text: String, config: CwtPropertyConfig, parentScopeContext: ParadoxScopeConfig): ParadoxScopeConfig? {
		if(text.isLeftQuoted()) return null
		val textRange = TextRange.create(0, text.length)
		val scopeFieldExpression = ParadoxScopeFieldExpression.resolve(text, textRange, config.info.configGroup, true) ?: return null
		return resolveScopeContextFromScopeFieldExpression(scopeFieldExpression, parentScopeContext)
	}
	
	private fun resolveScopeContextFromScopeFieldExpression(scopeFieldExpression: ParadoxScopeFieldExpression, parentScopeContext: ParadoxScopeConfig): ParadoxScopeConfig? {
		val scopeNodes = scopeFieldExpression.scopeNodes
		var resolvedScope: String? = null
		var scopeContext = parentScopeContext
		for(i in scopeNodes.lastIndex .. 0) {
			val scopeNode = scopeNodes[i]
			when(scopeNode) {
				is ParadoxScopeLinkExpressionNode -> {
					resolvedScope = resolveScopeFromScopeLink(scopeNode)
					break
				}
				is ParadoxScopeLinkFromDataExpressionNode -> {
					resolvedScope = resolveScopeFromScopeLinkFromData(scopeNode)
					break
				}
				is ParadoxSystemScopeExpressionNode -> {
					scopeContext = resolveScopeFromSystemScope(scopeNode, scopeContext) ?: return null
					resolvedScope = scopeContext.thisScope
				}
				is ParadoxErrorScopeExpressionNode -> {
					return null //error
				}
			}
		}
		if(resolvedScope == null) return null
		return parentScopeContext.resolve(resolvedScope)
	}
	
	private fun resolveScopeFromScopeLink(node: ParadoxScopeLinkExpressionNode): String {
		return node.config.outputScope ?: anyScopeId
	}
	
	private fun resolveScopeFromScopeLinkFromData(node: ParadoxScopeLinkFromDataExpressionNode) : String? {
		return null //TODO
	}
	
	private fun resolveScopeFromSystemScope(node: ParadoxSystemScopeExpressionNode, scopeContext: ParadoxScopeConfig) : ParadoxScopeConfig? {
		val systemScopeConfig = node.config
		val id = systemScopeConfig.id
		return when {
			id == "This" -> scopeContext
			id == "Root" -> scopeContext.root
			id == "Prev" -> scopeContext.prev
			id == "PrevPrev" -> scopeContext.prev?.prev
			id == "PrevPrevPrev" -> scopeContext.prev?.prev?.prev
			id == "PrevPrevPrevPrev" -> scopeContext.prev?.prev?.prev?.prev
			id == "From" -> scopeContext.from
			else -> null //TODO
		}
	}
}