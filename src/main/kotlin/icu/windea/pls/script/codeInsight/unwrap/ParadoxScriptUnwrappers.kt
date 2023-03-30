package icu.windea.pls.script.codeInsight.unwrap

import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.script.psi.*

object ParadoxScriptUnwrappers {
    class ParadoxScriptScriptedVariableRemover(key: String) : ParadoxScriptRemover(key) {
        override fun getName(e: PsiElement): String {
            return if(e is ParadoxScriptScriptedVariable) e.name else ""
        }
        
        override fun isApplicableTo(e: PsiElement): Boolean {
            return e is ParadoxScriptScriptedVariable
        }
    }
    
    class ParadoxScriptPropertyRemover(key: String) : ParadoxScriptRemover(key) {
        override fun getName(e: PsiElement): String {
            return if(e is ParadoxScriptProperty) e.name else ""
        }
        
        override fun isApplicableTo(e: PsiElement): Boolean {
            return e is ParadoxScriptProperty
        }
    }
    
    class ParadoxScriptValueRemover(key: String) : ParadoxScriptRemover(key) {
        override fun getName(e: PsiElement): String {
            return if(e is ParadoxScriptValue) e.name else ""
        }
        
        override fun isApplicableTo(e: PsiElement): Boolean {
            return e is ParadoxScriptValue && e.isBlockValue()
        }
    }
    
    class ParadoxScriptParameterConditionRemover(key: String) : ParadoxScriptRemover(key) {
        override fun getName(e: PsiElement): String {
            return if(e is ParadoxScriptParameterCondition) e.conditionExpression?.let { PlsConstants.parameterConditionFolder(it) }.orEmpty() else ""
        }
        
        override fun isApplicableTo(e: PsiElement): Boolean {
            return e is ParadoxScriptParameterCondition
        }
    }
}