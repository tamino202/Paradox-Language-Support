package icu.windea.pls.core.codeInsight.generation

import com.intellij.codeInsight.*
import com.intellij.codeInsight.actions.*
import com.intellij.codeInsight.generation.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.actions.*
import icu.windea.pls.script.psi.*

/**
 * 生成当前定义的所有（缺失的）本地化。
 */
class GenerateLocalisationsAction : BaseCodeInsightAction(), GenerateActionPopupTemplateInjector {
    private val handler = GenerateLocalisationsHandler()
    
    override fun getHandler(): CodeInsightActionHandler {
        return handler
    }
    
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        return file is ParadoxScriptFile && file.fileInfo != null
    }
    
    override fun update(event: AnActionEvent) {
        //当选中的文件是脚本文件时不显示 - 目前不认为存在相关本地化
        //当选中的文件是定义或者光标位置的元素是定义的rootKey或者作为名字的字符串时启用
        val presentation = event.presentation
        presentation.isEnabledAndVisible = false
        val project = event.project
        val editor = event.editor
        if(editor == null || project == null) return
        val file = PsiUtilBase.getPsiFileInEditor(editor, project)
        if(file !is ParadoxScriptFile) return
        presentation.isVisible = true
        val offset = editor.caretModel.offset
        val element = findElement(file, offset)
        val isEnabled = when {
            element == null -> false
            element.isDefinitionRootKeyOrName() -> true
            else -> false
        }
        presentation.isEnabled = isEnabled
    }
    
    private fun findElement(file: PsiFile, offset: Int): ParadoxScriptStringExpressionElement? {
        //direct parent
        return file.findElementAt(offset) {
            it.parent as? ParadoxScriptStringExpressionElement
        }?.takeIf { it.isExpression() }
    }
    
    override fun createEditTemplateAction(dataContext: DataContext?): AnAction? {
        return null
    }
}