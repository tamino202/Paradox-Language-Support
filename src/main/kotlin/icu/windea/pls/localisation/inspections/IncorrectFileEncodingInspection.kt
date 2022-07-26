package icu.windea.pls.localisation.inspections

import com.intellij.codeInsight.daemon.impl.actions.*
import com.intellij.codeInspection.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.encoding.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.localisation.psi.*

//com.intellij.openapi.editor.actions.AddBomAction
//com.intellij.openapi.editor.actions.RemoveBomAction

/**
 * 不正确的的文件编码的检查。
 *
 * 注意：[icu.windea.pls.core.ParadoxFileTypeOverrider]会尝试自动修正文件的BOM。
 *
 * 提供快速修复：
 * * 改为正确的文件编码
 */
class IncorrectFileEncodingInspection : LocalInspectionTool() {
	override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
		if(file !is ParadoxLocalisationFile) return null //不期望的结果
		val virtualFile = file.virtualFile ?: return null
		val charset = virtualFile.charset
		val hasBom = virtualFile.hasBom(utf8Bom)
		val isValid = charset == Charsets.UTF_8 && hasBom
		if(!isValid) {
			val holder = ProblemsHolder(manager, file, isOnTheFly)
			val bom = if(hasBom) "BOM" else "NO BOM"
			holder.registerProblem(file, PlsBundle.message("localisation.inspection.incorrectFileEncoding.description", charset, bom),
				ChangeToCorrectFileEncodingFix(file)
			)
			return holder.resultsArray
		}
		return null
	}
	
	private class ChangeToCorrectFileEncodingFix(
		element: PsiElement
	) : LocalQuickFixAndIntentionActionOnPsiElement(element), IntentionActionWithFixAllOption {
		override fun getText() = PlsBundle.message("localisation.inspection.incorrectFileEncoding.quickfix.1")
		
		override fun getFamilyName() = text
		
		override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
			val virtualFile = file.virtualFile
			val isUtf8 = virtualFile.charset == Charsets.UTF_8
			val hasBom = virtualFile.hasBom(utf8Bom)
			if(!hasBom) {
				try {
					virtualFile.addBom(utf8Bom)
				} catch(e: Exception) {
					thisLogger().warn("Unexpected exception occurred on attempt to add BOM from file $this", e)
				}
			}
			if(!isUtf8) virtualFile.charset = Charsets.UTF_8
			val fileDocumentManager = FileDocumentManager.getInstance()
			val document = fileDocumentManager.getDocument(virtualFile)
			if(document != null) {
				if(!isUtf8) {
					ChangeFileEncodingAction.changeTo(project, document, editor, virtualFile, Charsets.UTF_8, EncodingUtil.Magic8.ABSOLUTELY, EncodingUtil.Magic8.ABSOLUTELY)
				}
				fileDocumentManager.saveDocument(document) //保存文件
			}
		}
		
		override fun startInWriteAction() = false
	}
}
