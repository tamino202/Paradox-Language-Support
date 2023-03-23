package icu.windea.pls.lang.scope

import com.intellij.openapi.progress.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.script.psi.*

class ParadoxGameRuleScopeContextProvider : ParadoxDefinitionScopeContextProvider {
    override fun getScopeContext(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo): ParadoxScopeContext? {
        ProgressManager.checkCanceled()
        if(definitionInfo.type != "game_rule") return null
        //直接使用来自game_rules.cwt的作用域信息
        val configGroup = definitionInfo.configGroup
        val config = configGroup.gameRules.get(definitionInfo.name)
        val result = config?.scopeContext
        return result
    }
}
