package icu.windea.pls.script.exp.nodes

import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.script.highlighter.*

class ParadoxScopeLinkExpressionNode(
	override val text: String,
	override val rangeInExpression: TextRange,
	val config: CwtLinkConfig
) : ParadoxScriptExpressionNode {
	override fun getAttributesKey() = ParadoxScriptAttributesKeys.SCOPE_KEY
	
	override fun getReference(element: PsiElement) = Reference(element, rangeInExpression, config.pointer.element)
	
	companion object Resolver {
		fun resolve(text: String, textRange: TextRange, configGroup: CwtConfigGroup): ParadoxScopeLinkExpressionNode? {
			val config = configGroup.linksAsScopeNotData.get(text)
				?: return null
			return ParadoxScopeLinkExpressionNode(text, textRange, config)
		}
	}
	
	class Reference(
		element: PsiElement,
		rangeInElement: TextRange,
		private val resolved: CwtProperty?
	) : PsiReferenceBase<PsiElement>(element, rangeInElement) {
		override fun handleElementRename(newElementName: String) = throw IncorrectOperationException()
		
		override fun resolve() = resolved
	}
}
