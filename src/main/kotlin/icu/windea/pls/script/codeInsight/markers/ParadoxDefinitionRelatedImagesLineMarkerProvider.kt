package icu.windea.pls.script.codeInsight.markers

import com.intellij.codeInsight.daemon.*
import com.intellij.navigation.*
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.navigation.*
import icu.windea.pls.script.psi.*

/**
 * 定义的相关图片（relatedImages，对应类型为sprite的定义或者DDS图片）的装订线图标提供器。
 */
class ParadoxDefinitionRelatedImagesLineMarkerProvider : RelatedItemLineMarkerProvider() {
	override fun getName() = PlsBundle.message("script.gutterIcon.relatedImages")
	
	override fun getIcon() = PlsIcons.Gutter.RelatedImage
	
	override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
		//何时显示装订线图标：element是definition，且definitionInfo.images不为空，且计算得到的keys不为空
		if(element !is ParadoxScriptProperty) return
		val definitionInfo = element.definitionInfo ?: return
		val imageInfos = definitionInfo.images
		if(imageInfos.isEmpty()) return
		
		//显示在提示中 & 可导航：去重后的一组DDS文件的filePath，或者sprite的definitionKey，不包括没有对应的图片的项，按解析顺序排序
		val icon = PlsIcons.Gutter.RelatedImage
		val tooltipBuilder = StringBuilder()
		val project = element.project
		val keys = mutableSetOf<String>()
		val targets = mutableSetOf<PsiElement>() //这里需要考虑基于引用相等去重
		var isFirst = true
		runReadAction {
			for((key, locationExpression) in imageInfos) {
				val (filePath, files) = locationExpression.resolveAll(element, definitionInfo, project) ?: continue
				if(files.isNotEmpty()) targets.addAll(files)
				if(files.isNotEmpty() && keys.add(key)) {
					if(isFirst) isFirst = false else tooltipBuilder.appendBr()
					tooltipBuilder.append(PlsDocBundle.message("prefix.relatedImage")).append(" ").append(key).append(" = ").append(filePath)
				}
			}
		}
		if(keys.isEmpty()) return
		if(targets.isEmpty()) return
		val locationElement = element.propertyKey.let { it.propertyKeyId ?: it.quotedPropertyKeyId!! }
		val tooltip = tooltipBuilder.toString()
		val lineMarkerInfo = createNavigationGutterIconBuilder(icon) { createGotoRelatedItem(targets) }
			.setTooltipText(tooltip)
			.setPopupTitle(PlsBundle.message("script.gutterIcon.relatedImages.title"))
			.setTargets(targets)
			.setAlignment(GutterIconRenderer.Alignment.RIGHT)
			.setNamer { PlsBundle.message("script.gutterIcon.relatedImages") }
			.createLineMarkerInfo(locationElement)
		//NavigateAction.setNavigateAction(
		//	lineMarkerInfo,
		//	PlsBundle.message("script.gutterIcon.relatedImages.action"),
		//	PlsActions.GutterGotoRelatedImage
		//)
		result.add(lineMarkerInfo)
	}
	
	private fun createGotoRelatedItem(targets: Set<PsiElement>): Collection<GotoRelatedItem> {
		return ParadoxGotoRelatedItem.createItems(targets, PlsBundle.message("script.gutterIcon.relatedImages.group"))
	}
}