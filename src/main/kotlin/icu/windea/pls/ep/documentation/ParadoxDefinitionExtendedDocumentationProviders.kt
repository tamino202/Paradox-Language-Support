package icu.windea.pls.ep.documentation

import icu.windea.pls.config.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.*
import icu.windea.pls.model.*
import icu.windea.pls.model.expression.*
import icu.windea.pls.script.psi.*

class ParadoxBaseDefinitionExtendedDocumentationProvider : ParadoxDefinitionExtendedDocumentationProvider {
    override fun getDocumentation(definition: ParadoxScriptProperty, definitionInfo: ParadoxDefinitionInfo): String? {
        val definitionName = definitionInfo.name
        if(definitionName.isEmpty()) return null
        if(definitionName.isParameterized()) return null
        val configGroup = definitionInfo.configGroup
        val configs = configGroup.extendedDefinitions.findByPattern(definitionName, definition, configGroup).orEmpty()
        val config = configs.findLast { ParadoxDefinitionTypeExpression.resolve(it.type).matches(definitionInfo) } ?: return null
        val documentation = config.config.documentation?.orNull()
        return documentation
    }
}

class ParadoxGameRuleExtendedDocumentationProvider : ParadoxDefinitionExtendedDocumentationProvider {
    override fun getDocumentation(definition: ParadoxScriptProperty, definitionInfo: ParadoxDefinitionInfo): String? {
        val definitionName = definitionInfo.name
        if(definitionName.isEmpty()) return null
        if(definitionName.isParameterized()) return null
        if(definitionInfo.type != "game_rule") return null
        val configGroup = definitionInfo.configGroup
        val config = configGroup.extendedGameRules.findByPattern(definitionName, definition, configGroup) ?: return null
        val documentation = config.config.documentation?.orNull()
        return documentation
    }
}

class ParadoxOnActionExtendedDocumentationProvider : ParadoxDefinitionExtendedDocumentationProvider {
    override fun getDocumentation(definition: ParadoxScriptProperty, definitionInfo: ParadoxDefinitionInfo): String? {
        val definitionName = definitionInfo.name
        if(definitionName.isEmpty()) return null
        if(definitionName.isParameterized()) return null
        if(definitionInfo.type != "on_action") return null
        val configGroup = definitionInfo.configGroup
        val config = configGroup.extendedOnActions.findByPattern(definitionInfo.name, definition, configGroup) ?: return null
        val documentation = config.config.documentation?.orNull()
        return documentation
    }
}