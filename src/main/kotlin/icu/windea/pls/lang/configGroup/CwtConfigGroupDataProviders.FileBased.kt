package icu.windea.pls.lang.configGroup

import com.intellij.openapi.vfs.*
import icu.windea.pls.config.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.config.expression.*
import icu.windea.pls.config.settings.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.util.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.lang.*
import icu.windea.pls.model.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * 用于初始CWT规则分组中基于文件内容的那些数据。
 */
class FileBasedCwtConfigGroupDataProvider : CwtConfigGroupDataProvider {
    override fun process(configGroup: CwtConfigGroup): Boolean {
        //按照文件路径（相对于规则分组的根目录）正序读取所有规则文件
        //后加入的规则文件会覆盖先加入的同路径的规则文件
        //后加入的数据项会覆盖先加入的同名同类型的数据项
        
        val allFiles = mutableMapOf<String, Tuple2<VirtualFile, CwtConfigGroupFileProvider>>()
        val fileProviders = CwtConfigGroupFileProvider.EP_NAME.extensionList
        fileProviders.all f@{ fileProvider ->
            fileProvider.processFiles(configGroup) { filePath, file ->
                allFiles[filePath] = tupleOf(file, fileProvider)
                true
            }
        }
        
        allFiles.all f@{ (filePath, tuple) ->
            val (file, fileProcessor) = tuple
            processFile(filePath, file, fileProcessor, configGroup)
        }
        
        return true
    }
    
    private fun processFile(filePath: String, file: VirtualFile, fileProcessor: CwtConfigGroupFileProvider, configGroup: CwtConfigGroup): Boolean {
        val psiFile = file.toPsiFile(configGroup.project) as? CwtFile ?: return true
        val fileConfig = CwtConfigResolver.resolve(psiFile, configGroup.info)
        if(fileProcessor.isBuiltIn()) doProcessBuiltInFile(filePath, fileConfig, configGroup)
        doProcessFile(fileConfig, configGroup)
        return true
    }
    
    private fun doProcessBuiltInFile(filePath: String, fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        when(filePath) {
            "settings/folding_settings.pls.cwt" -> resolveFoldingSettingsInFile(fileConfig, configGroup)
            "settings/postfix_template_settings.pls.cwt" -> resolvePostfixTemplateSettingsInFile(fileConfig, configGroup)
            "system_links.pls.cwt" -> resolveSystemLinks(fileConfig, configGroup)
            "localisation_locales.pls.cwt" -> resolveLocalisationLocalesInFile(fileConfig, configGroup)
            "localisation_predefined_parameters.pls.cwt" -> resolveLocalisationPredefinedParametersInFile(fileConfig, configGroup)
        }
    }
    
    private fun resolveFoldingSettingsInFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        val configs = fileConfig.properties
        configs.forEach { groupProperty ->
            val groupName = groupProperty.key
            val map = caseInsensitiveStringKeyMap<CwtFoldingSettings>()
            groupProperty.properties?.forEach { property ->
                val id = property.key
                var key: String? = null
                var keys: List<String>? = null
                var placeholder: String? = null
                property.properties?.forEach { prop ->
                    when {
                        prop.key == "key" -> key = prop.stringValue
                        prop.key == "keys" -> keys = prop.values?.mapNotNull { it.stringValue }
                        prop.key == "placeholder" -> placeholder = prop.stringValue
                    }
                }
                if(placeholder != null) {
                    val foldingSetting = CwtFoldingSettings(id, key, keys, placeholder!!)
                    map.put(id, foldingSetting)
                }
            }
            configGroup.foldingSettings.asMutable()[groupName] = map
        }
    }
    
    private fun resolvePostfixTemplateSettingsInFile(fileConfig: CwtFileConfig,configGroup: CwtConfigGroup ) {
        val configs = fileConfig.properties
        configs.forEach { groupProperty ->
            val groupName = groupProperty.key
            val map = caseInsensitiveStringKeyMap<CwtPostfixTemplateSettings>()
            groupProperty.properties?.forEach { property ->
                val id = property.key
                var key: String? = null
                var example: String? = null
                var variables: Map<String, String>? = null
                var expression: String? = null
                property.properties?.forEach { prop ->
                    when {
                        prop.key == "key" -> key = prop.stringValue
                        prop.key == "example" -> example = prop.stringValue
                        prop.key == "variables" -> variables = prop.properties?.let {
                            buildMap {
                                it.forEach { p -> put(p.key, p.value) }
                            }
                        }
                        prop.key == "expression" -> expression = prop.stringValue
                    }
                }
                if(key != null && expression != null) {
                    val foldingSetting = CwtPostfixTemplateSettings(id, key!!, example, variables.orEmpty(), expression!!)
                    map.put(id, foldingSetting)
                }
            }
            configGroup.postfixTemplateSettings.asMutable()[groupName] = map
        }
    }
    
    private fun resolveSystemLinks(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        val configs = fileConfig.properties.find { it.key == "system_links" }?.properties ?: return
        configs.forEach { property ->
            val id = property.key
            val baseId = property.properties?.find { p -> p.key == "base_id" }?.stringValue ?: id
            val description = property.documentation.orEmpty()
            val name = property.stringValue ?: id
            val config = CwtSystemLinkConfig(property.pointer, fileConfig.info, id, baseId, description, name)
            configGroup.systemLinks.asMutable()[id] = config
        }
    }
    
    private fun resolveLocalisationLocalesInFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        val configs = fileConfig.properties.find { it.key == "localisation_locales" }?.properties ?: return
        configs.forEach { property ->
            val id = property.key
            val description = property.documentation.orEmpty()
            val codes = property.properties?.find { p -> p.key == "codes" }?.values?.mapNotNull { v -> v.stringValue }.orEmpty()
            val config = CwtLocalisationLocaleConfig(property.pointer, fileConfig.info, id, description, codes)
            configGroup.localisationLocalesById.asMutable()[id] = config
            codes.forEach { code -> configGroup.localisationLocalesByCode.asMutable()[code] = config }
        }
    }
    
    private fun resolveLocalisationPredefinedParametersInFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        val configs = fileConfig.properties.find { it.key == "localisation_predefined_parameters" }?.properties ?: return
        configs.forEach { property ->
            val id = property.key
            val mockValue = property.value
            val description = property.documentation.orEmpty()
            val config = CwtLocalisationPredefinedParameterConfig(property.pointer, fileConfig.info, id, mockValue, description)
            configGroup.localisationPredefinedParameters.asMutable()[id] = config
        }
    }
    
    private fun doProcessFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        when(fileConfig.key) {
            //解析要将其中的文件识别为脚本文件的目录列表 - 仅作记录，插件目前并不这个目录列表来判断是否要将文件识别为脚本文件
            "folders" -> resolveFoldersInFile(fileConfig, configGroup)
            //对于其他情况，不限制文件名，统一处理
            else -> resolveOthersInFile(fileConfig, configGroup)
        }
    }
    
    private fun resolveFoldersInFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        fileConfig.values.mapTo(configGroup.folders.asMutable()) { it.value }
    }
    
    private fun resolveOthersInFile(fileConfig: CwtFileConfig, configGroup: CwtConfigGroup) {
        for(property in fileConfig.properties) {
            val key = property.key
            when {
                //找到配置文件中的顶级的key为"types"的属性，然后解析它的子属性，添加到types中
                key == "types" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val typeName = prop.key.removeSurroundingOrNull("type[", "]")
                        if(!typeName.isNullOrEmpty()) {
                            val typeConfig = resolveTypeConfig(prop, typeName)
                            configGroup.types.asMutable()[typeName] = typeConfig
                        }
                    }
                }
                //找到配置文件中的顶级的key为"enums"的属性，然后解析它的子属性，添加到enums和complexEnums中
                key == "enums" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        //TODO enumName may be a template expression (e.g. xxx_<xxx>)
                        val enumName = prop.key.removeSurroundingOrNull("enum[", "]")
                        if(!enumName.isNullOrEmpty()) {
                            val enumConfig = resolveEnumConfig(prop, enumName) ?: continue
                            configGroup.enums.asMutable()[enumName] = enumConfig
                        }
                        val complexEnumName = prop.key.removeSurroundingOrNull("complex_enum[", "]")
                        if(!complexEnumName.isNullOrEmpty()) {
                            val complexEnumConfig = resolveComplexEnumConfig(prop, complexEnumName) ?: continue
                            configGroup.complexEnums.asMutable()[complexEnumName] = complexEnumConfig
                        }
                    }
                }
                key == "links" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val linkName = prop.key
                        val linkConfig = resolveLinkConfig(prop, linkName) ?: continue
                        configGroup.links.asMutable()[linkName] = linkConfig
                        //要求data_source存在
                        val fromData = linkConfig.fromData && linkConfig.dataSource != null
                        val withPrefix = linkConfig.prefix != null
                        val type = linkConfig.type
                        if(type == null || type == "scope" || type == "both") {
                            when {
                                !fromData -> configGroup.linksAsScopeNotData.asMutable()[linkName] = linkConfig
                                withPrefix -> configGroup.linksAsScopeWithPrefix.asMutable()[linkName] = linkConfig
                                else -> configGroup.linksAsScopeWithoutPrefix.asMutable()[linkName] = linkConfig
                            }
                        }
                        if(type == "value" || type == "both") {
                            when {
                                !fromData -> configGroup.linksAsValueNotData.asMutable()[linkName] = linkConfig
                                withPrefix -> configGroup.linksAsValueWithPrefix.asMutable()[linkName] = linkConfig
                                else -> configGroup.linksAsValueWithoutPrefix.asMutable()[linkName] = linkConfig
                            }
                        }
                    }
                }
                key == "localisation_links" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val linkName = prop.key
                        val linkConfig = resolveLocalisationLinkConfig(prop, linkName) ?: continue
                        configGroup.localisationLinks.asMutable()[linkName] = linkConfig
                    }
                }
                key == "localisation_commands" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val commandName = prop.key
                        val commandConfig = resolveLocalisationCommandConfig(prop, commandName)
                        configGroup.localisationCommands.asMutable()[commandName] = commandConfig
                    }
                }
                key == "modifier_categories" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val modifierCategoryName = prop.key
                        val categoryConfig = resolveModifierCategoryConfig(prop, modifierCategoryName) ?: continue
                        configGroup.modifierCategories.asMutable()[modifierCategoryName] = categoryConfig
                    }
                }
                key == "modifiers" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val modifierName = prop.key
                        val modifierConfig = resolveModifierConfig(prop, modifierName) ?: continue
                        configGroup.modifiers.asMutable()[modifierName] = modifierConfig
                        for(snippetExpression in modifierConfig.template.snippetExpressions) {
                            if(snippetExpression.type == CwtDataTypes.Definition) {
                                val typeExpression = snippetExpression.value ?: continue
                                configGroup.type2ModifiersMap.asMutable().getOrPut(typeExpression) { mutableMapOf() }.asMutable()[modifierName] = modifierConfig
                            }
                        }
                    }
                }
                key == "scopes" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val scopeName = prop.key
                        val scopeConfig = resolveScopeConfig(prop, scopeName) ?: continue
                        configGroup.scopes.asMutable()[scopeName] = scopeConfig
                        for(alias in scopeConfig.aliases) {
                            configGroup.scopeAliasMap.asMutable()[alias] = scopeConfig
                        }
                    }
                }
                key == "scope_groups" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        val scopeGroupName = prop.key
                        val scopeGroupConfig = resolveScopeGroupConfig(prop, scopeGroupName) ?: continue
                        configGroup.scopeGroups.asMutable()[scopeGroupName] = scopeGroupConfig
                    }
                }
                key == "definitions" -> {
                    val configs = property.configs ?: continue
                    for(config in configs) {
                        val definitionConfig = resolveDefinitionConfig(config) ?: continue
                        configGroup.definitions.asMutable().getOrPut(definitionConfig.name) { mutableListOf() }.asMutable() += definitionConfig
                    }
                }
                key == "game_rules" -> {
                    val configs = property.configs ?: continue
                    for(config in configs) {
                        val gameRuleConfig = resolveGameRuleConfig(config)
                        configGroup.gameRules.asMutable()[gameRuleConfig.name] = gameRuleConfig
                    }
                }
                key == "on_actions" -> {
                    val configs = property.configs ?: continue
                    for(config in configs) {
                        val onActionConfig = resolveOnActionConfig(config) ?: continue
                        configGroup.onActions.asMutable()[onActionConfig.name] = onActionConfig
                    }
                }
                key == "values" -> {
                    val props = property.properties ?: continue
                    for(prop in props) {
                        //TODO valueName may be a template expression (e.g. xxx_<xxx>)
                        val dynamicValueName = prop.key.removeSurroundingOrNull("value[", "]")
                        if(!dynamicValueName.isNullOrEmpty()) {
                            val valueConfig = resolveDynamicValueConfig(prop, dynamicValueName) ?: continue
                            configGroup.dynamicValues.asMutable()[dynamicValueName] = valueConfig
                        }
                    }
                }
                else -> {
                    //判断配置文件中的顶级的key是否匹配"single_alias[?]"，如果匹配，则解析配置并添加到singleAliases中
                    val singleAliasName = key.removeSurroundingOrNull("single_alias[", "]")
                    if(singleAliasName != null) {
                        val singleAliasConfig = resolveSingleAliasConfig(property, singleAliasName)
                        configGroup.singleAliases.asMutable()[singleAliasName] = singleAliasConfig
                        continue
                    }
                    
                    //判断配置文件中的顶级的key是否匹配"alias[?:?]"，如果匹配，则解析配置并添加到aliasGroups中
                    val aliasTokens = key.removeSurroundingOrNull("alias[", "]")?.split(':', limit = 2)?.takeIf { it.size == 2 }
                    if(aliasTokens != null) {
                        val (aliasName, aliasSubName) = aliasTokens
                        val aliasConfig = resolveAliasConfig(property, aliasName, aliasSubName)
                        //目前不这样处理
                        //if(aliasConfig.name == "modifier" && aliasConfig.expression.type.isConstantLikeType()) {
                        //	val modifierConfig = resolveModifierConfigFromAliasConfig(aliasConfig)
                        //	modifiers.asMutable()[modifierConfig.name] = modifierConfig
                        //	continue
                        //} 
                        val map = configGroup.aliasGroups.asMutable().getOrPut(aliasName) { mutableMapOf() }
                        val list = map.asMutable().getOrPut(aliasSubName) { mutableListOf() }.asMutable()
                        list.add(aliasConfig)
                        continue
                    }
                    
                    //判断配置文件中的顶级的key是否匹配"inline[?]"，如果匹配，则解析配置并添加到inlineConfigGroup中
                    val inlineConfigName = key.removeSurroundingOrNull("inline[", "]")
                    if(inlineConfigName != null) {
                        val inlineConfig = resolveInlineConfig(property, inlineConfigName)
                        val list = configGroup.inlineConfigGroup.asMutable().getOrPut(inlineConfigName) { mutableListOf() }.asMutable()
                        list.add(inlineConfig)
                        continue
                    }
                    
                    //其他情况，放到declarations中
                    val declarationConfig = resolveDeclarationConfig(property, key)
                    configGroup.declarations.asMutable()[key] = declarationConfig
                }
            }
        }
    }
    
    private fun resolveTypeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtTypeConfig {
        val configGroup = propertyConfig.info.configGroup
        
        var baseType: String? = null
        var path: String? = null
        var pathStrict = false
        var pathFile: String? = null
        var pathExtension: String? = null
        var nameField: String? = null
        var nameFromFile = false
        var typePerFile = false
        var unique = false
        var severity: String? = null
        var skipRootKey: MutableList<List<String>>? = null
        var typeKeyFilter: ReversibleValue<Set<String>>? = null
        var typeKeyRegex: Regex? = null
        var startsWith: String? = null
        var graphRelatedTypes: Set<String>? = null
        val subtypes: MutableMap<String, CwtSubtypeConfig> = mutableMapOf()
        var localisation: CwtTypeLocalisationConfig? = null
        var images: CwtTypeImagesConfig? = null
        
        val props = propertyConfig.properties
        if(!props.isNullOrEmpty()) {
            for(prop in props) {
                val key = prop.key
                when(key) {
                    "base_type" -> baseType = prop.stringValue
                    //这里的path一般以"game/"开始，需要去除
                    "path" -> path = prop.stringValue?.removePrefix("game")?.trim('/') ?: continue
                    "path_strict" -> pathStrict = prop.booleanValue ?: continue
                    "path_file" -> pathFile = prop.stringValue ?: continue
                    //这里的path_extension一般以"."开始，需要去除
                    "path_extension" -> pathExtension = prop.stringValue?.removePrefix(".") ?: continue
                    "name_field" -> nameField = prop.stringValue ?: continue
                    "name_from_file" -> nameFromFile = prop.booleanValue ?: continue
                    "type_per_file" -> typePerFile = prop.booleanValue ?: continue
                    "unique" -> unique = prop.booleanValue ?: continue
                    "severity" -> severity = prop.stringValue ?: continue
                    "skip_root_key" -> {
                        //值可能是string也可能是stringArray
                        val list = prop.stringValue?.let { listOf(it) }
                            ?: prop.values?.mapNotNull { it.stringValue }
                            ?: continue
                        if(skipRootKey == null) skipRootKey = mutableListOf()
                        skipRootKey.add(list) //出于一点点的性能考虑，这里保留大小写，后面匹配路径时会忽略掉
                    }
                    "localisation" -> {
                        val configs: MutableList<Pair<String?, CwtLocationConfig>> = mutableListOf()
                        val propPointer = prop.pointer
                        val propProps = prop.properties ?: continue
                        for(p in propProps) {
                            val subtypeName = p.key.removeSurroundingOrNull("subtype[", "]")
                            if(subtypeName != null) {
                                val pps = p.properties ?: continue
                                for(pp in pps) {
                                    val locationConfig = resolveLocationConfig(pp, pp.key) ?: continue
                                    configs.add(subtypeName to locationConfig)
                                }
                            } else {
                                val locationConfig = resolveLocationConfig(p, p.key) ?: continue
                                configs.add(null to locationConfig)
                            }
                        }
                        localisation = CwtTypeLocalisationConfig(propPointer, propertyConfig.info, configs)
                    }
                    "images" -> {
                        val configs: MutableList<Pair<String?, CwtLocationConfig>> = mutableListOf()
                        val propPointer = prop.pointer
                        val propProps = prop.properties ?: continue
                        for(p in propProps) {
                            val subtypeName = p.key.removeSurroundingOrNull("subtype[", "]")
                            if(subtypeName != null) {
                                val pps = p.properties ?: continue
                                for(pp in pps) {
                                    val locationConfig = resolveLocationConfig(pp, pp.key) ?: continue
                                    configs.add(subtypeName to locationConfig)
                                }
                            } else {
                                val locationConfig = resolveLocationConfig(p, p.key) ?: continue
                                configs.add(null to locationConfig)
                            }
                        }
                        images = CwtTypeImagesConfig(propPointer, propertyConfig.info, configs)
                    }
                    "modifiers" -> {
                        val propProps = prop.properties ?: continue
                        for(p in propProps) {
                            val subtypeName = p.key.removeSurroundingOrNull("subtype[", "]")
                            if(subtypeName != null) {
                                val pps = p.properties ?: continue
                                for(pp in pps) {
                                    val typeExpression = "$name.$subtypeName"
                                    val modifierConfig = resolveDefinitionModifierConfig(pp, pp.key, typeExpression) ?: continue
                                    configGroup.modifiers.asMutable()[modifierConfig.name] = modifierConfig
                                    configGroup.type2ModifiersMap.asMutable().getOrPut(typeExpression) { mutableMapOf() }.asMutable()[pp.key] = modifierConfig
                                }
                            } else {
                                val typeExpression = name
                                val modifierConfig = resolveDefinitionModifierConfig(p, p.key, typeExpression) ?: continue
                                configGroup.modifiers.asMutable()[modifierConfig.name] = modifierConfig
                                configGroup.type2ModifiersMap.asMutable().getOrPut(typeExpression) { mutableMapOf() }.asMutable()[p.key] = modifierConfig
                            }
                        }
                    }
                }
                
                val subtypeName = key.removeSurroundingOrNull("subtype[", "]")
                if(subtypeName != null) {
                    val subtypeConfig = resolveSubtypeConfig(prop, subtypeName)
                    subtypes[subtypeName] = subtypeConfig
                }
            }
        }
        
        val options = propertyConfig.options
        if(!options.isNullOrEmpty()) {
            for(option in options) {
                if(option !is CwtOptionConfig) continue
                val key = option.key
                when(key) {
                    "type_key_filter" -> {
                        //值可能是string也可能是stringArray
                        val values = option.getOptionValueOrValues() ?: continue
                        val set = caseInsensitiveStringSet() //忽略大小写
                        set.addAll(values)
                        val o = option.separatorType == CwtSeparatorType.EQUAL
                        typeKeyFilter = set reverseIf o
                    }
                    "type_key_regex" -> {
                        typeKeyRegex = option.stringValue?.toRegex(RegexOption.IGNORE_CASE)
                    }
                    "starts_with" -> startsWith = option.stringValue ?: continue //忽略大小写
                    "graph_related_types" -> {
                        graphRelatedTypes = option.getOptionValues()
                    }
                }
            }
        }
        
        return CwtTypeConfig(
            propertyConfig.pointer, propertyConfig.info, propertyConfig,
            name, baseType,
            path, pathStrict, pathFile, pathExtension,
            nameField, nameFromFile, typePerFile, unique, severity, skipRootKey,
            typeKeyFilter, typeKeyRegex, startsWith,
            graphRelatedTypes, subtypes,
            localisation, images
        )
    }
    
    private fun resolveSubtypeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtSubtypeConfig {
        var typeKeyFilter: ReversibleValue<Set<String>>? = null
        var typeKeyRegex: Regex? = null
        var pushScope: String? = null
        var startsWith: String? = null
        var displayName: String? = null
        var abbreviation: String? = null
        var onlyIfNot: Set<String>? = null
        
        val options = propertyConfig.options
        if(!options.isNullOrEmpty()) {
            for(option in options) {
                if(option !is CwtOptionConfig) continue
                val key = option.key
                when(key) {
                    "type_key_filter" -> {
                        //值可能是string也可能是stringArray
                        val values = option.getOptionValueOrValues()
                        if(values == null) continue
                        val set = caseInsensitiveStringSet() //忽略大小写
                        set.addAll(values)
                        val o = option.separatorType == CwtSeparatorType.EQUAL
                        typeKeyFilter = set reverseIf o
                    }
                    "type_key_regex" -> {
                        typeKeyRegex = option.stringValue?.toRegex(RegexOption.IGNORE_CASE)
                    }
                    "starts_with" -> startsWith = option.stringValue ?: continue //忽略大小写
                    "push_scope" -> pushScope = option.stringValue ?: continue
                    "display_name" -> displayName = option.stringValue ?: continue
                    "abbreviation" -> abbreviation = option.stringValue ?: continue
                    "only_if_not" -> onlyIfNot = option.getOptionValueOrValues() ?: continue
                }
            }
        }
        return CwtSubtypeConfig(
            propertyConfig.pointer, propertyConfig.info, propertyConfig,
            name, typeKeyFilter, typeKeyRegex, startsWith,
            pushScope, displayName, abbreviation, onlyIfNot
        )
    }
    
    private fun resolveLocationConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLocationConfig? {
        //default to optional
        //default to primary for name and title if it represents a localisation location (by inference)
        //default to primary for icon if it represents an image location (by inference)
        val expression = propertyConfig.stringValue ?: return null
        val required = propertyConfig.findOptionValue("required") != null
        val optional = propertyConfig.findOptionValue("optional") != null
        val primary = propertyConfig.findOptionValue("primary") != null
        return CwtLocationConfig(propertyConfig.pointer, propertyConfig.info, name, expression, required || !optional, primary)
    }
    
    private fun resolveDefinitionModifierConfig(propertyConfig: CwtPropertyConfig, name: String, typeExpression: String): CwtModifierConfig? {
        //string | string[]
        val modifierName = name.replace("$", "<$typeExpression>")
        val categories = propertyConfig.stringValue?.let { setOf(it) }
            ?: propertyConfig.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
            ?: return null
        return CwtModifierConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, modifierName, categories)
    }
    
    private fun resolveEnumConfig(propertyConfig: CwtPropertyConfig, name: String): CwtEnumConfig? {
        val pointer = propertyConfig.pointer
        val info = propertyConfig.info
        val propertyConfigValues = propertyConfig.values ?: return null
        if(propertyConfigValues.isEmpty()) {
            return CwtEnumConfig(pointer, info, name, emptySet(), emptyMap())
        }
        val values = caseInsensitiveStringSet() //忽略大小写
        val valueConfigMap = caseInsensitiveStringKeyMap<CwtValueConfig>() //忽略大小写
        for(propertyConfigValue in propertyConfigValues) {
            values.add(propertyConfigValue.value)
            valueConfigMap.put(propertyConfigValue.value, propertyConfigValue)
        }
        return CwtEnumConfig(pointer, info, name, values, valueConfigMap)
    }
    
    private fun resolveComplexEnumConfig(propertyConfig: CwtPropertyConfig, name: String): CwtComplexEnumConfig? {
        val pointer = propertyConfig.pointer
        val info = propertyConfig.info
        val props = propertyConfig.properties ?: return null
        if(props.isEmpty()) return null //invalid
        val path: MutableSet<String> = mutableSetOf()
        var pathFile: String? = null
        var pathStrict = false
        var startFromRoot = false
        var nameConfig: CwtPropertyConfig? = null
        for(prop in props) {
            when(prop.key) {
                //这里的path一般"game/"开始，这里需要忽略
                "path" -> prop.stringValue?.removePrefix("game")?.trim('/')?.let { path.add(it) }
                "path_file" -> pathFile = prop.stringValue
                "path_strict" -> pathStrict = prop.booleanValue ?: false
                "start_from_root" -> startFromRoot = prop.booleanValue ?: false
                "name" -> nameConfig = prop
            }
        }
        val searchScopeType = propertyConfig.findOption("search_scope_type")?.stringValue
        if(path.isEmpty() || nameConfig == null) return null //invalid
        return CwtComplexEnumConfig(pointer, info, name, path, pathFile, pathStrict, startFromRoot, searchScopeType, nameConfig)
    }
    
    private fun resolveLinkConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLinkConfig? {
        var desc: String? = null
        var fromData = false
        var type: String? = null
        var dataSource: CwtValueExpression? = null
        var prefix: String? = null
        var inputScopes: Set<String>? = null
        var outputScope: String? = null
        var forDefinitionType: String? = null
        val props = propertyConfig.properties ?: return null
        for(prop in props) {
            when(prop.key) {
                "desc" -> desc = prop.stringValue?.trim() //去除首尾空白
                "from_data" -> fromData = prop.booleanValue ?: false
                "type" -> type = prop.stringValue
                "data_source" -> dataSource = prop.valueExpression
                "prefix" -> prefix = prop.stringValue
                "for_definition_type" -> forDefinitionType = prop.stringValue
                "input_scopes" -> inputScopes = buildSet {
                    prop.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) }
                    prop.values?.forEach { it.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) } }
                }
                "output_scope" -> outputScope = prop.stringValue?.let { v -> ParadoxScopeHandler.getScopeId(v) }
            }
        }
        inputScopes = inputScopes.orNull() ?: ParadoxScopeHandler.anyScopeIdSet
        return CwtLinkConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name, desc, fromData, type, dataSource, prefix, forDefinitionType, inputScopes, outputScope)
    }
    
    private fun resolveLocalisationLinkConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLocalisationLinkConfig? {
        var desc: String? = null
        var inputScopes: Set<String>? = null
        var outputScope: String? = null
        val props = propertyConfig.properties ?: return null
        for(prop in props) {
            when(prop.key) {
                "desc" -> desc = prop.stringValue?.trim() //排除占位码 & 去除首尾空白
                "input_scopes" -> inputScopes = buildSet {
                    prop.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) }
                    prop.values?.forEach { it.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) } }
                }
                "output_scope" -> outputScope = prop.stringValue?.let { v -> ParadoxScopeHandler.getScopeId(v) }
            }
        }
        inputScopes = inputScopes.orNull() ?: ParadoxScopeHandler.anyScopeIdSet
        return CwtLocalisationLinkConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name, desc, inputScopes, outputScope)
    }
    
    private fun resolveLocalisationCommandConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLocalisationCommandConfig {
        val supportedScopes = buildSet {
            propertyConfig.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) }
            propertyConfig.values?.forEach { it.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) } }
        }.ifEmpty { ParadoxScopeHandler.anyScopeIdSet }
        return CwtLocalisationCommandConfig(propertyConfig.pointer, propertyConfig.info, name, supportedScopes)
    }
    
    private fun resolveModifierCategoryConfig(propertyConfig: CwtPropertyConfig, name: String): CwtModifierCategoryConfig? {
        var supportedScopes: Set<String>? = null
        val props = propertyConfig.properties
        if(props.isNullOrEmpty()) return null
        for(prop in props) {
            when(prop.key) {
                "supported_scopes" -> supportedScopes = buildSet {
                    prop.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) }
                    prop.values?.forEach { it.stringValue?.let { v -> add(ParadoxScopeHandler.getScopeId(v)) } }
                } //may be empty here (e.g. "AI Economy")
            }
        }
        supportedScopes = supportedScopes ?: ParadoxScopeHandler.anyScopeIdSet
        return CwtModifierCategoryConfig(propertyConfig.pointer, propertyConfig.info, name, supportedScopes)
    }
    
    private fun resolveModifierConfig(propertyConfig: CwtPropertyConfig, name: String): CwtModifierConfig? {
        //string | string[]
        val categories = propertyConfig.stringValue?.let { setOf(it) }
            ?: propertyConfig.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
            ?: return null
        return CwtModifierConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name, categories)
    }
    
    private fun resolveScopeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtScopeConfig? {
        var aliases: Set<String>? = null
        val props = propertyConfig.properties
        if(props.isNullOrEmpty()) return null
        for(prop in props) {
            if(prop.key == "aliases") aliases = prop.values?.mapNotNullTo(caseInsensitiveStringSet()) { it.stringValue }
        }
        if(aliases == null) aliases = emptySet()
        return CwtScopeConfig(propertyConfig.pointer, propertyConfig.info, name, aliases)
    }
    
    private fun resolveScopeGroupConfig(propertyConfig: CwtPropertyConfig, name: String): CwtScopeGroupConfig? {
        val pointer = propertyConfig.pointer
        val info = propertyConfig.info
        val propertyConfigValues = propertyConfig.values ?: return null
        if(propertyConfigValues.isEmpty()) return CwtScopeGroupConfig(pointer, info, name, emptySet(), emptyMap())
        val values = caseInsensitiveStringSet() //忽略大小写
        val valueConfigMap = caseInsensitiveStringKeyMap<CwtValueConfig>() //忽略大小写
        for(propertyConfigValue in propertyConfigValues) {
            values.add(propertyConfigValue.value)
            valueConfigMap.put(propertyConfigValue.value, propertyConfigValue)
        }
        return CwtScopeGroupConfig(pointer, info, name, values, valueConfigMap)
    }
    
    private fun resolveDefinitionConfig(config: CwtMemberConfig<*>): CwtDefinitionConfig? {
        val name = when(config) {
            is CwtPropertyConfig -> config.key
            is CwtValueConfig -> config.value
        }
        val type = config.findOption("type")?.stringValue ?: return null
        return CwtDefinitionConfig(config.pointer, config.info, config, name, type)
    }
    
    private fun resolveGameRuleConfig(config: CwtMemberConfig<*>): CwtGameRuleConfig {
        val name = when(config) {
            is CwtPropertyConfig -> config.key
            is CwtValueConfig -> config.value
        }
        return CwtGameRuleConfig(config.pointer, config.info, config, name)
    }
    
    private fun resolveOnActionConfig(config: CwtMemberConfig<*>): CwtOnActionConfig? {
        val name = when(config) {
            is CwtPropertyConfig -> config.key
            is CwtValueConfig -> config.value
        }
        val eventType = config.findOption("event_type")?.stringValue ?: return null
        return CwtOnActionConfig(config.pointer, config.info, config, name, eventType)
    }
    
    private fun resolveDynamicValueConfig(propertyConfig: CwtPropertyConfig, name: String): CwtDynamicValueConfig? {
        val pointer = propertyConfig.pointer
        val info = propertyConfig.info
        val propertyConfigValues = propertyConfig.values ?: return null
        if(propertyConfigValues.isEmpty()) {
            return CwtDynamicValueConfig(pointer, info, name, emptySet(), emptyMap())
        }
        val values = caseInsensitiveStringSet() //忽略大小写
        val valueConfigMap = caseInsensitiveStringKeyMap<CwtValueConfig>() //忽略大小写
        for(propertyConfigValue in propertyConfigValues) {
            values.add(propertyConfigValue.value)
            valueConfigMap.put(propertyConfigValue.value, propertyConfigValue)
        }
        return CwtDynamicValueConfig(pointer, info, name, values, valueConfigMap)
    }
    
    private fun resolveSingleAliasConfig(propertyConfig: CwtPropertyConfig, name: String): CwtSingleAliasConfig {
        return CwtSingleAliasConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name)
    }
    
    private fun resolveAliasConfig(propertyConfig: CwtPropertyConfig, name: String, subName: String): CwtAliasConfig {
        return CwtAliasConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name, subName)
            .apply {
                info.acceptConfigExpression(subNameExpression, null)
            }
    }
    
    private fun resolveModifierConfigFromAliasConfig(aliasConfig: CwtAliasConfig): CwtModifierConfig {
        val propertyConfig = aliasConfig.config
        return CwtModifierConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, aliasConfig.subName)
    }
    
    private fun resolveInlineConfig(propertyConfig: CwtPropertyConfig, name: String): CwtInlineConfig {
        return CwtInlineConfig(propertyConfig.pointer, propertyConfig.info, propertyConfig, name)
    }
    
    private fun resolveDeclarationConfig(propertyConfig: CwtPropertyConfig, name: String): CwtDeclarationConfig {
        return CwtDeclarationConfig(propertyConfig.pointer, propertyConfig.info, name, propertyConfig)
    }
}
