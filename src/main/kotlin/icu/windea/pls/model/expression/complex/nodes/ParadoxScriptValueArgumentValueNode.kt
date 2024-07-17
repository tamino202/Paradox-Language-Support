package icu.windea.pls.model.expression.complex.nodes

import com.intellij.lang.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.util.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.model.*
import icu.windea.pls.script.highlighter.*

class ParadoxScriptValueArgumentValueNode(
    override val text: String,
    override val rangeInExpression: TextRange,
    val valueNode: ParadoxScriptValueNode?,
    val argumentNode: ParadoxScriptValueArgumentNode?,
    val configGroup: CwtConfigGroup
) : ParadoxComplexExpressionNode.Base() {
    override fun getAttributesKey(language: Language): TextAttributesKey {
        //为参数值提供基础代码高亮
        val type = ParadoxType.resolve(text)
        return when {
            type.isBooleanType() -> ParadoxScriptAttributesKeys.KEYWORD_KEY
            type.isFloatType() -> ParadoxScriptAttributesKeys.NUMBER_KEY
            text.startsWith('@') -> ParadoxScriptAttributesKeys.SCRIPTED_VARIABLE_KEY
            else -> ParadoxScriptAttributesKeys.STRING_KEY
        }
    }
    
    //相关高级语言功能（代码高亮、引用解析等）改为使用语言注入实现
    //see: icu.windea.pls.script.injection.ParadoxScriptLanguageInjector
    
    //region
    //override fun getAttributesKeyConfig(element: ParadoxScriptStringExpressionElement): CwtConfig<*>? {
    //    if(!getSettings().inference.parameterConfig) return null
    //    val parameterElement = argumentNode?.getReference(element)?.resolve() ?: return null
    //    return ParadoxParameterHandler.getInferredConfig(parameterElement)
    //}
    //
    //override fun getReference(element: ParadoxScriptStringExpressionElement): Reference? {
    //    if(!getSettings().inference.parameterConfig) return null
    //    if(valueNode == null) return null
    //    if(text.isEmpty()) return null
    //    val reference = valueNode.getReference(element)
    //    if(reference?.resolve() == null) return null //skip if script value cannot be resolved
    //    val rangeInElement = rangeInExpression.shiftRight(ParadoxExpressionHandler.getExpressionOffset(element))
    //    return Reference(element, rangeInElement, this)
    //}
    //
    //class Reference(
    //    element: ParadoxScriptStringExpressionElement,
    //    rangeInElement: TextRange,
    //    val node: ArgumentValueNode
    //) : PsiReferenceBase<ParadoxScriptStringExpressionElement>(element, rangeInElement) {
    //    override fun handleElementRename(newElementName: String): PsiElement {
    //        throw IncorrectOperationException()
    //    }
    //    
    //    override fun resolve(): PsiElement? {
    //        return null
    //    }
    //}
    //endregion
    
    companion object Resolver {
        fun resolve(text: String, textRange: TextRange, valueNode: ParadoxScriptValueNode?, argumentNode: ParadoxScriptValueArgumentNode?, configGroup: CwtConfigGroup): ParadoxScriptValueArgumentValueNode {
            return ParadoxScriptValueArgumentValueNode(text, textRange, valueNode, argumentNode, configGroup)
        }
    }
}
