package icu.windea.pls.localisation.intentions

import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.codeInsight.intention.*
import com.intellij.ide.plugins.*
import com.intellij.notification.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.ide.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.progress.impl.*
import com.intellij.openapi.project.*
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.*
import com.intellij.openapi.wm.ex.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import com.intellij.xml.util.*
import icu.windea.pls.*
import icu.windea.pls.core.ui.*
import icu.windea.pls.localisation.*
import icu.windea.pls.localisation.psi.*
import java.awt.datatransfer.*

//https://github.com/YiiGuxing/TranslationPlugin/blob/master/src/main/kotlin/cn/yiiguxing/plugin/translate/action/TranslateAndReplaceAction.kt

/**
 * 复制本地化到剪贴板并在这之前尝试将本地化文本翻译到指定的语言区域的意向。（鼠标位置对应的本地化，或者鼠标选取范围涉及到的所有本地化）
 *
 * 复制的文本格式为：`KEY:0 "TEXT"`
 */
class CopyLocalisationForLocaleIntention : IntentionAction, PriorityAction {
	override fun getPriority() = PriorityAction.Priority.HIGH
	
	override fun startInWriteAction() = false
	
	override fun getText() = PlsBundle.message("localisation.intention.copyLocalisationForLocale")
	
	override fun getFamilyName() = text
	
	override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
		if(editor == null || file == null) return false
		if(file.language != ParadoxLocalisationLanguage) return false
		val selectionStart = editor.selectionModel.selectionStart
		val selectionEnd = editor.selectionModel.selectionEnd
		return if(selectionStart == selectionEnd) {
			val originalElement = file.findElementAt(selectionStart)
			originalElement?.parentOfType<ParadoxLocalisationProperty>() != null
		} else {
			val originalStartElement = file.findElementAt(selectionStart) ?: return false
			val originalEndElement = file.findElementAt(selectionEnd) ?: return false
			hasLocalisationPropertiesBetween(originalStartElement, originalEndElement)
		}
	}
	
	//在翻译之前，要将特殊标记用<>包围起来，这样翻译后就可以保留特殊标记（期望如此）
	
	override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
		if(editor == null || file == null) return
		if(file.language != ParadoxLocalisationLanguage) return
		val selectionStart = editor.selectionModel.selectionStart
		val selectionEnd = editor.selectionModel.selectionEnd
		val elements = if(selectionStart == selectionEnd) {
			val originalElement = file.findElementAt(selectionStart)
			val element = originalElement?.parentOfType<ParadoxLocalisationProperty>() ?: return
			listOf(element)
		} else {
			val originalStartElement = file.findElementAt(selectionStart) ?: return
			val originalEndElement = file.findElementAt(selectionEnd) ?: return
			findLocalisationPropertiesBetween(originalStartElement, originalEndElement)
		}
		if(elements.isEmpty()) return
		
		val localeDialog = SelectParadoxLocaleDialog(preferredParadoxLocale())
		if(!localeDialog.showAndGet()) return
		
		//check whether the translation plugin is installed and enabled 
		val isEnabled = PluginManagerCore.isPluginInstalled(translationPluginId) && !PluginManagerCore.isDisabled(translationPluginId)
		if(!isEnabled) {
			NotificationGroupManager.getInstance().getNotificationGroup("pls")
				.createNotification(PlsBundle.message("translation.notification.pluginNotEnabled.title"),
					XmlStringUtil.wrapInHtml(PlsBundle.message("translation.notification.pluginNotEnabled.content")),
					NotificationType.WARNING)
				.addAction(NotificationAction.create(PlsBundle.message("translation.notification.pluginNotEnabled.action.1")) { _, notification -> 
					installAndEnable(project, setOf(translationPluginId)) { notification.expire() }
				})
				.notify(project)
		}
		
		val targetLocale = localeDialog.locale
		val targetLang = targetLocale.languageTag.let { runCatching { Lang[it] }.getOrNull() }
		val textList = elements.map { element ->
			if(targetLang == null) return@map element.text
			val sourceLang = element.localeConfig?.languageTag?.let { runCatching { Lang[it] }.getOrNull() } ?: return@map element.text
			if(sourceLang == targetLang) return@map element.text
			if(!isEnabled) return@map element.text
			
			val key = element.name
			val indicatorTitle = PlsBundle.message("translation.indicator.translate.title", key, targetLocale)
			val progressIndicator = BackgroundableProcessIndicator(project, indicatorTitle, null, "", true)
			progressIndicator.text = PlsBundle.message("translation.indicator.translate.text1", key)
			progressIndicator.text2 = PlsBundle.message("translation.indicator.translate.text2", text.processBeforeTranslate() ?: text)
			progressIndicator.addStateDelegate(ProcessIndicatorDelegate(progressIndicator))
			
			var resultText = element.text
			TranslateService.translate(element.text, sourceLang, targetLang, object : TranslateListener {
				override fun onSuccess(translation: Translation) {
					if(checkProcessCanceledAndEditorDisposed(progressIndicator, project, editor)) return
					
					progressIndicator.processFinish()
					resultText = translation.translation
				}
				
				override fun onError(throwable: Throwable) {
					if(checkProcessCanceledAndEditorDisposed(progressIndicator, project, editor)) return
					
					progressIndicator.processFinish()
					TranslationNotifications.showTranslationErrorNotification(project, PlsBundle.message("translation.notification.translate.failed.title", key, targetLocale), null, throwable)
				}
			})
			resultText
			
		}
		val finalText = textList.joinToString("\n")
		CopyPasteManager.getInstance().setContents(StringSelection(finalText))
	}
	
	fun checkProcessCanceledAndEditorDisposed(progressIndicator: BackgroundableProcessIndicator, project: Project?, editor: Editor?): Boolean {
		if(progressIndicator.isCanceled) {
			// no need to finish the progress indicator,
			// because it's already finished in the delegate.
			return true
		}
		if((project != null && project.isDisposed) || editor.let { it == null || it.isDisposed }) {
			progressIndicator.processFinish()
			return true
		}
		return false
	}
	
	private class ProcessIndicatorDelegate(
		private val progressIndicator: BackgroundableProcessIndicator,
	) : EmptyProgressIndicatorBase(), ProgressIndicatorEx {
		override fun cancel() {
			// 在用户取消的时候使`progressIndicator`立即结束并且不再显示，否则需要等待任务结束才能跟着结束
			progressIndicator.processFinish()
		}
		
		override fun isCanceled(): Boolean = true
		override fun finish(task: TaskInfo) = Unit
		override fun isFinished(task: TaskInfo): Boolean = true
		override fun wasStarted(): Boolean = false
		override fun processFinish() = Unit
		override fun initStateFrom(indicator: ProgressIndicator) = Unit
		
		override fun addStateDelegate(delegate: ProgressIndicatorEx) {
			throw UnsupportedOperationException()
		}
	}
}