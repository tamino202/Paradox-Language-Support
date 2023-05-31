package icu.windea.pls.lang.hierarchy.impl

import icu.windea.pls.config.config.*
import icu.windea.pls.config.expression.*
import icu.windea.pls.lang.hierarchy.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.script.psi.*

class ParadoxEventInOnActionDefinitionHierarchySupport: ParadoxDefinitionHierarchySupport {
    companion object {
        const val ID = "event.in.onAction"
        
        //val containingOnActionNameKey = Key.create<String>("paradox.definition.hierarchy.event.in.event.containingOnActionName") //definitionName
    }
    
    override val id: String get() = ID
    
    override fun indexData(fileData: MutableMap<String, MutableList<ParadoxDefinitionHierarchyInfo>>, element: ParadoxScriptStringExpressionElement, config: CwtMemberConfig<*>, definitionInfo: ParadoxDefinitionInfo) {
        if(definitionInfo.type != "on_action") return
        val configExpression = config.expression
        if(configExpression.type != CwtDataType.Definition) return
        val definitionType = configExpression.value?.substringBefore('.') ?: return
        if(definitionType != "event") return
        
        //elementOffset is unused yet by this support
        val info = ParadoxDefinitionHierarchyInfo(id, element.value, config.expression, definitionInfo.name, definitionInfo.type, definitionInfo.subtypes, -1 /*element.startOffset*/, definitionInfo.gameType)
        fileData.getOrPut(id) { mutableListOf() }.add(info)
    }
}
