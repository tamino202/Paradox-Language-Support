package com.windea.plugin.idea.pls.localisation.reference

import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.windea.plugin.idea.pls.*
import com.windea.plugin.idea.pls.localisation.psi.*

class ParadoxLocalisationCommandFieldPsiReference(
	element: ParadoxLocalisationCommandField,
	rangeInElement: TextRange
) : PsiReferenceBase<ParadoxLocalisationCommandField>(element, rangeInElement) {
	override fun handleElementRename(newElementName: String): PsiElement {
		return element.setName(newElementName)
	}
	
	override fun resolve(): PsiElement? {
		val name = element.commandFieldId?.text?: return null
		val project = element.project
		return findScriptLocalisation(name, project)
	}
	
	//注意要传入elementName而非element
	override fun getVariants(): Array<out Any> {
		val project = element.project
		return findScriptLocalisations(project).mapToArray {
			LookupElementBuilder.create(it).withIcon(scriptLocalisationIcon).withTypeText(it.containingFile.name)
		}
	}
}
