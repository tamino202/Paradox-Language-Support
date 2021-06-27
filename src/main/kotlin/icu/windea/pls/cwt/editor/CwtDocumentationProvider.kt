package icu.windea.pls.cwt.editor

import com.intellij.lang.documentation.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.cwt.psi.*
import java.util.*

class CwtDocumentationProvider : AbstractDocumentationProvider() {
	override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
		return when(element) {
			is CwtProperty -> getPropertyInfo(element)
			is CwtValue -> getValueInfo(element)
			else -> null
		}
	}
	
	private fun getPropertyInfo(element: CwtProperty): String {
		return buildString {
			val name = element.name
			definition {
				append("<b>").append(name.escapeXmlOrAnonymous()).append("</b>")
			}
		}
	}
	
	private fun getValueInfo(element: CwtValue): String {
		return buildString {
			val name = element.name
			definition {
				append("<b>").append(name.escapeXmlOrAnonymous()).append("</b>")
			}
		}
	}
	
	override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
		return when(element) {
			is CwtProperty -> getPropertyDoc(element)
			is CwtValue -> getValueDoc(element)
			else -> null
		}
	}
	
	private fun getPropertyDoc(element: CwtProperty): String {
		return buildString {
			val name = element.name
			definition {
				append("<b>").append(name.escapeXmlOrAnonymous()).append("</b>")
			}
			//文档注释，以###开始
			val documentation = getDocumentation(element)
			if(documentation != null) {
				content {
					append(documentation)
				}
			}
		}
	}
	
	private fun getValueDoc(element: CwtValue): String {
		return buildString {
			val name = element.name
			definition {
				append("<b>").append(name.escapeXmlOrAnonymous()).append("</b>")
			}
			//文档注释，以###开始
			val documentation = getDocumentation(element)
			if(documentation != null) {
				content {
					append(documentation)
				}
			}
		}
	}
	
	private fun getDocumentation(element: PsiElement): String? {
		var current: PsiElement = element
		val documentationElements = LinkedList<CwtDocumentationText>()
		while(true) {
			current = current.prevSibling ?: break
			when {
				current is CwtDocumentationComment -> {
					val documentationText = current.documentationText
					if(documentationText != null) documentationElements.addFirst(documentationText)
				}
				current is PsiWhiteSpace || current is PsiComment -> continue
				else -> break
			}
		}
		if(documentationElements.isEmpty()) return null
		return documentationElements.joinToString("\n") { it.text.orEmpty() }.trim()
	}
	
	override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String?, context: PsiElement?): PsiElement? {
		if(link == null || context == null) return null
		return resolveLink(link, context)
	}
}