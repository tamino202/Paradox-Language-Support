package icu.windea.pls.script.psi

import com.intellij.openapi.project.*
import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.*

object ParadoxScriptElementFactory {
	@JvmStatic
	fun createDummyFile(project: Project, text: String): ParadoxScriptFile {
		return PsiFileFactory.getInstance(project).createFileFromText(ParadoxScriptLanguage, text).cast()
	}
	
	@JvmStatic
	fun createLine(project: Project):PsiElement{
		return ParadoxLocalisationElementFactory.createDummyFile(project, "\n").firstChild
	}
	
	fun createRootBlock(project: Project, text: String): ParadoxScriptRootBlock {
		return createDummyFile(project, text).findChild()!!
	}
	
	@JvmStatic
	fun createVariable(project: Project, name: String, value: String): ParadoxScriptScriptedVariable {
		return createRootBlock(project, "@$name=$value").findChild()!!
	}
	
	@JvmStatic
	fun createVariableName(project: Project, name: String): ParadoxScriptScriptedVariableName {
		return createVariable(project, name, "0").findChild()!!
	}
	
	@JvmStatic
	fun createProperty(project: Project, key: String, value: String): ParadoxScriptProperty {
		val usedKey = key.quoteIfNecessary()
		return createRootBlock(project, "$usedKey=$value").findChild()!!
	}
	
	@JvmStatic
	fun createPropertyKey(project: Project, key: String): ParadoxScriptPropertyKey {
		return createProperty(project, key, "0").findChild()!!
	}
	
	@JvmStatic
	fun createPropertyValue(project: Project, value: String): ParadoxScriptValue {
		return createProperty(project, "a", value).findChild()!!
	}
	
	@JvmStatic
	fun createValue(project: Project, value: String): ParadoxScriptValue {
		return createRootBlock(project, value).findChild()!!
	}
	
	@JvmStatic
	fun createVariableReference(project: Project, name: String): ParadoxScriptScriptedVariableReference {
		return createPropertyValue(project, "@$name").findChild()!!
	}
	
	@JvmStatic
	fun createString(project: Project, value: String): ParadoxScriptString {
		val usedValue = value.quoteIfNecessary()
		return createRootBlock(project, usedValue).findChild()!!
	}
	
	@JvmStatic
	fun createInlineMath(project: Project, expression: String): ParadoxScriptInlineMath {
		return createPropertyValue(project, "@[$expression]").findChild()!!
	}
	
	@JvmStatic
	fun createInlineMathVariableReference(project: Project, name: String): ParadoxScriptInlineMathScriptedVariableReference {
		return createInlineMath(project, name).findChild()!!
	}
	
	@JvmStatic
	fun createParameter(project: Project, name: String): ParadoxScriptParameter {
		val text = "$$name$"
		return createRootBlock(project, text).findChild<ParadoxScriptString>()!!.findChild()!!
	}
	
	@JvmStatic
	fun createParameterCondition(project: Project, expression: String, itemsText: String): ParadoxScriptParameterCondition {
		val text = "a = { [[$expression] $itemsText ] }"
		return createRootBlock(project, text)
			.findChild<ParadoxScriptProperty>()!!
			.findChild<ParadoxScriptBlock>()!!
			.findChild()!!
	}
	
	@JvmStatic
	fun createParameterConditionParameter(project: Project, name: String): ParadoxScriptParameterConditionParameter {
		return createParameterCondition(project, name, "a")
			.findChild<ParadoxScriptParameterConditionExpression>()!!
			.findChild()!!
	}
	
	@JvmStatic
	fun createInlineMathParameter(project: Project, name: String): ParadoxScriptInlineMathParameter {
		val text = "$$name$"
		return createInlineMath(project, text).findChild()!!
	}
}
