package icu.windea.pls.localisation.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.progress.*
import com.intellij.psi.util.*
import com.intellij.util.*
import icons.*
import icu.windea.pls.config.cwt.*
import icu.windea.pls.core.*
import icu.windea.pls.core.codeInsight.completion.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.selector.*
import icu.windea.pls.localisation.psi.*

/**
 * 提供命令字段名字的代码补全。
 */
class ParadoxLocalisationCommandFieldCompletionProvider : CompletionProvider<CompletionParameters>() {
	override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
		val offsetInParent = parameters.offset - parameters.position.textRange.startOffset
		val keyword = parameters.position.getKeyword(offsetInParent)
		val file = parameters.originalFile
		val project = file.project
		val gameType = file.fileInfo?.rootInfo?.gameType ?: return
		val configGroup = getCwtConfig(project).get(gameType) ?: return
		
		context.put(PlsCompletionKeys.completionTypeKey, parameters.completionType)
		context.put(PlsCompletionKeys.offsetInParentKey, offsetInParent)
		context.put(PlsCompletionKeys.keywordKey, keyword)
		context.put(PlsCompletionKeys.configGroupKey, configGroup)
		
		val prevScope = parameters.position.parentOfType<ParadoxLocalisationCommandIdentifier>()?.prevIdentifier?.name
		if(prevScope != null) context.put(PlsCompletionKeys.prevScopeKey, prevScope)
		
		//提示scope
		CwtConfigHandler.completeSystemScope(context, result)
		CwtConfigHandler.completeLocalisationCommandScope(context, result)
		
		//提示command
		CwtConfigHandler.completeLocalisationCommandField(context, result)
		
		//提示<scripted_loc>
		ProgressManager.checkCanceled()
		val scriptedLocSelector = definitionSelector().gameTypeFrom(file).preferRootFrom(file).distinctByName()
		val scriptedLocQuery = ParadoxDefinitionSearch.search("scripted_loc", project, selector = scriptedLocSelector)
		scriptedLocQuery.processResult { scriptedLoc ->
			val name = scriptedLoc.definitionInfo?.name.orEmpty() //不应该为空
			val icon = PlsIcons.Definition
			val tailText = " from <scripted_loc>"
			val typeFile = scriptedLoc.containingFile
			val lookupElement = LookupElementBuilder.create(scriptedLoc, name).withIcon(icon)
				.withTailText(tailText, true)
				.withTypeText(typeFile.name, typeFile.icon, true)
				.withCaseSensitivity(false) //忽略大小写
			result.addElement(lookupElement)
			true
		}
		
		//提示value[event_target]
		ProgressManager.checkCanceled()
		val eventTargetSelector = valueSetValueSelector().gameTypeFrom(file).preferRootFrom(file).distinctByValue()
		val eventTargetQuery = ParadoxValueSetValueSearch.search("event_target", project, selector = eventTargetSelector)
		eventTargetQuery.processResult { eventTarget ->
			val value = eventTarget.value.substringBefore('@')
			val icon = PlsIcons.ValueSetValue
			val tailText = " from value[event_target]"
			val lookupElement = LookupElementBuilder.create(eventTarget, value)
				.withIcon(icon)
				.withTailText(tailText, true)
				.withCaseSensitivity(false) //忽略大小写
			result.addElement(lookupElement)
			true
		}
		
		//提示value[global_event_target]
		ProgressManager.checkCanceled()
		val globalEventTargetSelector = valueSetValueSelector().gameTypeFrom(file).preferRootFrom(file).distinctByValue()
		val globalEventTargetQuery = ParadoxValueSetValueSearch.search("global_event_target", project, selector = globalEventTargetSelector)
		globalEventTargetQuery.processResult { globalEventTarget ->
			val value = globalEventTarget.value.substringBefore('@')
			val icon = PlsIcons.ValueSetValue
			val tailText = " from value[global_event_target]"
			val lookupElement = LookupElementBuilder.create(globalEventTarget, value)
				.withIcon(icon)
				.withTailText(tailText, true)
				.withCaseSensitivity(false) //忽略大小写
			result.addElement(lookupElement)
			true
		}
		
		//提示value[variable]
		ProgressManager.checkCanceled()
		val variableSelector = valueSetValueSelector().gameTypeFrom(file).preferRootFrom(file).distinctByValue()
		val variableQuery = ParadoxValueSetValueSearch.search("variable", project, selector = variableSelector)
		variableQuery.processResult { variable -> 
			val value = variable.value.substringBefore('@')
			val icon = PlsIcons.Variable
			val tailText = " from value[variable]"
			val lookupElement = LookupElementBuilder.create(variable, value)
				.withIcon(icon)
				.withTailText(tailText, true)
				.withCaseSensitivity(false) //忽略大小写
			result.addElement(lookupElement)
			true
		}
	}
}

