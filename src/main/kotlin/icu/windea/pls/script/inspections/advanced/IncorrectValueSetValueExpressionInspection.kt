package icu.windea.pls.script.inspections.advanced

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.config.script.*
import icu.windea.pls.core.*
import icu.windea.pls.core.expression.*
import icu.windea.pls.core.expression.errors.*
import icu.windea.pls.core.selector.*
import icu.windea.pls.script.psi.*

class IncorrectValueSetValueExpressionInspection : LocalInspectionTool() {
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
		if(file !is ParadoxScriptFile) return null
		val project = file.project
		val gameType = selectGameType(file)
		val holder = ProblemsHolder(manager, file, isOnTheFly)
		file.accept(object : PsiRecursiveElementWalkingVisitor() {
			override fun visitElement(element: PsiElement) {
				if(element is ParadoxScriptStringExpressionElement) visitStringExpressionElement(element)
				if(element.isExpressionOrMemberContext()) super.visitElement(element)
			}
			
			private fun visitStringExpressionElement(element: ParadoxScriptStringExpressionElement) {
				ProgressManager.checkCanceled()
				val config = ParadoxCwtConfigHandler.resolveConfigs(element).firstOrNull() ?: return
				val type = config.expression.type
				if(type == CwtDataType.Value || type == CwtDataType.ValueSet) {
					val value = element.value
					val gameTypeToUse = gameType ?: selectGameType(element) ?: return
					val configGroup = getCwtConfig(project).getValue(gameTypeToUse)
					val textRange = TextRange.create(0, value.length)
					val isKey = element is ParadoxScriptPropertyKey
					val valueSetValueExpression = ParadoxValueSetValueExpression.resolve(value, textRange, config, configGroup, isKey)
						?: return
					valueSetValueExpression.validate().forEach { error ->
						handleScriptExpressionError(element, error)
					}
					valueSetValueExpression.processAllNodes { node ->
						val unresolvedError = node.getUnresolvedError(element)
						if(unresolvedError != null) {
							handleScriptExpressionError(element, unresolvedError)
						}
						true
					}
				}
			}
			
			private fun handleScriptExpressionError(element: ParadoxScriptStringExpressionElement, error: ParadoxExpressionError) {
				holder.registerScriptExpressionError(element, error)
			}
		})
		return holder.resultsArray
	}
}