package icu.windea.pls.lang.search

import com.intellij.psi.*
import icu.windea.pls.core.search.*
import icu.windea.pls.lang.*
import icu.windea.pls.model.constraints.*

class ParadoxFilteredRequestResultProcessor(
    target: PsiElement,
    private val constraint: ParadoxResolveConstraint
) : FilteredRequestResultProcessor(target) {
    override fun applyFor(element: PsiElement): Boolean {
        return element.language.isParadoxLanguage()
    }

    override fun acceptElement(element: PsiElement): Boolean {
        return element.canResolveReference(constraint)
    }

    override fun acceptReference(reference: PsiReference): Boolean {
        return reference.canResolve(constraint)
    }
}
