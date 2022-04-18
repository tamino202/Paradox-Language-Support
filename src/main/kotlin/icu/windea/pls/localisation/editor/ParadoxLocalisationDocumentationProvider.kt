package icu.windea.pls.localisation.editor

import com.intellij.lang.documentation.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.localisation.psi.*

class ParadoxLocalisationDocumentationProvider : AbstractDocumentationProvider() {
	override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, `object`: Any?, element: PsiElement?): PsiElement? {
		if(`object` is PsiElement) return `object`
		return super.getDocumentationElementForLookupItem(psiManager, `object`, element)
	}
	
	override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String?, context: PsiElement?): PsiElement? {
		if(link == null || context == null) return null
		return resolveLink(link, context)
	}
	
	override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
		return when {
			element is ParadoxLocalisationProperty -> getPropertyInfo(element)
			element is ParadoxLocalisationLocale -> getLocaleConfig(element)
			element is ParadoxLocalisationIcon -> getIconInfo(element)
			element is ParadoxLocalisationSequentialNumber -> getSequentialNumberConfig(element)
			element is ParadoxLocalisationCommandScope -> getCommandScopeInfo(element)
			element is ParadoxLocalisationCommandField -> getCommandFieldInfo(element)
			element is ParadoxLocalisationColorfulText -> getColorConfig(element)
			else -> null
		}
	}
	
	private fun getPropertyInfo(element: ParadoxLocalisationProperty): String {
		val name = element.name
		val category = element.category
		if(category != null) return getLocalisationInfo(element, name, category)
		return buildString {
			buildPropertyDefinition(element)
		}
	}
	
	private fun getLocalisationInfo(element: ParadoxLocalisationProperty, name: String, category: ParadoxLocalisationCategory): String {
		return buildString {
			buildLocalisationDefinition(element, category, name)
		}
	}
	
	private fun getLocaleConfig(element: ParadoxLocalisationLocale): String {
		val name = element.name
		return buildString {
			buildLocaleDefinition(name)
		}
	}
	
	private fun getIconInfo(element: ParadoxLocalisationIcon): String {
		val name = element.name
		return buildString {
			buildIconDefinition(name)
		}
	}
	
	private fun getSequentialNumberConfig(element: ParadoxLocalisationSequentialNumber): String {
		val name = element.name
		return buildString {
			buildSequentialNumberDefinition(name)
		}
	}
	
	private fun getCommandScopeInfo(element: ParadoxLocalisationCommandScope): String {
		val name = element.name
		return buildString {
			buildCommandScopeDefinition(name)
		}
	}
	
	private fun getCommandFieldInfo(element: ParadoxLocalisationCommandField): String {
		val name = element.name
		return buildString {
			buildCommandFieldDefinition(name)
		}
	}
	
	private fun getColorConfig(element: ParadoxLocalisationColorfulText): String {
		val name = element.name
		return buildString {
			buildColorDefinition(name)
		}
	}
	
	override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
		return when {
			element is ParadoxLocalisationProperty -> getPropertyDoc(element)
			element is ParadoxLocalisationLocale -> getLocaleDoc(element)
			element is ParadoxLocalisationIcon -> getIconDoc(element)
			element is ParadoxLocalisationSequentialNumber -> getSequentialNumberDoc(element)
			element is ParadoxLocalisationCommandScope -> getCommandScopeDoc(element)
			element is ParadoxLocalisationCommandField -> getCommandFieldDoc(element)
			element is ParadoxLocalisationColorfulText -> getColorDoc(element)
			else -> null
		}
	}
	
	private fun getPropertyDoc(element: ParadoxLocalisationProperty): String {
		val name = element.name
		val category = element.category
		if(category != null) return getLocalisationDoc(element, name, category)
		return buildString {
			buildPropertyDefinition(element)
		}
	}
	
	private fun getLocalisationDoc(element: ParadoxLocalisationProperty, name: String, category: ParadoxLocalisationCategory): String {
		return buildString {
			buildLocalisationDefinition(element, category, name)
			buildLineCommentContent(element)
			buildLocalisationSections(element)
		}
	}
	
	private fun getLocaleDoc(element: ParadoxLocalisationLocale): String {
		val name = element.name
		return buildString {
			buildLocaleDefinition(name)
			buildLocaleContent(element)
		}
	}
	
	private fun getIconDoc(element: ParadoxLocalisationIcon): String {
		val name = element.name
		return buildString {
			buildIconDefinition(name)
		}
	}
	
	private fun getSequentialNumberDoc(element: ParadoxLocalisationSequentialNumber): String {
		val name = element.name
		return buildString {
			buildSequentialNumberDefinition(name)
			buildSequentialNumberContent(element)
		}
	}
	
	private fun getCommandScopeDoc(element: ParadoxLocalisationCommandScope): String {
		val name = element.name
		return buildString {
			buildCommandScopeDefinition(name)
		}
	}
	
	private fun getCommandFieldDoc(element: ParadoxLocalisationCommandField): String {
		val name = element.name
		return buildString {
			buildCommandFieldDefinition(name)
		}
	}
	
	private fun getColorDoc(element: ParadoxLocalisationColorfulText): String {
		return buildString {
			val name = element.name
			definition {
				append("(localisation color) <b>").append(name).append("</b>")
			}
			//描述
			val colorConfig = element.colorConfig
			if(colorConfig != null) {
				val description = colorConfig.description
				val colorText = colorConfig.colorText
				content {
					append(description).append(" - ").append(colorText) //注明颜色
				}
			}
		}
	}
	
	private fun StringBuilder.buildPropertyDefinition(element: ParadoxLocalisationProperty) {
		definition {
			element.fileInfo?.let { fileInfo -> appendFileInfo(fileInfo).appendBr() }
			append("(localisation property) <b>").append(element.name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildLocalisationDefinition(element: ParadoxLocalisationProperty, category: ParadoxLocalisationCategory, name: String) {
		definition {
			element.fileInfo?.let { fileInfo -> appendFileInfo(fileInfo).appendBr() }
			append("(${category.id}) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildLocalisationSections(element: ParadoxLocalisationProperty) {
		//本地化文本
		if(renderLocalisationText) {
			val richText = element.renderText()
			if(richText.isNotEmpty()) {
				sections {
					section("Text", richText)
				}
			}
		}
	}
	
	private fun StringBuilder.buildLocaleDefinition(name: String) {
		definition {
			append("(localisation locale) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildLocaleContent(element: ParadoxLocalisationLocale) {
		//描述
		val localeConfig = element.localeConfig
		if(localeConfig != null) {
			content {
				append(localeConfig.description)
			}
		}
	}
	
	private fun StringBuilder.buildIconDefinition(name: String) {
		definition {
			append("(localisation icon) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildSequentialNumberDefinition(name: String) {
		definition {
			append("(localisation sequential number) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildSequentialNumberContent(element: ParadoxLocalisationSequentialNumber) {
		//描述
		val sequentialNumberConfig = element.sequentialNumberConfig
		if(sequentialNumberConfig != null) {
			content {
				append(sequentialNumberConfig.description)
			}
		}
	}
	
	private fun StringBuilder.buildCommandScopeDefinition(name: String) {
		definition {
			append("(localisation command scope) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildCommandFieldDefinition(name: String) {
		definition {
			append("(localisation command field) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildColorDefinition(name: String) {
		definition {
			append("(localisation color) <b>").append(name).append("</b>")
		}
	}
	
	private fun StringBuilder.buildLineCommentContent(element: PsiElement) {
		//单行注释文本
		if(renderLineCommentText) {
			val docText = getDocTextFromPreviousComment(element)
			if(docText.isNotEmpty()) {
				content {
					append(docText)
				}
			}
		}
	}
}
