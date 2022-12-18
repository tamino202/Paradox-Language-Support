package icu.windea.pls.script.inspections.advanced

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.ui.dsl.builder.*
import icu.windea.pls.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.core.*
import icu.windea.pls.core.handler.ParadoxCwtConfigHandler.resolvePropertyConfigs
import icu.windea.pls.core.handler.ParadoxCwtConfigHandler.resolveValueConfigs
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.quickfix.*
import icu.windea.pls.script.psi.*
import javax.swing.*

/**
 * 定义声明中无法解析的表达式的检查。
 */
class UnresolvedExpressionInspection : LocalInspectionTool() {
	@JvmField var showExpectInfo = true
	@JvmField var checkPropertyKey = true
	@JvmField var checkPropertyValue = true
	@JvmField var checkValue = true
	
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
		if(file !is ParadoxScriptFile) return null
		val holder = ProblemsHolder(manager, file, isOnTheFly)
		file.accept(object : ParadoxScriptRecursiveElementWalkingVisitor() {
			override fun visitElement(element: PsiElement) {
				if(element !is ParadoxScriptExpressionContextElement) return
				super.visitElement(element)
			}
			
			override fun visitProperty(element: ParadoxScriptProperty) {
				ProgressManager.checkCanceled()
				run {
					val shouldCheck = checkPropertyKey
					if(!shouldCheck) return@run
					//skip checking property if property key may contain parameters
					if(element.propertyKey.isParameterAwareExpression()) return
					val definitionMemberInfo = element.definitionMemberInfo
					if(definitionMemberInfo == null || definitionMemberInfo.isDefinition) return@run
					val matchType = CwtConfigMatchType.INSPECTION
					val configs = resolvePropertyConfigs(element, matchType = matchType)
					val config = configs.firstOrNull()
					if(config == null) {
						val expectConfigs = if(showExpectInfo) {
							element.findParentProperty()?.definitionMemberInfo?.getChildPropertyConfigs()
						} else null
						val expect = expectConfigs?.mapTo(mutableSetOf()) { it.expression }?.joinToString()
						val message = when {
							expect == null -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.1.1", element.expression)
							expect.isNotEmpty() -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.1.2", element.expression, expect)
							else -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.1.3", element.expression)
						}
						val fix = ImportGameOrModDirectoryFix(element)
						holder.registerProblem(element, message, fix)
						return
					}
				}
				super.visitProperty(element)
			}
			
			override fun visitValue(element: ParadoxScriptValue) {
				ProgressManager.checkCanceled()
				run {
					val shouldCheck = when {
						element is ParadoxScriptedVariableReference -> return //skip
						element.isPropertyValue() -> checkPropertyValue
						element.isBlockValue() -> checkValue
						else -> return //skip
					}
					if(!shouldCheck) return@run
					//skip checking value if it may contain parameters
					if(element is ParadoxScriptString && element.isParameterAwareExpression()) return
					val definitionMemberInfo = element.definitionMemberInfo
					if(definitionMemberInfo == null || definitionMemberInfo.isDefinition) return@run
					val matchType = CwtConfigMatchType.INSPECTION
					val configs = resolveValueConfigs(element, matchType = matchType, orDefault = false)
					val config = configs.firstOrNull()
					if(config == null) {
						val expectConfigs = if(showExpectInfo) {
							resolveValueConfigs(element, orDefault = true)
						} else null
						val expect = expectConfigs?.mapTo(mutableSetOf()) { it.expression }?.joinToString()
						val message = when {
							expect == null -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.2.1", element.expression)
							expect.isNotEmpty() -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.2.2", element.expression, expect)
							else -> PlsBundle.message("script.inspection.advanced.unresolvedExpression.description.2.3", element.expression)
						}
						val fix = ImportGameOrModDirectoryFix(element)
						holder.registerProblem(element, message, fix)
						//skip checking children
						return
					}
				}
				super.visitValue(element)
			}
		})
		return holder.resultsArray
	}
	
	override fun createOptionsPanel(): JComponent {
		return panel {
			row {
				checkBox(PlsBundle.message("script.inspection.advanced.unresolvedExpression.option.showExpectInfo"))
					.bindSelected(::showExpectInfo)
					.actionListener { _, component -> showExpectInfo = component.isSelected }
			}
			row {
				checkBox(PlsBundle.message("script.inspection.advanced.unresolvedExpression.option.checkPropertyKey"))
					.bindSelected(::checkPropertyKey)
					.actionListener { _, component -> checkPropertyKey = component.isSelected }
			}
			row {
				checkBox(PlsBundle.message("script.inspection.advanced.unresolvedExpression.option.checkPropertyValue"))
					.bindSelected(::checkPropertyValue)
					.actionListener { _, component -> checkPropertyValue = component.isSelected }
			}
			row {
				checkBox(PlsBundle.message("script.inspection.advanced.unresolvedExpression.option.checkValue"))
					.bindSelected(::checkValue)
					.actionListener { _, component -> checkValue = component.isSelected }
			}
		}
	}
}