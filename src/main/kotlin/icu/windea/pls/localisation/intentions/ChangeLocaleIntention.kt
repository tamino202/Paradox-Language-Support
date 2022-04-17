package icu.windea.pls.localisation.intentions

import com.intellij.codeInsight.intention.*
import com.intellij.openapi.application.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.openapi.ui.popup.util.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.config.internal.*
import icu.windea.pls.localisation.psi.*

class ChangeLocaleIntention : IntentionAction {
	companion object {
		private val _name = PlsBundle.message("localisation.intention.changeLocale")
		private val _title = PlsBundle.message("localisation.intention.changeLocale.title")
	}
	
	override fun startInWriteAction() = false
	
	override fun getText() = _name
	
	override fun getFamilyName() = _name
	
	override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
		if(editor == null || file == null) return false
		val originalElement = file.findElementAt(editor.caretModel.offset) ?: return false
		val element = originalElement.parent
		return element is ParadoxLocalisationLocale
	}
	
	override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
		if(editor == null || file == null) return
		val originalElement = file.findElementAt(editor.caretModel.offset) ?: return
		val element = originalElement.parent
		if(element is ParadoxLocalisationLocale) {
			JBPopupFactory.getInstance().createListPopup(Popup(element, getInternalConfig().locales)).showInBestPositionFor(editor)
		}
	}
	
	private class Popup(
		private val value: ParadoxLocalisationLocale,
		values: Array<ParadoxLocaleConfig>
	) : BaseListPopupStep<ParadoxLocaleConfig>(_title, *values) {
		override fun getIconFor(value: ParadoxLocaleConfig) = value.icon
		
		override fun getTextFor(value: ParadoxLocaleConfig) = value.popupText
		
		override fun getDefaultOptionIndex() = 0
		
		override fun isSpeedSearchEnabled(): Boolean = true
		
		override fun onChosen(selectedValue: ParadoxLocaleConfig, finalChoice: Boolean): PopupStep<*>? {
			runWriteAction { value.name = selectedValue.name }
			return PopupStep.FINAL_CHOICE
		}
	}
}

