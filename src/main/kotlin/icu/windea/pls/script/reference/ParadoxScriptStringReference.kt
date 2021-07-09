package icu.windea.pls.script.reference

import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.*
import icu.windea.pls.cwt.expression.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.script.psi.*
import kotlin.text.removeSurrounding

class ParadoxScriptStringReference(
	element: ParadoxScriptString,
	rangeInElement: TextRange
): PsiReferenceBase<ParadoxScriptString>(element,rangeInElement), PsiPolyVariantReference {
	override fun handleElementRename(newElementName: String): PsiElement {
		//尝试重命名关联的definition、localisation、syncedLocalisation等
		val resolved = resolve()
		when {
			resolved == null -> pass()
			!resolved.isWritable -> throw IncorrectOperationException(message("cannotBeRenamed"))
			else -> resolved.setName(newElementName)
		}
		return element.setValue(newElementName)
	}
	
	override fun resolve(): PsiNamedElement? {
		return resolveValue(element) //根据对应的expression进行解析
	}
	
	override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
		return multiResolveValue(element).mapToArray { PsiElementResolveResult(it) } //根据对应的expression进行解析
	}
	
	//代码提示功能由ParadoxScriptCompletionContributor统一实现
}