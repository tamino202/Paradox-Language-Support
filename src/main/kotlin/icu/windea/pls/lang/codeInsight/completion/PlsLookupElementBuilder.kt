package icu.windea.pls.lang.codeInsight.completion

import com.intellij.application.options.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.ui.*
import com.intellij.util.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.config.config.*
import icu.windea.pls.core.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.script.codeStyle.*
import icu.windea.pls.script.psi.*
import javax.swing.*

class PlsLookupElementBuilder(
    val element: PsiElement?,
    val lookupString: String
) {
    var icon: Icon? = null
    var presentableText: String? = null
    var tailText: String? = null
    var typeText: String? = null
    var typeIcon: Icon? = null
    var priority: Double? = null
    
    var bold: Boolean = false
    var italic: Boolean = false
    var underlined: Boolean = false
    var strikeout: Boolean = false
    var caseSensitive: Boolean = true
    
    var scopeMatched: Boolean = true
    var forceInsertCurlyBraces: Boolean = false
    var localizedNames: Set<String> = emptySet()
    
    fun withIcon(icon: Icon?) = apply { this.icon = icon }
    fun withPresentableText(presentableText: String?) = apply { this.presentableText = presentableText }
    fun withTailText(tailText: String?) = apply { this.tailText = tailText }
    fun withTypeText(typeText: String?) = apply { this.typeText = typeText }
    fun withTypeIcon(typeIcon: Icon?) = apply { this.typeIcon = typeIcon }
    fun withPriority(priority: Double?) = apply { this.priority = priority }
    
    fun bold() = apply { this.bold = true }
    fun italic() = apply { this.italic = true }
    fun underlined() = apply { this.underlined = true }
    fun strikeout() = apply { this.strikeout = true }
    fun caseInsensitive() = apply { this.caseSensitive = false }
    
    fun withScopeMatched(scopeMatched: Boolean) = apply { this.scopeMatched = scopeMatched }
    fun withForceInsertCurlyBraces(forceInsertCurlyBraces: Boolean) = apply { this.forceInsertCurlyBraces = forceInsertCurlyBraces }
    fun withLocalizedNames(localizedNames: Set<String>) = apply { this.localizedNames = localizedNames }
    
    companion object {
        @JvmStatic
        fun create(lookupString: String): PlsLookupElementBuilder {
            return PlsLookupElementBuilder(null, lookupString)
        }
        
        @JvmStatic
        fun create(element: PsiElement?, lookupString: String): PlsLookupElementBuilder {
            return PlsLookupElementBuilder(element, lookupString)
        }
    }
}