package icu.windea.pls.lang.parameter.impl

import com.intellij.codeInsight.highlighting.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.expression.*
import icu.windea.pls.core.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.parameter.*
import icu.windea.pls.script.psi.*

open class ParadoxInlineScriptParameterSupport : ParadoxParameterSupport {
    companion object {
        @JvmField val containingContext = Key.create<SmartPsiElementPointer<ParadoxScriptDefinitionElement>>("paradox.parameterElement.containingContext")
        @JvmField val inlineScriptExpressionKey = Key.create<String>("paradox.parameterElement.inlineScriptExpression")
    }
    
    override fun isContext(element: ParadoxScriptDefinitionElement): Boolean {
        if(element !is ParadoxScriptFile) return false
        return ParadoxInlineScriptHandler.getInlineScriptExpression(element) != null
    }
    
    override fun findContext(element: PsiElement): ParadoxScriptDefinitionElement? {
        val context = element.containingFile?.castOrNull<ParadoxScriptFile>()
        return context?.takeIf { isContext(it) }
    }
    
    override fun findContextReferenceInfo(element: PsiElement, config: CwtDataConfig<*>?, from: ParadoxParameterContextReferenceInfo.From): ParadoxParameterContextReferenceInfo? {
        TODO("Not yet implemented")
    }
    
    override fun resolveParameter(element: ParadoxParameter): ParadoxParameterElement? {
        val name = element.name ?: return null
        return doResolveParameterOrArgument(element, name)
    }
    
    override fun resolveConditionParameter(element: ParadoxConditionParameter): ParadoxParameterElement? {
        val name = element.name ?: return null
        return doResolveParameterOrArgument(element, name)
    }
    
    private fun doResolveParameterOrArgument(element: PsiElement, name: String): ParadoxParameterElement? {
        val context = findContext(element) as? ParadoxScriptFile ?: return null
        val expression = ParadoxInlineScriptHandler.getInlineScriptExpression(context) ?: return null
        val readWriteAccess = getReadWriteAccess(element)
        val contextKey = "inline_script@$expression"
        val gameType = selectGameType(context) ?: return null
        val project = context.project
        val result = ParadoxParameterElement(element, name, context.name, contextKey, readWriteAccess, gameType, project)
        result.putUserData(containingContext, context.createPointer())
        result.putUserData(inlineScriptExpressionKey, expression)
        return result
    }
    
    override fun resolveArgument(element: ParadoxScriptExpressionElement, rangeInElement: TextRange?, config: CwtDataConfig<*>?): ParadoxParameterElement? {
        if(config == null) return null
        if(config !is CwtPropertyConfig || config.expression.type != CwtDataType.Parameter) return null
        //infer inline config
        val inlineConfig = config.castOrNull<CwtPropertyConfig>()?.parent?.castOrNull<CwtPropertyConfig>()?.inlineableConfig as? CwtInlineConfig ?: return null
        if(inlineConfig.name != "inline_script") return null
        val contextElement = element.findParentProperty(fromParentBlock = true)?.castOrNull<ParadoxScriptProperty>() ?: return null
        val propertyValue = contextElement.propertyValue ?: return null
        val expression = ParadoxInlineScriptHandler.getExpressionFromInlineConfig(propertyValue, inlineConfig) ?: return null
        val name = element.name
        val readWriteAccess = ReadWriteAccessDetector.Access.Write
        val contextKey = "inline_script@$expression"
        val gameType = config.info.configGroup.gameType ?: return null
        val project = config.info.configGroup.project
        val result = ParadoxParameterElement(element, name, expression, contextKey, readWriteAccess, gameType, project)
        result.putUserData(inlineScriptExpressionKey, expression)
        return result
    }
    
    private fun getReadWriteAccess(element: PsiElement) = when {
        element is ParadoxParameter -> ReadWriteAccessDetector.Access.Read
        element is ParadoxConditionParameter -> ReadWriteAccessDetector.Access.Read
        else -> ReadWriteAccessDetector.Access.Write
    }
    
    override fun getContainingContext(element: ParadoxParameterElement): ParadoxScriptDefinitionElement? {
        return element.getUserData(containingContext)?.element
    }
    
    override fun processContext(element: ParadoxParameterElement, processor: (ParadoxScriptDefinitionElement) -> Boolean): Boolean {
        val expression = element.getUserData(inlineScriptExpressionKey) ?: return false
        val project = element.project
        ParadoxInlineScriptHandler.processInlineScript(expression, element, project, processor)
        return true
    }
    
    override fun processContext(element: PsiElement, contextReferenceInfo: ParadoxParameterContextReferenceInfo, processor: (ParadoxScriptDefinitionElement) -> Boolean): Boolean {
        val expression = contextReferenceInfo.getUserData(inlineScriptExpressionKey) ?: return false
        val project = contextReferenceInfo.project
        ParadoxInlineScriptHandler.processInlineScript(expression, element, project, processor)
        return true
    }
    
    override fun buildDocumentationDefinition(element: ParadoxParameterElement, builder: StringBuilder): Boolean = with(builder) {
        val inlineScriptExpression = element.getUserData(inlineScriptExpressionKey) ?: return false
        if(inlineScriptExpression.isEmpty()) return false
        val filePath = ParadoxInlineScriptHandler.getInlineScriptFilePath(inlineScriptExpression) ?: return false
        
        //不加上文件信息
        
        //加上名字
        val name = element.name
        append(PlsBundle.message("prefix.parameter")).append(" <b>").append(name.escapeXml().orAnonymous()).append("</b>")
        
        //加上所属内联脚本信息
        val gameType = element.gameType
        appendBr().appendIndent()
        append(PlsBundle.message("ofInlineScript")).append(" ")
        appendFilePathLink(gameType, filePath, inlineScriptExpression, element)
        return true
    }
}