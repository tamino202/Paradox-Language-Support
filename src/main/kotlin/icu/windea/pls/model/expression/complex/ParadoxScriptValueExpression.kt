package icu.windea.pls.model.expression.complex

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.util.*
import com.intellij.util.*
import icu.windea.pls.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.*
import icu.windea.pls.lang.codeInsight.completion.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.model.expression.complex.errors.*
import icu.windea.pls.model.expression.complex.nodes.*
import icu.windea.pls.script.psi.*

/**
 * 封装值表达式。作为[ParadoxValueFieldExpression]的一部分。
 *
 * 语法：
 *
 * ```bnf
 * script_value_expression ::= script_value ("|" (param_name "|" param_value "|")+)?
 * script_value ::= TOKEN //matching config expression "<script_value>"
 * param_name ::= TOKEN //parameter name, no surrounding "$"
 * param_value ::= TOKEN //boolean, int, float or string
 * ```
 *
 * 示例：
 *
 * * `some_sv`
 * * `some_sv|PARAM|VALUE|`
 */
interface ParadoxScriptValueExpression : ParadoxComplexExpression {
    val config: CwtConfig<*>
    
    companion object Resolver {
        fun resolve(expression: String, range: TextRange, configGroup: CwtConfigGroup, config: CwtConfig<*>): ParadoxScriptValueExpression? =
            doResolve(expression, range, configGroup, config)
    }
}

//Implementations

private fun doResolve(expression: String, range: TextRange, configGroup: CwtConfigGroup, config: CwtConfig<*>): ParadoxScriptValueExpression? {
    val parameterRanges = CwtConfigHandler.getParameterRangesInExpression(expression)
    //skip if text is a parameter with unary operator prefix
    if(CwtConfigHandler.isUnaryOperatorAwareParameter(expression, parameterRanges)) return null
    
    val incomplete = PlsStatus.incompleteComplexExpression.get() ?: false
    
    val nodes = mutableListOf<ParadoxExpressionNode>()
    val offset = range.startOffset
    var n = 0
    var scriptValueNode: ParadoxScriptValueExpressionNode? = null
    var parameterNode: ParadoxScriptValueArgumentExpressionNode? = null
    var index: Int
    var tokenIndex = -1
    var startIndex = 0
    val textLength = expression.length
    while(tokenIndex < textLength) {
        index = tokenIndex + 1
        tokenIndex = expression.indexOf('|', index)
        if(tokenIndex != -1 && CwtConfigHandler.inParameterRanges(parameterRanges, tokenIndex)) continue //这里需要跳过参数文本
        val pipeNode = if(tokenIndex != -1) {
            val pipeRange = TextRange.create(tokenIndex + offset, tokenIndex + 1 + offset)
            ParadoxMarkerExpressionNode("|", pipeRange)
        } else {
            null
        }
        if(tokenIndex == -1) {
            tokenIndex = textLength
        }
        if(!incomplete && index == tokenIndex && tokenIndex == textLength) break
        //resolve node
        val nodeText = expression.substring(startIndex, tokenIndex)
        val nodeRange = TextRange.create(startIndex + offset, tokenIndex + offset)
        startIndex = tokenIndex + 1
        val node = when {
            n == 0 -> {
                ParadoxScriptValueExpressionNode.resolve(nodeText, nodeRange, config, configGroup)
                    .also { scriptValueNode = it }
            }
            n % 2 == 1 -> {
                ParadoxScriptValueArgumentExpressionNode.resolve(nodeText, nodeRange, scriptValueNode, configGroup)
                    .also { parameterNode = it }
            }
            n % 2 == 0 -> {
                ParadoxScriptValueArgumentValueExpressionNode.resolve(nodeText, nodeRange, scriptValueNode, parameterNode, configGroup)
            }
            else -> throw InternalError()
        }
        nodes.add(node)
        if(pipeNode != null) nodes.add(pipeNode)
        n++
    }
    return ParadoxScriptValueExpressionImpl(expression, range, nodes, configGroup, config)
}

private class ParadoxScriptValueExpressionImpl(
    override val text: String,
    override val rangeInExpression: TextRange,
    override val nodes: List<ParadoxExpressionNode>,
    override val configGroup: CwtConfigGroup,
    override val config: CwtConfig<*>
) : ParadoxScriptValueExpression {
    override fun validate(): List<ParadoxExpressionError> {
        var malformed = false
        val errors = mutableListOf<ParadoxExpressionError>()
        var pipeCount = 0
        var lastIsParameter = false
        for(node in nodes) {
            if(node is ParadoxTokenExpressionNode) {
                pipeCount++
            } else {
                if(!malformed && (node.text.isEmpty() || !isValid(node))) {
                    malformed = true
                }
                when(node) {
                    is ParadoxScriptValueArgumentExpressionNode -> lastIsParameter = true
                    is ParadoxScriptValueArgumentValueExpressionNode -> lastIsParameter = false
                }
            }
        }
        //0, 1, 3, 5, ...
        if(!malformed && pipeCount != 0 && pipeCount % 2 == 0) {
            malformed = true
        }
        if(malformed) {
            val error = ParadoxMalformedScriptValueExpressionExpressionError(rangeInExpression, PlsBundle.message("script.expression.malformedScriptValueExpression", text))
            errors.add(error)
        }
        if(lastIsParameter) {
            val error = ParadoxMissingParameterValueExpressionExpressionError(rangeInExpression, PlsBundle.message("script.expression.missingParameterValueExpression"))
            errors.add(error)
        }
        return errors
    }
    
    private fun isValid(node: ParadoxExpressionNode): Boolean {
        return when(node) {
            is ParadoxScriptValueArgumentExpressionNode -> node.text.isExactIdentifier()
            is ParadoxScriptValueArgumentValueExpressionNode -> true //兼容数字文本、字符串文本、封装变量引用等，这里直接返回true
            else -> node.text.isExactParameterAwareIdentifier()
        }
    }
    
    override fun complete(context: ProcessingContext, result: CompletionResultSet) {
        val keyword = context.keyword
        val keywordOffset = context.keywordOffset
        val offsetInParent = context.offsetInParent!!
        val isKey = context.isKey
        val scopeContext = context.scopeContext ?: ParadoxScopeHandler.getAnyScopeContext()
        val scopeMatched = context.scopeMatched
        
        context.scopeContext = null //don't check now
        
        for(node in nodes) {
            val nodeRange = node.rangeInExpression
            val inRange = offsetInParent >= nodeRange.startOffset && offsetInParent <= nodeRange.endOffset
            if(node is ParadoxScriptValueExpressionNode) {
                if(inRange) {
                    val keywordToUse = node.text.substring(0, offsetInParent - nodeRange.startOffset)
                    val resultToUse = result.withPrefixMatcher(keywordToUse)
                    context.keyword = keywordToUse
                    context.keywordOffset = node.rangeInExpression.startOffset
                    val config = context.config
                    val configs = context.configs
                    context.config = this.config
                    context.configs = emptyList()
                    ParadoxCompletionManager.completeScriptExpression(context, resultToUse)
                    context.config = config
                    context.configs = configs
                }
            } else if(node is ParadoxScriptValueArgumentExpressionNode) {
                if(inRange && scriptValueNode.text.isNotEmpty()) {
                    val keywordToUse = node.text.substring(0, offsetInParent - nodeRange.startOffset)
                    val resultToUse = result.withPrefixMatcher(keywordToUse)
                    context.keyword = keywordToUse
                    context.keywordOffset = node.rangeInExpression.startOffset
                    ParadoxParameterHandler.completeArguments(context.contextElement!!, context, resultToUse)
                }
            } else if(node is ParadoxScriptValueArgumentValueExpressionNode && getSettings().inference.configContextForParameters) {
                if(inRange && scriptValueNode.text.isNotEmpty()) {
                    //尝试提示传入参数的值
                    run {
                        val keywordToUse = node.text.substring(0, offsetInParent - nodeRange.startOffset)
                        val resultToUse = result.withPrefixMatcher(keywordToUse)
                        val element = context.contextElement as? ParadoxScriptStringExpressionElement ?: return@run
                        val parameterElement = node.argumentNode?.getReference(element)?.resolve() ?: return@run
                        val inferredContextConfigs = ParadoxParameterHandler.getInferredContextConfigs(parameterElement)
                        val inferredConfig = inferredContextConfigs.singleOrNull()?.castOrNull<CwtValueConfig>() ?: return@run
                        val config = context.config
                        val configs = context.configs
                        context.keyword = keywordToUse
                        context.keywordOffset = node.rangeInExpression.startOffset
                        context.config = inferredConfig
                        context.configs = emptyList()
                        ParadoxCompletionManager.completeScriptExpression(context, resultToUse)
                        context.config = config
                        context.configs = configs
                    }
                }
            }
        }
        context.keyword = keyword
        context.keywordOffset = keywordOffset
        context.isKey = isKey
        context.scopeContext = scopeContext
        context.scopeMatched = scopeMatched
    }
    
    override fun equals(other: Any?): Boolean {
        return this === other || other is ParadoxScriptValueExpression && text == other.text
    }
    
    override fun hashCode(): Int {
        return text.hashCode()
    }
    
    override fun toString(): String {
        return text
    }
}
