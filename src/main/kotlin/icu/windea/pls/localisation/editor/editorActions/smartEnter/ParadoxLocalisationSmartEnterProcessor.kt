package icu.windea.pls.localisation.editor.editorActions.smartEnter

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.pls.core.*
import icu.windea.pls.core.model.*
import icu.windea.pls.localisation.psi.ParadoxLocalisationProperty
import icu.windea.pls.localisation.psi.ParadoxLocalisationPropertyKey

/**
 * 用于补充当前声明。
 */
class ParadoxLocalisationSmartEnterProcessor: SmartEnterProcessorWithFixers() {
	init {
		addFixers(
			AfterLocalisationKeyFixer()
		)
	}
	
	class AfterLocalisationKeyFixer: Fixer<ParadoxLocalisationSmartEnterProcessor>() {
		override fun apply(editor: Editor, processor: ParadoxLocalisationSmartEnterProcessor, element: PsiElement) {
			//要求光标位于行尾（忽略空白），且位于属性名（propertyKey）的末尾（忽略空白）
			val caretOffset = editor.caretModel.offset
			if(!editor.document.isAtLineEnd(caretOffset)) return
			val targetElement = element
				.parent.castOrNull<ParadoxLocalisationPropertyKey>()
				//.parentOfType<ParadoxLocalisationPropertyKey>()
				?: return
			val endOffset = element.textRange.endOffset
			if(caretOffset != endOffset){
				editor.document.deleteString(endOffset, caretOffset)
			}
			val property = targetElement.parent as? ParadoxLocalisationProperty ?: return
			val category = ParadoxLocalisationCategory.resolve(property)
			val text = when(category) {
				ParadoxLocalisationCategory.Localisation -> ":0 \"\""
				ParadoxLocalisationCategory.SyncedLocalisation -> ": \"\""
				null -> ": \"\""
			}
			EditorModificationUtil.insertStringAtCaret(editor, text, false, text.length - 1)
		}
	}
}