package icu.windea.pls.script.references

import com.intellij.patterns.PlatformPatterns.*
import com.intellij.psi.*
import icu.windea.pls.script.psi.*

class ParadoxScriptReferenceContributor : PsiReferenceContributor() {
	override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
		registrar.registerReferenceProvider(psiElement(ParadoxScriptStringExpressionElement::class.java), ParadoxScriptExpressionElementReferenceProvider())
		registrar.registerReferenceProvider(psiElement(ParadoxScriptBlock::class.java), ParadoxScriptExpressionElementReferenceProvider())
		registrar.registerReferenceProvider(psiElement(ParadoxScriptInt::class.java), ParadoxScriptExpressionElementReferenceProvider())
		registrar.registerReferenceProvider(psiElement(ParadoxScriptString::class.java), ParadoxEventNamespaceReferenceProvider())
	}
}
