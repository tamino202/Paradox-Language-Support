package icu.windea.pls.script.expression.reference

import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.script.psi.*

class ParadoxScriptValueFieldPrefixReference(
	element: ParadoxScriptExpressionElement,
	rangeInElement: TextRange,
	private val resolved: List<PsiElement>?
): PsiPolyVariantReferenceBase<ParadoxScriptExpressionElement>(element, rangeInElement){
	override fun handleElementRename(newElementName: String): ParadoxScriptExpressionElement {
		throw IncorrectOperationException() //不允许重命名
	}
	
	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		return resolved?.mapToArray { PsiElementResolveResult(it) } ?: ResolveResult.EMPTY_ARRAY
	}
}

class ParadoxScriptValueFieldDataSourceReference(
	element: ParadoxScriptExpressionElement,
	rangeInElement: TextRange,
	private val linkConfigs: List<CwtLinkConfig>
) : PsiPolyVariantReferenceBase<ParadoxScriptExpressionElement>(element, rangeInElement) {
	override fun handleElementRename(newElementName: String): ParadoxScriptExpressionElement {
		throw IncorrectOperationException() //不允许重命名（尽管有些类型如定义实际上可以重命名）
	}
	
	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		val element = element
		return linkConfigs.mapNotNull { linkConfig ->
			val dataSource = linkConfig.dataSource ?: return@mapNotNull null
			val resolved = CwtConfigHandler.resolveScriptExpression(element, dataSource, linkConfig, rangeInElement) ?: return@mapNotNull null
			ParadoxScriptValueFieldDataSourceResolveResult(resolved, true, dataSource)
		}.toTypedArray()
	}
}

class ParadoxScriptValueFieldDataSourceResolveResult(
	element: PsiElement,
	validResult: Boolean = true,
	val expression: CwtValueExpression
) : PsiElementResolveResult(element, validResult)