package icu.windea.pls.lang.config

import com.intellij.openapi.extensions.*
import com.intellij.psi.*
import icu.windea.pls.lang.ParadoxConfigMatcher.Options
import icu.windea.pls.lang.cwt.config.*

/**
 * 用于基于通用的逻辑获取脚本表达式所在的CWT规则上下文。
 *
 * @see ParadoxConfigContext
 */
interface ParadoxConfigContextProvider {
    fun getConfigContext(contextElement: PsiElement, file: PsiFile): ParadoxConfigContext?
    
    fun getConfigs(contextElement: PsiElement, configContext: ParadoxConfigContext, matchOptions: Int = Options.Default): List<CwtMemberConfig<*>>?
    
    companion object INSTANCE {
        val EP_NAME = ExtensionPointName.create<ParadoxConfigContextProvider>("icu.windea.pls.configContextProvider")
        
        fun getContext(contextElement: PsiElement, file: PsiFile): ParadoxConfigContext? {
            return EP_NAME.extensionList.firstNotNullOfOrNull f@{ ep ->
                ep.getConfigContext(contextElement, file)
                    ?.also { it.provider = ep }
            }
        }
    }
}
