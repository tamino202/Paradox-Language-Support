package icu.windea.pls.script.codeInsight.markers

import com.intellij.codeInsight.daemon.*
import com.intellij.navigation.*
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.navigation.*
import icu.windea.pls.core.selector.chained.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.psi.*

/**
 * 定义的相关本地化（relatedLocalisation，对应localisation，不对应localisation_synced）的装订线图标提供器。
 */
class ParadoxDefinitionRelatedLocalisationsLineMarkerProvider : RelatedItemLineMarkerProvider() {
	override fun getName() = PlsBundle.message("script.gutterIcon.relatedLocalisations")
	
	override fun getIcon() = PlsIcons.Gutter.RelatedLocalisations
	
	override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
		//何时显示装订线图标：element是definition，且definitionInfo.localisation不为空，且计算得到的keys不为空
		if(element !is ParadoxScriptProperty) return
		val definitionInfo = element.definitionInfo ?: return
		val localisationInfos = definitionInfo.localisation
		if(localisationInfos.isEmpty()) return
		
		//显示在提示中 & 可导航：去重后的一组本地化的键名，不包括没有对应的本地化的项，按解析顺序排序
		val icon = PlsIcons.Gutter.RelatedLocalisations
		val tooltipBuilder = StringBuilder()
		val project = element.project
		val keys = mutableSetOf<String>()
		val targets = mutableSetOf<ParadoxLocalisationProperty>() //这里需要考虑基于引用相等去重
		var isFirst = true
		for((key, locationExpression) in localisationInfos) {
			val selector = localisationSelector().gameTypeFrom(element).preferRootFrom(element).preferLocale(preferredParadoxLocale())
			val resolved = locationExpression.resolveAll(definitionInfo.name, element, project, selector = selector) ?: continue
			if(resolved.localisations.isNotEmpty()) {
				targets.addAll(resolved.localisations)
			}
			if(resolved.message != null) {
				if(isFirst) isFirst = false else tooltipBuilder.appendBr()
				tooltipBuilder.append(PlsDocBundle.message("prefix.relatedLocalisation")).append(" ").append(key).append(" = ").append(resolved.message)
			} else if(resolved.localisations.isNotEmpty() && keys.add(key)) {
				if(isFirst) isFirst = false else tooltipBuilder.appendBr()
				tooltipBuilder.append(PlsDocBundle.message("prefix.relatedLocalisation")).append(" ").append(key).append(" = ").append(resolved.key)
			}
		}
		if(keys.isEmpty()) return
		if(targets.isEmpty()) return
		val locationElement = element.propertyKey.let { it.propertyKeyId ?: it.quotedPropertyKeyId!! }
		val tooltip = tooltipBuilder.toString()
		val lineMarkerInfo = createNavigationGutterIconBuilder(icon) { createGotoRelatedItem(targets) }
			.setTooltipText(tooltip)
			.setPopupTitle(PlsBundle.message("script.gutterIcon.relatedLocalisations.title"))
			.setTargets(targets)
			.setAlignment(GutterIconRenderer.Alignment.RIGHT)
			.setNamer { PlsBundle.message("script.gutterIcon.relatedLocalisations") }
			.createLineMarkerInfo(locationElement)
		//NavigateAction.setNavigateAction(
		//	lineMarkerInfo,
		//	PlsBundle.message("script.gutterIcon.relatedLocalisation.action"),
		//	PlsActions.GutterGotoRelatedLocalisation
		//)
		result.add(lineMarkerInfo)
	}
	
	private fun createGotoRelatedItem(targets: Set<ParadoxLocalisationProperty>): Collection<GotoRelatedItem> {
		return ParadoxGotoRelatedItem.createItems(targets, PlsBundle.message("script.gutterIcon.relatedLocalisations.group"))
	}
}