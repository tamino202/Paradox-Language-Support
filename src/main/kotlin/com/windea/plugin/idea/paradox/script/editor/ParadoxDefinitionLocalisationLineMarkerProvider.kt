package com.windea.plugin.idea.paradox.script.editor

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.navigation.*
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.*
import com.intellij.ui.awt.*
import com.intellij.util.*
import com.windea.plugin.idea.paradox.*
import com.windea.plugin.idea.paradox.script.psi.*

class ParadoxDefinitionLocalisationLineMarkerProvider: LineMarkerProviderDescriptor(){
	companion object {
		private val _name = message("paradox.script.gutterIcon.definitionLocalisation")
		private val _title = message("paradox.script.gutterIcon.definitionLocalisation.title")
		private fun _tooltip(name: String,type:String) = message("paradox.script.gutterIcon.definitionLocalisation.tooltip", name, type)
	}
	
	override fun getName() = _name
	
	override fun getIcon() = definitionLocalisationGutterIcon
	
	override fun getLineMarkerInfo(element: PsiElement): LineMarker? {
		return when(element) {
			is ParadoxScriptProperty -> {
				val typeInfo = element.paradoxTypeInfo ?: return null
				if(!typeInfo.hasLocalisation) return null //没有localisation时不加上gutterIcon
				LineMarker(element, typeInfo)
			}
			else -> null
		}
	}
	
	class LineMarker(element: ParadoxScriptProperty,typeInfo: ParadoxTypeInfo) : LineMarkerInfo<PsiElement>(
		element.propertyKey.let { it.propertyKeyId ?: it.quotedPropertyKeyId!! },
		element.textRange,
		definitionLocalisationGutterIcon,
		{ typeInfo.localisation.joinToString("<br>"){ (k,v)-> _tooltip(v,k.value) }},
		{ mouseEvent, _ ->
			val names = typeInfo.localisationValueKeys
			val project = element.project
			val elements = findLocalisations(names, null, project,hasDefault=true,keepOrder= true).toTypedArray()
			when(elements.size) {
				0 -> {}
				1 -> OpenSourceUtil.navigate(true, elements.first())
				else -> NavigationUtil.getPsiElementPopup(elements, _title).show(RelativePoint(mouseEvent))
			}
		},
		GutterIconRenderer.Alignment.RIGHT,
		{ _name }
	)
}