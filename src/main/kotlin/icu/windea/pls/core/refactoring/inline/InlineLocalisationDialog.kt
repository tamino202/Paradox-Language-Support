package icu.windea.pls.core.refactoring.inline

import com.intellij.openapi.editor.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.*
import com.intellij.refactoring.inline.*
import icu.windea.pls.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.core.refactoring.*
import icu.windea.pls.core.search.scope.*
import icu.windea.pls.localisation.*
import icu.windea.pls.localisation.psi.ParadoxLocalisationProperty
import icu.windea.pls.script.*

class InlineLocalisationDialog(
    project: Project,
    private val element: ParadoxLocalisationProperty,
    private val reference: PsiReference?,
    private val editor: Editor?
) : InlineOptionsDialog(project, true, element) {
    private val optimizedScope = ParadoxSearchScope.fromElement(element)
        ?.withFileTypes(ParadoxLocalisationFileType)
        ?.intersectWith(GlobalSearchScope.projectScope(project))
        ?: GlobalSearchScope.projectScope(project)
    
    private val occurrencesNumber = getNumberOfOccurrences(element)
    
    init {
        title = PlsBundle.message("title.inline.localisation")
        myInvokedOnReference = reference != null
        init()
        helpAction.isEnabled = false
    }
    
    override fun getNameLabelText(): String {
        val name = element.name.orEmpty()
        return when {
            occurrencesNumber > -1 -> PlsBundle.message("inline.localisation.occurrences", name, occurrencesNumber)
            else -> PlsBundle.message("inline.localisation.label", name)
        }
    }
    
    override fun getBorderTitle(): String {
        return PlsBundle.message("inline.localisation.border.title")
    }
    
    override fun getInlineThisText(): String {
        return PlsBundle.message("inline.localisation.inline.this")
    }
    
    override fun getInlineAllText(): String {
        return if(element.isWritable) PlsBundle.message("inline.localisation.inline.all.remove")
        else PlsBundle.message("inline.localisation.inline.all")
    }
    
    override fun getKeepTheDeclarationText(): String {
        return if(element.isWritable) PlsBundle.message("inline.localisation.inline.all.keep")
        else super.getKeepTheDeclarationText()
    }
    
    override fun allowInlineAll(): Boolean {
        return true
    }
    
    
    override fun isInlineThis(): Boolean {
        return ParadoxRefactoringSettings.getInstance().inlineLocalisationThis
    }
    
    override fun isKeepTheDeclarationByDefault(): Boolean {
        return ParadoxRefactoringSettings.getInstance().inlineLocalisationKeep
    }
    
    override fun ignoreOccurrence(reference: PsiReference?): Boolean {
        return true
    }
    
    override fun getNumberOfOccurrences(nameIdentifierOwner: PsiNameIdentifierOwner): Int {
        val element = nameIdentifierOwner
        return getNumberOfOccurrences(element, this::ignoreOccurrence) {
            ReferencesSearch.search(element, optimizedScope)
        }
    }
    
    override fun doAction() {
        val processor = InlineLocalisationProcessor(project, optimizedScope, element, reference, editor, isInlineThisOnly, isKeepTheDeclaration())
        invokeRefactoring(processor)
        val settings = ParadoxRefactoringSettings.getInstance()
        if(myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.inlineLocalisationThis = isInlineThisOnly
        }
        if(myKeepTheDeclaration != null && myKeepTheDeclaration!!.isEnabled) {
            settings.inlineLocalisationKeep = isKeepTheDeclaration()
        }
    }
}