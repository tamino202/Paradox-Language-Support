package icu.windea.pls.model.expression.complex.nodes

import com.intellij.openapi.util.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.psi.*
import icu.windea.pls.model.expression.complex.*

class ParadoxErrorCommandFieldNode(
    override val text: String,
    override val rangeInExpression: TextRange
) : ParadoxComplexExpressionNode.Base(), ParadoxCommandFieldNode, ParadoxErrorNode {
    override fun getUnresolvedError(element: ParadoxExpressionElement): ParadoxComplexExpressionError? {
        if(nodes.isNotEmpty()) return null
        if(text.isEmpty()) return null
        if(text.isParameterized()) return null
        return ParadoxComplexExpressionErrors.unresolvedCommandField(rangeInExpression, text)
    }
}
