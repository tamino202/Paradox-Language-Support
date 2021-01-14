package com.windea.plugin.idea.paradox.localisation.psi.impl

import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.util.Iconable.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.refactoring.suggested.*
import com.intellij.util.*
import com.windea.plugin.idea.paradox.*
import com.windea.plugin.idea.paradox.localisation.psi.*
import com.windea.plugin.idea.paradox.localisation.psi.ParadoxLocalisationElementFactory.createPropertyKey
import com.windea.plugin.idea.paradox.localisation.psi.ParadoxLocalisationElementFactory.createPropertyReference
import com.windea.plugin.idea.paradox.localisation.psi.ParadoxLocalisationTypes.*
import com.windea.plugin.idea.paradox.localisation.reference.*
import javax.swing.*

//NOTE getName 确定进行重构和导航时显示的PsiElement的名字
//NOTE setName 确定进行重命名时的逻辑
//NOTE getTextOffset 确定选中一个PsiElement时，哪一部分会高亮显示
//NOTE getReference 确定选中一个PsiElement时，哪些其他的PsiElement会同时高亮显示

@Suppress("UNUSED_PARAMETER")
object ParadoxLocalisationPsiImplUtil {
	//region ParadoxLocalisationLocale
	@JvmStatic
	fun getName(element: ParadoxLocalisationLocale): String {
		return element.localeId.text.orEmpty()
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationLocale, name: String): PsiElement {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationLocale) {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationLocale): PsiElement {
		return element.localeId
	}
	
	@JvmStatic
	fun getIcon(element: ParadoxLocalisationLocale, @IconFlags flags: Int): Icon {
		return localisationLocaleIcon
	}
	
	@JvmStatic
	fun getParadoxLocale(element: ParadoxLocalisationLocale): ParadoxLocale? {
		return paradoxLocaleMap[element.name]
	}
	//endregion
	
	//region ParadoxLocalisationProperty
	@JvmStatic
	fun getName(element: ParadoxLocalisationProperty): String {
		return element.stub?.key ?: element.propertyKey.text
	}
	
	//TODO 检查是否是项目中的localisation，这样才允许重命名
	@JvmStatic
	fun setName(element: ParadoxLocalisationProperty, name: String): PsiElement {
		element.propertyKey.replace(createPropertyKey(element.project, name))
		return element
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationProperty) {
		
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationProperty): PsiElement {
		return element.propertyKey.propertyKeyId
	}
	
	@JvmStatic
	fun getIcon(element: ParadoxLocalisationProperty, @IconFlags flags: Int): Icon {
		return localisationPropertyIcon
	}
	
	@JvmStatic
	fun getValue(element: ParadoxLocalisationProperty): String? {
		return element.propertyValue?.text?.unquote()
	}
	
	@JvmStatic
	fun getParadoxLocale(element: ParadoxLocalisationProperty): ParadoxLocale? {
		return (element.containingFile as? ParadoxLocalisationFile)?.paradoxLocale
	}
	//endregion
	
	//region ParadoxLocalisationPropertyReference
	@JvmStatic
	fun getTextOffset(element: ParadoxLocalisationPropertyReference): Int {
		return element.startOffset + 1
	}
	
	@JvmStatic
	fun getName(element: ParadoxLocalisationPropertyReference): String {
		return element.propertyReferenceId?.text.orEmpty()
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationPropertyReference, name: String): PsiElement {
		element.propertyReferenceId?.replace(createPropertyReference(element.project, name).propertyReferenceId!!)
		return element
	}
	
	@JvmStatic
	fun getReference(element: ParadoxLocalisationPropertyReference): ParadoxLocalisationPropertyPsiReference? {
		val propertyReferenceId = element.propertyReferenceId ?: return null
		return ParadoxLocalisationPropertyPsiReference(element, propertyReferenceId.textRangeInParent)
	}
	
	@JvmStatic
	fun getParadoxColor(element: ParadoxLocalisationPropertyReference): ParadoxColor? {
		val colorId = element.propertyReferenceParameter?.text?.firstOrNull()
		if(colorId != null && colorId.isUpperCase()) {
			return paradoxColorMap[colorId.toString()]
		}
		return null
	}
	//endregion
	
	//region ParadoxLocalisationIcon
	@JvmStatic
	fun getName(element: ParadoxLocalisationIcon): String {
		return element.iconId?.text.orEmpty()
	}
	
	//TODO 实现icon引用解析后，只有项目中的icon才能重命名
	@JvmStatic
	fun setName(element: ParadoxLocalisationIcon, name: String): PsiElement {
		//element.iconId?.replace(createIcon(element.project, name).iconId!!)
		//return element
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationIcon) {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationIcon): PsiElement? {
		return element.iconId
	}
	
	@JvmStatic
	fun getTextOffset(element: ParadoxLocalisationIcon): Int {
		return element.startOffset + 1
	}
	
	@JvmStatic
	fun getReference(element: ParadoxLocalisationIcon): ParadoxLocalisationIconPsiReference? {
		val iconId = element.iconId ?: return null
		return ParadoxLocalisationIconPsiReference(element, iconId.textRangeInParent)
	}
	//endregion
	
	//region ParadoxLocalisationCommandIdentifier
	@JvmStatic
	fun getPrevIdentifier(element: ParadoxLocalisationCommandIdentifier): ParadoxLocalisationCommandIdentifier? {
		var separator = element.prevSibling ?: return null
		if(separator.elementType == TokenType.WHITE_SPACE) separator = separator.prevSibling ?: return null
		if(separator.elementType != COMMAND_SEPARATOR) return null
		var prev = separator.prevSibling ?: return null
		if(prev.elementType == TokenType.WHITE_SPACE) prev = prev.prevSibling ?: return null
		if(prev !is ParadoxLocalisationCommandIdentifier) return null
		return prev
	}
	
	@JvmStatic
	fun getNextIdentifier(element: ParadoxLocalisationCommandIdentifier): ParadoxLocalisationCommandIdentifier? {
		var separator = element.nextSibling ?: return null
		if(separator.elementType == TokenType.WHITE_SPACE) separator = separator.nextSibling ?: return null
		if(separator.elementType != COMMAND_SEPARATOR) return null
		var next = separator.nextSibling ?: return null
		if(next.elementType == TokenType.WHITE_SPACE) next = next.nextSibling ?: return null
		if(next !is ParadoxLocalisationCommandIdentifier) return null
		return next
	}
	//endregion
	
	//region ParadoxLocalisationCommandScope
	@JvmStatic
	fun getName(element: ParadoxLocalisationCommandScope): String {
		return element.text
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationCommandScope, name: String): PsiElement {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationCommandScope){
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationCommandScope): PsiElement {
		return element.commandScopeId
	}
	
	@JvmStatic
	fun getReference(element: ParadoxLocalisationCommandScope): ParadoxLocalisationCommandScopePsiReference {
		val commandScopeId = element.commandScopeId
		return ParadoxLocalisationCommandScopePsiReference(element, commandScopeId.textRangeInParent)
	}
	
	@JvmStatic
	fun getIcon(element: ParadoxLocalisationCommandScope, @IconFlags flags: Int): Icon {
		return localisationCommandScopeIcon
	}
	//endregion
	
	//region ParadoxLocalisationCommandField
	@JvmStatic
	fun getName(element: ParadoxLocalisationCommandField): String? {
		return element.commandFieldId?.text
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationCommandField, name: String): PsiElement {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationCommandField){
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationCommandField): PsiElement? {
		return element.commandFieldId
	}
	
	@JvmStatic
	fun getReference(element: ParadoxLocalisationCommandField): ParadoxLocalisationCommandFieldPsiReference? {
		val commandFieldId = element.commandFieldId ?: return null
		return ParadoxLocalisationCommandFieldPsiReference(element, commandFieldId.textRangeInParent)
	}
	
	@JvmStatic
	fun getIcon(element: ParadoxLocalisationCommandField, @IconFlags flags: Int): Icon {
		return localisationCommandFieldIcon
	}
	//endregion
	
	//region ParadoxLocalisationSerialNumber
	@JvmStatic
	fun getName(element: ParadoxLocalisationSerialNumber): String {
		return element.serialNumberId?.text?.toUpperCase().orEmpty()
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationSerialNumber, name: String): PsiElement {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationSerialNumber) {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationSerialNumber): PsiElement? {
		return element.serialNumberId
	}
	
	@JvmStatic
	fun getTextOffset(element: ParadoxLocalisationSerialNumber): Int {
		return element.startOffset + 1
	}
	
	@JvmStatic
	fun getParadoxSerialNumber(element: ParadoxLocalisationSerialNumber): ParadoxSerialNumber? {
		return paradoxSerialNumberMap[element.name]
	}
	//endregion
	
	//region ParadoxLocalisationColorfulText
	@JvmStatic
	fun getName(element: ParadoxLocalisationColorfulText): String {
		return element.colorId?.text?.toUpperCase().orEmpty()
	}
	
	@JvmStatic
	fun setName(element: ParadoxLocalisationColorfulText, name: String): PsiElement {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun checkRename(element: ParadoxLocalisationColorfulText) {
		throw IncorrectOperationException(message("cannotBeRenamed"))
	}
	
	@JvmStatic
	fun getNameIdentifier(element: ParadoxLocalisationColorfulText): PsiElement? {
		return element.colorId
	}
	
	@JvmStatic
	fun getTextOffset(element: ParadoxLocalisationColorfulText): Int {
		return element.startOffset + 1
	}
	
	@JvmStatic
	fun getParadoxColor(element: ParadoxLocalisationColorfulText): ParadoxColor? {
		return paradoxColorMap[element.name]
	}
	//endregion
}
