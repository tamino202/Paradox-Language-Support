package icu.windea.pls.core.expression.nodes

import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.expression.errors.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.script.highlighter.*
import icu.windea.pls.script.psi.*

	class ParadoxScriptValueExpressionNode(
	override val text: String,
	override val rangeInExpression: TextRange,
	val config: CwtConfig<*>,
	val configGroup: CwtConfigGroup
) : ParadoxExpressionNode {
	override fun getAttributesKey(): TextAttributesKey? {
		if(text.isEmpty()) return null
		if(text.isParameterAwareExpression()) return null
		return ParadoxScriptAttributesKeys.DEFINITION_REFERENCE_KEY
	}
	
	override fun getUnresolvedError(element: ParadoxScriptStringExpressionElement): ParadoxExpressionError? {
		if(nodes.isNotEmpty()) return null
		if(text.isEmpty()) return null
		if(text.isParameterAwareExpression()) return null
		if(getReference(element).canResolve()) return null
		return ParadoxUnresolvedScriptValueExpressionError(rangeInExpression, PlsBundle.message("script.expression.unresolvedScriptValue", text))
	}
	
	override fun getReference(element: ParadoxScriptStringExpressionElement): Reference? {
		if(text.isEmpty()) return null
		if(text.isParameterAwareExpression()) return null
		return Reference(element, rangeInExpression, text, config, configGroup)
	}
	
	companion object Resolver {
		fun resolve(text: String, textRange: TextRange, config: CwtConfig<*>, configGroup: CwtConfigGroup): ParadoxScriptValueExpressionNode {
			return ParadoxScriptValueExpressionNode(text, textRange, config, configGroup)
		}
	}
	
	class Reference(
		element: ParadoxScriptStringExpressionElement,
		rangeInElement: TextRange,
		val name: String,
		val config: CwtConfig<*>,
		val configGroup: CwtConfigGroup
	) : PsiPolyVariantReferenceBase<ParadoxScriptStringExpressionElement>(element, rangeInElement), SmartPsiReference {
		override fun handleElementRename(newElementName: String): PsiElement {
			return element.setValue(rangeInElement.replace(element.value, newElementName))
		}
		
		override fun resolve(): PsiElement? {
			return resolve(true)
		}
		
		override fun resolve(exact: Boolean): PsiElement? {
			val configExpression = CwtValueExpression.resolve("<script_value>")
			return CwtConfigHandler.resolveScriptExpression(element, rangeInElement, config, config.expression, configGroup, exact = exact)
		}
		
		override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
			val configExpression = CwtValueExpression.resolve("<script_value>")
			return CwtConfigHandler.multiResolveScriptExpression(element, rangeInElement, config, config.expression, configGroup)
				.mapToArray { PsiElementResolveResult(it) }
		}
	}
}
