package icu.windea.pls.script.psi.impl

import com.intellij.openapi.project.*
import com.intellij.pom.*
import com.intellij.psi.*
import com.intellij.psi.impl.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.core.model.*
import icu.windea.pls.script.psi.*
import javax.swing.*

/**
 * 值集中的值并不存在一个真正意义上的声明处，用这个模拟。
 */
class ParadoxValueSetValueElement(
	element: PsiElement,
	private val name: String,
	val valueSetName: String,
	private val project: Project,
	val gameType: ParadoxGameType
) : RenameableFakePsiElement(element), ParadoxScriptNamedElement, Navigatable {
	override fun getText(): String {
		return name
	}
	
	override fun getName(): String {
		return name
	}
	
	override fun getTypeName(): String {
		return PlsBundle.message("script.description.valueSetValue")
	}
	
	override fun getIcon(): Icon {
		return PlsIcons.ValueSetValue
	}
	
	override fun getNameIdentifier(): PsiElement {
		return this
	}
	
	override fun getProject(): Project {
		return project
	}
	
	override fun navigate(requestFocus: Boolean) {
		
	}
	
	override fun canNavigate(): Boolean {
		return false // false -> click to show usages
	}
	
	override fun isEquivalentTo(another: PsiElement?): Boolean {
		return another is ParadoxValueSetValueElement &&
			name == another.name &&
			valueSetName == another.valueSetName &&
			project == another.project &&
			gameType == another.gameType
	}
}