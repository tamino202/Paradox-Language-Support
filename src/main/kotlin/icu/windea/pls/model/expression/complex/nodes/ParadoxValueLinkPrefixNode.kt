package icu.windea.pls.model.expression.complex.nodes

import com.intellij.lang.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.config.config.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.script.highlighter.*
import icu.windea.pls.script.psi.*

class ParadoxValueLinkPrefixNode(
    override val text: String,
    override val rangeInExpression: TextRange,
    override val linkConfigs: List<CwtLinkConfig>
) : ParadoxComplexExpressionNode.Base(), ParadoxLinkPrefixNode {
    override fun getAttributesKey(language: Language): TextAttributesKey {
        return ParadoxScriptAttributesKeys.VALUE_LINK_PREFIX_KEY
    }
    
    override fun getReference(element: ParadoxScriptStringExpressionElement): Reference {
        val rangeInElement = rangeInExpression.shiftRight(ParadoxExpressionHandler.getExpressionOffset(element))
        return Reference(element, rangeInElement, linkConfigs)
    }
    
    class Reference(
        element: ParadoxScriptStringExpressionElement,
        rangeInElement: TextRange,
        val linkConfigs: List<CwtLinkConfig>
    ) : PsiPolyVariantReferenceBase<ParadoxScriptStringExpressionElement>(element, rangeInElement) {
        override fun handleElementRename(newElementName: String): PsiElement {
            throw IncorrectOperationException() //cannot rename cwt config
        }
        
        override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
            return linkConfigs.mapNotNull { it.pointer.element }.mapToArray { PsiElementResolveResult(it) }
        }
    }
    
    companion object Resolver {
        fun resolve(text: String, textRange: TextRange, linkConfigs: List<CwtLinkConfig>): ParadoxValueLinkPrefixNode {
            return ParadoxValueLinkPrefixNode(text, textRange, linkConfigs)
        }
    }
}
