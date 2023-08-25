package icu.windea.pls.core.refactoring

import com.intellij.lang.refactoring.*
import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.psi.*

class ParadoxRefactoringSupportProvider : RefactoringSupportProvider() {
	override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
		return when {
			element is ParadoxScriptScriptedVariable -> true
			element is ParadoxScriptProperty && element.definitionInfo.let { it != null && it.typeConfig.nameField == null } -> true
			element is ParadoxScriptStringExpressionElement -> true
			element is ParadoxLocalisationProperty -> true
			else -> false
		}
	}
}
