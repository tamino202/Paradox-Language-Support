package icu.windea.pls.script.codeInsight.makers

import com.intellij.codeInsight.daemon.*
import com.intellij.codeInsight.navigation.*
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.psi.*

/**
 * 定义的相关图片（relatedPictures，对应类型为sprite的定义或者DDS图片）的装订线图标提供器。
 */
class ParadoxRelatedPicturesLineMarkerProvider : RelatedItemLineMarkerProvider() {
	companion object {
		private val _name = PlsBundle.message("script.gutterIcon.relatedPictures")
		private val _title = PlsBundle.message("script.gutterIcon.relatedPictures.title")
	}
	
	override fun getName() = _name
	
	override fun getIcon() = PlsIcons.relatedPicturesGutterIcon
	
	override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
		//何时显示装订线图标：element是definition，且definitionInfo.pictures不为空，且计算得到的keys不为空
		if(element !is ParadoxScriptProperty) return
		val definitionInfo = element.definitionInfo ?: return
		val pictures = definitionInfo.pictures
		if(pictures.isEmpty()) return
		
		//显示在提示中 & 可导航：去重后的一组DDS文件的filePath，或者sprite的definitionKey，不包括可选且没有对应的图片的项，按解析顺序排序
		val icon = PlsIcons.relatedPicturesGutterIcon
		val tooltipBuilder = StringBuilder()
		val project = element.project
		val keys = mutableSetOf<String>()
		val targets = mutableSetOf<PsiElement>() //这里需要考虑基于引用相等去重
		var isFirst = true
		for((key, location, required) in definitionInfo.localisation) {
			val list = findPicturesByLocation(location, project)
			if(list.isNotEmpty()) targets.addAll(list)
			if((required || list.isNotEmpty()) && keys.add(key)) {
				if(isFirst) isFirst = false else tooltipBuilder.appendBr()
				tooltipBuilder.append("(related pictures) ").append(key).append(" = ").append(location)
			}
		}
		if(keys.isEmpty()) return
		val locationElement = element.propertyKey.let { it.propertyKeyId ?: it.quotedPropertyKeyId!! }
		val tooltip = tooltipBuilder.toString()
		val lineMarkerInfo = NavigationGutterIconBuilder.create(icon)
			.setTooltipText(tooltip)
			.setPopupTitle(_title)
			.setTargets(targets)
			.setAlignment(GutterIconRenderer.Alignment.RIGHT)
			.setNamer { _name }
			.createLineMarkerInfo(locationElement)
		result.add(lineMarkerInfo)
	}
	
}