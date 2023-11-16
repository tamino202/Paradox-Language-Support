package icu.windea.pls.inject.injectors

import com.intellij.openapi.util.text.*
import com.intellij.util.text.*
import icu.windea.pls.inject.*
import icu.windea.pls.inject.annotations.*

/**
 * 重写IDE底层的检测字符串是否是标识符的代码逻辑，从而可以正确地查找特定类型的引用。
 */
@InjectTarget("com.intellij.psi.impl.search.LowLevelSearchUtil")
class LowLevelSearchUtilCodeInjector : CodeInjectorBase() {
    //com.intellij.psi.impl.search.LowLevelSearchUtil
    //com.intellij.psi.impl.search.LowLevelSearchUtil.checkJavaIdentifier
    
    //rewrite this method to compatible with:
    //localisation icon references (e.g. "£unity£")
    
    @InjectMethod(static = true)
    fun checkJavaIdentifier(text: CharSequence, searcher: StringSearcher, index: Int): Boolean {
        if(!searcher.isJavaIdentifier) {
            return true
        }
        if(index > 0) {
            val c = text[index - 1]
            if(c != '£') { //'£'
                if(Character.isJavaIdentifierPart(c) && c != '$') {
                    if(!searcher.isHandleEscapeSequences || index < 2 || StringUtil.isEscapedBackslash(text, 0, index - 2)) { //escape sequence
                        return false
                    }
                } else if(searcher.isHandleEscapeSequences && !StringUtil.isEscapedBackslash(text, 0, index - 1)) {
                    return false
                }
            }
        }
        val patternLength = searcher.pattern.length
        if(index + patternLength < text.length) {
            val c = text[index + patternLength]
            if(c != '£') { //'£'
                return !Character.isJavaIdentifierPart(c) || c == '$'
            }
        }
        return true
    }
}