package icu.windea.pls.script.codeInsight.navigation

import com.intellij.codeInsight.navigation.*
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.pom.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.config.core.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.selector.chained.*
import icu.windea.pls.script.psi.*
import java.util.*

//com.intellij.testIntegration.GotoTestOrCodeHandler

@Suppress("DialogTitleCapitalization")
class GotoRelatedImageHandler : GotoTargetHandler() {
	override fun getFeatureUsedKey(): String {
		return "navigation.goto.paradoxRelatedImage"
	}
	
	override fun getSourceAndTargetElements(editor: Editor, file: PsiFile): GotoData? {
		val project = file.project
		val offset = editor.caretModel.offset
		val element = findElement(file, offset) ?: return null
		if(element.isDefinitionRootKeyOrName()) {
			val definition = element.findParentDefinition() ?: return null
			val definitionInfo = definition.definitionInfo ?: return null
			val imageInfos = definitionInfo.images
			if(imageInfos.isEmpty()) return GotoData(definition, PsiElement.EMPTY_ARRAY, emptyList())
			val targets = Collections.synchronizedList(mutableListOf<PsiElement>())
			val runResult = ProgressManager.getInstance().runProcessWithProgressSynchronously({
				//need read action here
				runReadAction {
					for((_, locationExpression) in imageInfos) {
						ProgressManager.checkCanceled()
						val (_, files) = locationExpression.resolveAll(definition, definitionInfo, project) ?: continue
						if(files.isNotEmpty()) targets.addAll(files)
					}
				}
			}, PlsBundle.message("script.goto.relatedImage.search.1", definitionInfo.name), true, project)
			if(!runResult) return null
			return GotoData(definition, targets.toTypedArray(), emptyList())
		}
		val modifierInfo = ParadoxModifierHandler.getModifierInfo(element, project)
		if(modifierInfo != null) {
			val gameType = modifierInfo.gameType
			val configGroup = getCwtConfig(project).getValue(gameType)
			val targets = Collections.synchronizedList(mutableListOf<PsiElement>())
			val runResult = ProgressManager.getInstance().runProcessWithProgressSynchronously({
				runReadAction {
					val iconPaths = ParadoxModifierHandler.getModifierIconPaths(modifierInfo.name, configGroup)
					val iconFiles = iconPaths.firstNotNullOfOrNull {
						val iconSelector = fileSelector().gameType(gameType).preferRootFrom(element)
						val result = ParadoxFilePathSearch.search(it, project, selector = iconSelector).findAll()
						result.takeIfNotEmpty()
					} 
					if(iconFiles != null) targets.addAll(targets)
				}
			}, PlsBundle.message("script.goto.relatedImage.search.2", modifierInfo.name), true, project)
			if(!runResult) return null
			return GotoData(element, targets.toTypedArray(), emptyList())
		}
		return null
	}
	
	private fun findElement(file: PsiFile, offset: Int): ParadoxScriptStringExpressionElement? {
		//direct parent
		return file.findElementAt(offset) {
			it.parent as? ParadoxScriptStringExpressionElement
		}?.takeIf { it.isExpression() }
	}
	
	override fun shouldSortTargets(): Boolean {
		return false
	}
	
	override fun getChooserTitle(sourceElement: PsiElement, name: String?, length: Int, finished: Boolean): String {
		val definitionInfo = sourceElement.castOrNull<ParadoxScriptDefinitionElement>()?.definitionInfo
		if(definitionInfo != null) {
			val definitionName = definitionInfo.name.orAnonymous()
			return PlsBundle.message("script.goto.relatedImage.chooseTitle.1", definitionName.escapeXml())
		}
		val modifierInfo = sourceElement.castOrNull<ParadoxScriptStringExpressionElement>()?.modifierInfo
		if(modifierInfo != null) {
			val modifierName = modifierInfo.name
			return PlsBundle.message("script.goto.relatedImage.chooseTitle.2", modifierName.escapeXml())
		}
		val sourceName = sourceElement.text.unquote()
		return PlsBundle.message("script.goto.relatedImage.chooseTitle.0", sourceName.escapeXml())
	}
	
	override fun getFindUsagesTitle(sourceElement: PsiElement, name: String?, length: Int): String {
		val definitionInfo = sourceElement.castOrNull<ParadoxScriptDefinitionElement>()?.definitionInfo
		if(definitionInfo != null) {
			val definitionName = definitionInfo.name.orAnonymous()
			return PlsBundle.message("script.goto.relatedImage.findUsagesTitle.1", definitionName.escapeXml())
		}
		val modifierInfo = sourceElement.castOrNull<ParadoxScriptStringExpressionElement>()?.modifierInfo
		if(modifierInfo != null) {
			val modifierName = modifierInfo.name
			return PlsBundle.message("script.goto.relatedImage.findUsagesTitle.2", modifierName.escapeXml())
		}
		val sourceName = sourceElement.text.unquote()
		return PlsBundle.message("script.goto.relatedImage.findUsagesTitle.0", sourceName.escapeXml())
	}
	
	override fun getNotFoundMessage(project: Project, editor: Editor, file: PsiFile): String {
		return PlsBundle.message("script.goto.relatedImage.notFoundMessage")
	}
	
	override fun useEditorFont(): Boolean {
		return false
	}
	
	override fun navigateToElement(descriptor: Navigatable) {
		if(descriptor is PsiElement) {
			NavigationUtil.activateFileWithPsiElement(descriptor, true)
		} else {
			descriptor.navigate(true)
		}
	}
}
