package icu.windea.pls.localisation.references

import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.core.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.localisation.highlighter.*
import icu.windea.pls.localisation.psi.*

/**
 * @see icu.windea.pls.localisation.codeInsight.completion.ParadoxLocalisationColorCompletionProvider
 */
class ParadoxLocalisationColorReference(
	element: ParadoxLocalisationColorfulText,
	rangeInElement: TextRange
) : PsiReferenceBase<ParadoxLocalisationColorfulText>(element, rangeInElement), PsiSmartReference {
	override fun handleElementRename(newElementName: String): PsiElement {
		//尝试重命名关联的definition
		val resolved = resolve()
		when {
			resolved == null -> pass()
			resolved is PsiNamedElement -> resolved.setName(newElementName)
			else -> throw IncorrectOperationException() //不允许重命名
		}
		//重命名当前元素
		return element.setName(newElementName)
	}
	
	override fun resolve(): PsiElement? {
		return resolve(true)
	}
	
	override fun resolve(exact: Boolean): PsiElement? {
		return element.colorConfig?.pointer?.element
	}
	
	override fun resolveTextAttributesKey(): TextAttributesKey? {
		return element.colorConfig?.color?.let { ParadoxLocalisationAttributesKeys.getColorKey(it) }
	}
}
