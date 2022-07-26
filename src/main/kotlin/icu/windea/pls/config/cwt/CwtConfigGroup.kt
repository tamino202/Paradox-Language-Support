package icu.windea.pls.config.cwt

import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.util.*
import com.intellij.util.containers.*
import icu.windea.pls.*
import icu.windea.pls.annotations.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.model.*
import icu.windea.pls.script.psi.*

class CwtConfigGroup(
	val gameType: ParadoxGameType,
	val project: Project,
	cwtFileConfigs: Map<String, CwtFileConfig>
) {
	val folders: Set<String>
	val types: Map<String, CwtTypeConfig>
	val values: Map<String, CwtEnumConfig>
	
	//enumValue可以是int、float、bool类型，统一用字符串表示
	val enums: Map<String, CwtEnumConfig>
	
	//since: stellaris v3.4
	val tags: Map<@CaseInsensitive String, CwtTagConfig> //tagName - tagConfig
	
	val links: Map<@CaseInsensitive String, CwtLinkConfig>
	val localisationLinks: Map<@CaseInsensitive String, CwtLinkConfig>
	val localisationCommands: Map<String, CwtLocalisationCommandConfig>
	val modifierCategories: Map<String, CwtModifierCategoryConfig>
	val modifiers: Map<String, CwtModifierConfig>
	val scopes: Map<String, CwtScopeConfig>
	val scopeAliasMap: Map<String, CwtScopeConfig>
	val scopeGroups: Map<String, CwtScopeGroupConfig>
	
	//同名的single_alias可以有多个
	val singleAliases: Map<String, List<CwtSingleAliasConfig>>
	
	//同名的alias可以有多个
	val aliases: Map<String, Map<@CaseInsensitive String, List<CwtAliasConfig>>>
	val declarations: Map<String, CwtDeclarationConfig>
	
	//目前版本的CWT配置已经不再使用
	val modifierCategoryIdMap: Map<String, CwtModifierCategoryConfig>
	
	//since: stellaris v3.4
	val tagMap: Map<String, Map<@CaseInsensitive String, CwtTagConfig>> //definitionType - tagName - tagConfig
	
	//支持参数的定义类型
	val definitionTypesSupportParameters: Set<String>
	
	init {
		folders = mutableSetOf()
		types = mutableMapOf()
		values = mutableMapOf()
		enums = mutableMapOf()
		tags = CollectionFactory.createCaseInsensitiveStringMap()
		links = CollectionFactory.createCaseInsensitiveStringMap()
		localisationLinks = CollectionFactory.createCaseInsensitiveStringMap()
		localisationCommands = mutableMapOf()
		modifierCategories = mutableMapOf()
		modifiers = mutableMapOf()
		scopes = mutableMapOf()
		scopeAliasMap = mutableMapOf()
		scopeGroups = mutableMapOf()
		singleAliases = mutableMapOf<String, MutableList<CwtSingleAliasConfig>>()
		aliases = mutableMapOf<String, MutableMap<String, MutableList<CwtAliasConfig>>>()
		declarations = mutableMapOf()
		
		//目前不检查配置文件的位置和文件名
		
		for((filePath, fileConfig) in cwtFileConfigs) {
			//如果存在folders.cwt，则将其中的相对路径列表添加到folders中 
			if(filePath == "folders.cwt") {
				resolveFoldersCwt(fileConfig, folders)
			}
			
			//处理fileConfig的properties
			for(property in fileConfig.properties) {
				val key = property.key
				when(key) {
					//找到配置文件中的顶级的key为"types"的属性，然后解析它的子属性，添加到types中
					"types" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val typeName = prop.key.removeSurroundingOrNull("type[", "]")
							if(typeName != null && typeName.isNotEmpty()) {
								val typeConfig = resolveTypeConfig(prop, typeName)
								types[typeName] = typeConfig
							}
						}
					}
					//找到配置文件中的顶级的key为"values"的属性，然后解析它的子属性，添加到values中
					"values" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val valueName = prop.key.removeSurroundingOrNull("value[", "]")
							if(valueName != null && valueName.isNotEmpty()) {
								val valueConfig = resolveEnumConfig(prop, valueName) ?: continue
								values[valueName] = valueConfig
							}
						}
					}
					//找到配置文件中的顶级的key为"enums"的属性，然后解析它的子属性，添加到enums中
					"enums" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val enumName = prop.key.removeSurroundingOrNull("enum[", "]")
							if(enumName != null && enumName.isNotEmpty()) {
								val enumConfig = resolveEnumConfig(prop, enumName) ?: continue
								enums[enumName] = enumConfig
							}
						}
					}
					//找到配置文件中的顶级的key为"tags"的属性，然后解析它的子属性，添加到tags中
					"tags" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val tagName = prop.key
							val tagConfig = resolveTagConfig(prop, tagName) ?: continue
							tags[tagName] = tagConfig
						}
					}
					//找到配置文件中的顶级的key为"links"的属性，然后解析它的子属性，添加到links中
					"links" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val linkName = prop.key
							val linkConfig = resolveLinkConfig(prop, linkName) ?: continue
							links[linkName] = linkConfig
						}
					}
					//找到配置文件中的顶级的key为"localisation_links"的属性，然后解析它的子属性，添加到localisationLinks中
					"localisation_links" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val linkName = prop.key
							val linkConfig = resolveLinkConfig(prop, linkName) ?: continue
							localisationLinks[linkName] = linkConfig
						}
					}
					//找到配置文件中的顶级的key为"localisation_commands"的属性，然后解析它的子属性，添加到localisationCommands中
					"localisation_commands" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val commandName = prop.key
							val commandConfig = resolveLocalisationCommandConfig(prop, commandName) ?: continue
							localisationCommands[commandName] = commandConfig
						}
					}
					//找到配置文件中的顶级的key为"modifier_categories"的属性，然后解析它的子属性，添加到modifierCategories中
					"modifier_categories" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val modifierCategoryName = prop.key
							val categoryConfig = resolveModifierCategoryConfig(prop, modifierCategoryName) ?: continue
							modifierCategories[modifierCategoryName] = categoryConfig
						}
					}
					//找到配置文件中的顶级的key为"modifiers"的属性，然后解析它的子属性，添加到modifiers中
					"modifiers" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val modifierName = prop.key
							val modifierConfig = resolveModifierConfig(prop, modifierName) ?: continue
							modifiers[modifierName] = modifierConfig
						}
					}
					//找到配置文件中的顶级的key为"scopes"的属性，然后解析它的子属性，添加到scopes中
					"scopes" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val scopeName = prop.key
							val scopeConfig = resolveScopeConfig(prop, scopeName) ?: continue
							scopes[scopeName] = scopeConfig
							for(alias in scopeConfig.aliases) {
								scopeAliasMap[alias] = scopeConfig
							}
						}
					}
					//找到配置文件中的顶级的key为"scope_groups"的属性，然后解析它的子属性，添加到scopeGroups中
					"scope_groups" -> {
						val props = property.properties ?: continue
						for(prop in props) {
							val scopeGroupName = prop.key
							val scopeGroupConfig = resolveScopeGroupConfig(prop, scopeGroupName) ?: continue
							scopeGroups[scopeGroupName] = scopeGroupConfig
						}
					}
					else -> {
						//判断配置文件中的顶级的key是否匹配"single_alias[?]"，如果匹配，则解析配置并添加到single_aliases中
						val singleAliasName = key.removeSurroundingOrNull("single_alias[", "]")
						if(singleAliasName != null) {
							val singleAliasConfig = resolveSingleAliasConfig(property, singleAliasName)
							val list = singleAliases.getOrPut(singleAliasName) { SmartList() }
							list.add(singleAliasConfig)
						}
						
						//判断配置文件中的顶级的key是否匹配"alias[?:?]"，如果匹配，则解析配置并添加到aliases中
						val aliasNamePair = key.removeSurroundingOrNull("alias[", "]")?.splitToPair(':')
						if(aliasNamePair != null) {
							val (aliasName, aliasSubName) = aliasNamePair
							val aliasConfig = resolveAliasConfig(property, aliasName, aliasSubName)
							
							val map = aliases.getOrPut(aliasName) { CollectionFactory.createCaseInsensitiveStringMap() } //忽略大小写
							val list = map.getOrPut(aliasSubName) { SmartList() }
							list.add(aliasConfig)
						}
						
						//其他情况，放到definition中
						val declarationConfig = resolveDefinitionConfig(property, key)
						declarations[key] = declarationConfig
					}
				}
			}
		}
		
		modifierCategoryIdMap = initModifierCategoryIdMap()
		tagMap = initTagMap()
		definitionTypesSupportParameters = initDefinitionTypesSupportParameters()
		
		bindModifierCategorySupportedScopeNames()
		bindLocalisationCommandSupportedScopeNames()
		bindModifierCategories()
	}
	
	//解析CWT配置
	
	private fun resolveFoldersCwt(fileConfig: CwtFileConfig, folders: MutableSet<String>) {
		fileConfig.values.mapTo(folders) { it.value }
	}
	
	private fun resolveTypeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtTypeConfig {
		var block = true
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
		var typeKeyFilter: ReversibleSet<String>? = null
		var startsWith: String? = null
		var graphRelatedTypes: Set<String>? = null
		val subtypes: MutableMap<String, CwtSubtypeConfig> = mutableMapOf()
		var localisation: CwtTypeLocalisationConfig? = null
		var images: CwtTypeImagesConfig? = null
		
		val props = propertyConfig.properties
		if(props != null && props.isNotEmpty()) {
			for(prop in props) {
				val key = prop.key
				when(key) {
					//定义的值是否需要为代码块，默认为是
					"block" -> block = prop.booleanValue ?: continue //EXTENDED BY PLS
					//这里path需要移除第一个子路径"game"，这个插件会忽略它
					"path" -> path = prop.stringValue?.removePrefix("game")?.trimStart('/') ?: continue
					"path_strict" -> pathStrict = prop.booleanValue ?: continue
					"path_file" -> pathFile = prop.stringValue ?: continue
					"path_extension" -> pathExtension = prop.stringValue ?: continue
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
						if(skipRootKey == null) skipRootKey = SmartList()
						skipRootKey.add(list) //出于一点点的性能考虑，这里保留大小写，后面匹配路径时会忽略掉
					}
					"localisation" -> {
						val configs: MutableList<Pair<String?, CwtLocationConfig>> = SmartList()
						val propPointer = prop.pointer
						val propProps = prop.properties ?: continue
						for(p in propProps) {
							val k = p.key
							val subtypeName = k.removeSurroundingOrNull("subtype[", "]")
							if(subtypeName != null) {
								val pps = p.properties ?: continue
								for(pp in pps) {
									val locationConfig = resolveLocationConfig(pp, pp.key) ?: continue
									configs.add(subtypeName to locationConfig)
								}
							} else {
								val locationConfig = resolveLocationConfig(p, k) ?: continue
								configs.add(null to locationConfig)
							}
						}
						localisation = CwtTypeLocalisationConfig(propPointer, configs)
					}
					"images" -> {
						val configs: MutableList<Pair<String?, CwtLocationConfig>> = SmartList()
						val propPointer = prop.pointer
						val propProps = prop.properties ?: continue
						for(p in propProps) {
							val k = p.key
							val subtypeName = k.removeSurroundingOrNull("subtype[", "]")
							if(subtypeName != null) {
								val pps = p.properties ?: continue
								for(pp in pps) {
									val locationConfig = resolveLocationConfig(pp, pp.key) ?: continue
									configs.add(subtypeName to locationConfig)
								}
							} else {
								val locationConfig = resolveLocationConfig(p, k) ?: continue
								configs.add(null to locationConfig)
							}
						}
						images = CwtTypeImagesConfig(propPointer, configs)
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
		if(options != null && options.isNotEmpty()) {
			for(option in options) {
				val key = option.key
				when(key) {
					"type_key_filter" -> {
						//值可能是string也可能是stringArray
						val value = option.stringValue
						val values = option.optionValues
						if(value == null && values == null) continue
						val set = CollectionFactory.createCaseInsensitiveStringSet() //忽略大小写
						if(value != null) set.add(value)
						if(values != null && values.isNotEmpty()) values.forEach { v -> v.stringValue?.let { sv -> set.add(sv) } }
						val notReversed = option.separatorType == CwtSeparatorType.EQUAL
						typeKeyFilter = set.toReversibleSet(notReversed)
					}
					"starts_with" -> startsWith = option.stringValue ?: continue //忽略大小写
					"graph_related_types" -> {
						graphRelatedTypes = option.optionValues?.mapNotNullTo(mutableSetOf()) { it.stringValue }
					}
				}
			}
		}
		
		return CwtTypeConfig(
			propertyConfig.pointer, name,
			block, path, pathStrict, pathFile, pathExtension,
			nameField, nameFromFile, typePerFile, unique, severity, skipRootKey,
			typeKeyFilter, startsWith, graphRelatedTypes, subtypes,
			localisation, images
		)
	}
	
	private fun resolveSubtypeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtSubtypeConfig {
		var typeKeyFilter: ReversibleSet<String>? = null
		var pushScope: String? = null
		var startsWith: String? = null
		var displayName: String? = null
		var abbreviation: String? = null
		var onlyIfNot: Set<String>? = null
		
		val options = propertyConfig.options
		if(options != null && options.isNotEmpty()) {
			for(option in options) {
				val key = option.key
				when(key) {
					"type_key_filter" -> {
						//值可能是string也可能是stringArray
						val value = option.stringValue
						val values = option.optionValues
						if(value == null && values == null) continue
						val set = CollectionFactory.createCaseInsensitiveStringSet() //忽略大小写
						if(value != null) set.add(value)
						if(values != null && values.isNotEmpty()) values.forEach { v -> v.stringValue?.let { sv -> set.add(sv) } }
						val notReversed = option.separatorType == CwtSeparatorType.EQUAL
						typeKeyFilter = set.toReversibleSet(notReversed)
					}
					"push_scope" -> pushScope = option.stringValue ?: continue
					"starts_with" -> startsWith = option.stringValue ?: continue //忽略大小写
					"display_name" -> displayName = option.stringValue ?: continue
					"abbreviation" -> abbreviation = option.stringValue ?: continue
					"only_if_not" -> onlyIfNot = option.optionValues?.mapNotNullTo(mutableSetOf()) { it.stringValue } ?: continue
				}
			}
		}
		return CwtSubtypeConfig(
			propertyConfig.pointer, name, propertyConfig,
			typeKeyFilter, pushScope, startsWith, displayName, abbreviation, onlyIfNot
		)
	}
	
	private fun resolveLocationConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLocationConfig? {
		val expression = propertyConfig.stringValue ?: return null
		var required = false
		var primary = false
		val optionValues = propertyConfig.optionValues
		if(optionValues != null && optionValues.isNotEmpty()) {
			for(optionValue in optionValues) {
				val value = optionValue.stringValue ?: continue
				when(value) {
					"required" -> required = true
					"primary" -> primary = true
					//"optional" -> pass() //忽略（默认就是required = false）
				}
			}
		}
		return CwtLocationConfig(propertyConfig.pointer, name, expression, required, primary)
	}
	
	private fun resolveEnumConfig(propertyConfig: CwtPropertyConfig, name: String): CwtEnumConfig? {
		val propertyConfigPointer = propertyConfig.pointer
		val propertyConfigValues = propertyConfig.values ?: return null
		if(propertyConfigValues.isEmpty()) return CwtEnumConfig(propertyConfigPointer, name, emptySet(), emptyMap())
		val values = CollectionFactory.createCaseInsensitiveStringSet() //忽略大小写
		val valueConfigMap = CollectionFactory.createCaseInsensitiveStringMap<CwtValueConfig>() //忽略大小写
		for(propertyConfigValue in propertyConfigValues) {
			values.add(propertyConfigValue.value)
			valueConfigMap.put(propertyConfigValue.value, propertyConfigValue)
		}
		return CwtEnumConfig(propertyConfigPointer, name, values, valueConfigMap)
	}
	
	private fun resolveTagConfig(propertyConfig: CwtPropertyConfig, name: String): CwtTagConfig? {
		val props = propertyConfig.properties ?: return null
		val since = propertyConfig.options?.find { it -> it.key == "since" }?.stringValue
		var supportedTypes: Set<String>? = null
		for(prop in props) {
			when(prop.key) {
				"supported_types" -> supportedTypes = prop.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
			}
		}
		if(supportedTypes == null) return null //排除
		return CwtTagConfig(propertyConfig.pointer, name, since, supportedTypes)
	}
	
	private fun resolveLinkConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLinkConfig? {
		var desc: String? = null
		var fromData = false
		var type: String? = null
		var dataSource: CwtValueExpression? = null
		var prefix: String? = null
		var inputScopes: Set<String>? = null
		var outputScope: String? = null
		val props = propertyConfig.properties ?: return null
		for(prop in props) {
			when(prop.key) {
				"desc" -> desc = prop.stringValue
				"from_data" -> fromData = prop.booleanValue ?: false
				"type" -> type = prop.stringValue
				"data_source" -> dataSource = prop.valueExpression
				"prefix" -> prefix = prop.stringValue
				"input_scopes" -> inputScopes = prop.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
				"output_scope" -> outputScope = prop.stringValue
			}
		}
		if(inputScopes == null) inputScopes = emptySet()
		if(outputScope == null) return null //排除
		return CwtLinkConfig(propertyConfig.pointer, name, desc, fromData, type, dataSource, prefix, inputScopes, outputScope)
	}
	
	private fun resolveLocalisationCommandConfig(propertyConfig: CwtPropertyConfig, name: String): CwtLocalisationCommandConfig? {
		val supportedScopes = propertyConfig.stringValue?.let { setOf(it) }
			?: propertyConfig.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
			?: return null
		return CwtLocalisationCommandConfig(propertyConfig.pointer, name, supportedScopes)
	}
	
	private fun resolveModifierCategoryConfig(propertyConfig: CwtPropertyConfig, name: String): CwtModifierCategoryConfig? {
		var internalId: String? = null
		var supportedScopes: Set<String>? = null
		val props = propertyConfig.properties
		if(props == null || props.isEmpty()) return null
		for(prop in props) {
			when(prop.key) {
				"internal_id" -> internalId = prop.value //目前版本的CWT配置已经不再有这个属性
				"supported_scopes" -> supportedScopes = prop.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
			}
		}
		if(supportedScopes == null) supportedScopes = emptySet()
		return CwtModifierCategoryConfig(propertyConfig.pointer, name, internalId, supportedScopes)
	}
	
	private fun resolveModifierConfig(propertyConfig: CwtPropertyConfig, name: String): CwtModifierConfig? {
		//string | string[]
		val categories = propertyConfig.stringValue?.let { setOf(it) }
			?: propertyConfig.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
			?: return null
		return CwtModifierConfig(propertyConfig.pointer, name, categories)
	}
	
	private fun resolveScopeConfig(propertyConfig: CwtPropertyConfig, name: String): CwtScopeConfig? {
		var aliases: Set<String>? = null
		val props = propertyConfig.properties
		if(props == null || props.isEmpty()) return null
		for(prop in props) {
			if(prop.key == "aliases") aliases = prop.values?.mapNotNullTo(mutableSetOf()) { it.stringValue }
		}
		if(aliases == null) aliases = emptySet()
		return CwtScopeConfig(propertyConfig.pointer, name, aliases)
	}
	
	private fun resolveScopeGroupConfig(propertyConfig: CwtPropertyConfig, name: String): CwtScopeGroupConfig? {
		val propertyConfigPointer = propertyConfig.pointer
		val propertyConfigValues = propertyConfig.values ?: return null
		if(propertyConfigValues.isEmpty()) return CwtScopeGroupConfig(propertyConfigPointer, name, emptySet(), emptyMap())
		val values = CollectionFactory.createCaseInsensitiveStringSet() //忽略大小写
		val valueConfigMap = CollectionFactory.createCaseInsensitiveStringMap<CwtValueConfig>() //忽略大小写
		for(propertyConfigValue in propertyConfigValues) {
			values.add(propertyConfigValue.value)
			valueConfigMap.put(propertyConfigValue.value, propertyConfigValue)
		}
		return CwtScopeGroupConfig(propertyConfigPointer, name, values, valueConfigMap)
	}
	
	private fun resolveSingleAliasConfig(propertyConfig: CwtPropertyConfig, name: String): CwtSingleAliasConfig {
		return CwtSingleAliasConfig(propertyConfig.pointer, name, propertyConfig)
	}
	
	private fun resolveAliasConfig(propertyConfig: CwtPropertyConfig, name: String, subName: String): CwtAliasConfig {
		return CwtAliasConfig(propertyConfig.pointer, name, subName, propertyConfig)
	}
	
	private fun resolveDefinitionConfig(propertyConfig: CwtPropertyConfig, name: String): CwtDeclarationConfig {
		if(propertyConfig.properties != null) {
			val configs = SmartList<Pair<String?, CwtPropertyConfig>>()
			for(prop in propertyConfig.properties) {
				//这里需要进行合并
				val subtypeName = prop.key.removeSurroundingOrNull("subtype[", "]")
				if(subtypeName != null) {
					val propProps = prop.properties
					if(propProps != null) {
						for(propProp in propProps) {
							configs.add(subtypeName to propProp)
						}
					}
				} else {
					configs.add(null to prop)
				}
			}
			return CwtDeclarationConfig(propertyConfig.pointer, name, propertyConfig, configs)
		} else {
			return CwtDeclarationConfig(propertyConfig.pointer, name, propertyConfig, null)
		}
	}
	
	//初始化基于解析的CWT配置的额外配置
	
	private fun initModifierCategoryIdMap(): Map<String, CwtModifierCategoryConfig> {
		val result: MutableMap<String, CwtModifierCategoryConfig> = mutableMapOf()
		for(modifierCategory in modifierCategories.values) {
			val internalId = modifierCategory.internalId ?: continue
			result[internalId] = modifierCategory
		}
		return result
	}
	
	private fun initTagMap(): Map<String, Map<@CaseInsensitive String, CwtTagConfig>> {
		val result: MutableMap<String, MutableMap<String, CwtTagConfig>> = mutableMapOf()
		for(tag in tags.values) {
			for(supportedType in tag.supportedTypes) {
				result.getOrPut(supportedType) { CollectionFactory.createCaseInsensitiveStringMap() }.put(tag.name, tag)
			}
		}
		return result
	}
	
	private fun initDefinitionTypesSupportParameters(): MutableSet<String> {
		val result = mutableSetOf<String>()
		for(aliasGroup in aliases.values) {
			for(aliasList in aliasGroup.values) {
				for(aliasConfig in aliasList) {
					val props = aliasConfig.config.properties ?: continue
					if(props.isEmpty()) continue
					if(props.none { it.keyExpression.let { e -> e.type == CwtDataTypes.Enum && e.value == CwtConfigHandler.paramsEnumName } }) continue
					val definitionType = aliasConfig.keyExpression.takeIf { it.type == CwtDataTypes.TypeExpression }?.value ?: continue
					result.add(definitionType)
				}
			}
		}
		return result
	}
	
	//绑定CWT配置
	
	private fun bindModifierCategorySupportedScopeNames() {
		for(modifierCategory in modifierCategories.values) {
			modifierCategory.supportedScopes.mapTo(modifierCategory.supportedScopeNames) { CwtConfigHandler.getScopeName(it, this) }
		}
	}
	
	private fun bindLocalisationCommandSupportedScopeNames() {
		for(localisationCommand in localisationCommands.values) {
			localisationCommand.supportedScopes.mapTo(localisationCommand.supportedScopeNames) { CwtConfigHandler.getScopeName(it, this) }
		}
	}
	
	private fun bindModifierCategories() {
		for(modifier in modifiers.values) {
			//category可能是modifierCategory的name，也可能是modifierCategory的internalId
			for(category in modifier.categories) {
				val categoryConfig = modifierCategories[category] ?: modifierCategoryIdMap[category] ?: continue
				modifier.categoryConfigMap[category] = categoryConfig
			}
		}
	}
	
	//解析定义和定义元素信息
	
	fun resolveDefinitionInfo(element: ParadoxDefinitionProperty,
		rootKey: String,
		path: ParadoxPath,
		elementPath: ParadoxElementPath<ParadoxScriptFile>
	): ParadoxDefinitionInfo? {
		for(typeConfig in types.values) {
			if(matchesType(typeConfig, element, rootKey, path, elementPath)) {
				//需要懒加载
				return ParadoxDefinitionInfo(rootKey, typeConfig, elementPath, gameType, this, element)
			}
		}
		return null
	}
	
	fun resolveDefinitionElementInfo(
		elementPath: ParadoxElementPath<ParadoxDefinitionProperty>,
		scope: String?,
		definitionInfo: ParadoxDefinitionInfo,
		element: PsiElement
	): ParadoxDefinitionElementInfo {
		return ParadoxDefinitionElementInfo(elementPath, scope, gameType, definitionInfo, this, element)
	}
	
	fun matchesType(typeConfig: CwtTypeConfig, element: ParadoxDefinitionProperty, rootKey: String, path: ParadoxPath, elementPath: ParadoxElementPath<ParadoxScriptFile>): Boolean {
		//判断element.value是否需要是block
		val blockConfig = typeConfig.block
		val elementBlock = element.block
		if(blockConfig) {
			if(elementBlock == null) return false
		}
		//判断element是否需要是scriptFile还是scriptProperty
		//TODO nameFromFile和typePerFile有什么区别？
		val nameFromFileConfig = typeConfig.nameFromFile || typeConfig.typePerFile
		if(nameFromFileConfig) {
			if(element !is ParadoxScriptFile) return false
		} else {
			if(element !is ParadoxScriptProperty) return false
		}
		//判断path是否匹配
		val pathConfig = typeConfig.path ?: return false
		val pathStrictConfig = typeConfig.pathStrict
		if(pathStrictConfig) {
			if(pathConfig != path.parent) return false
		} else {
			if(!pathConfig.matchesPath(path.parent)) return false
		}
		//判断path_name是否匹配
		val pathFileConfig = typeConfig.pathFile //String?
		if(pathFileConfig != null) {
			if(pathFileConfig != path.fileName) return false
		}
		//判断path_extension是否匹配
		val pathExtensionConfig = typeConfig.pathExtension //String?
		if(pathExtensionConfig != null) {
			if(pathExtensionConfig != "." + path.fileExtension) return false
		}
		//如果skip_root_key = any，则要判断是否需要跳过rootKey，如果为any，则任何情况都要跳过（忽略大小写）
		//skip_root_key可以为列表（如果是列表，其中的每一个root_key都要依次匹配）
		//skip_root_key可以重复（其中之一匹配即可）
		val skipRootKeyConfig = typeConfig.skipRootKey
		if(skipRootKeyConfig == null || skipRootKeyConfig.isEmpty()) {
			if(elementPath.length > 1) return false
		} else {
			var skipResult = false
			for(keys in skipRootKeyConfig) {
				if(keys.matchEntirePath(elementPath.subPaths, matchesParent = true)) {
					skipResult = true
					break
				}
			}
			if(!skipResult) return false
		}
		//如果starts_with存在，则要求type_key匹配这个前缀（忽略大小写）
		val startsWithConfig = typeConfig.startsWith
		if(startsWithConfig != null && startsWithConfig.isNotEmpty()) {
			if(!rootKey.startsWith(startsWithConfig, true)) return false
		}
		//如果type_key_filter存在，则通过type_key进行过滤（忽略大小写）
		val typeKeyFilterConfig = typeConfig.typeKeyFilter
		if(typeKeyFilterConfig != null && typeKeyFilterConfig.isNotEmpty()) {
			val filterResult = typeKeyFilterConfig.contains(rootKey)
			if(!filterResult) return false
		}
		//到这里再次处理block为false的情况
		if(!blockConfig) {
			return elementBlock == null
		}
		return true
	}
	
	fun matchesSubtype(subtypeConfig: CwtSubtypeConfig, element: ParadoxDefinitionProperty, rootKey: String, result: MutableList<CwtSubtypeConfig>): Boolean {
		//如果only_if_not存在，且已经匹配指定的任意子类型，则不匹配
		val onlyIfNotConfig = subtypeConfig.onlyIfNot
		if(onlyIfNotConfig != null && onlyIfNotConfig.isNotEmpty()) {
			val matchesAny = result.any { it.name in onlyIfNotConfig }
			if(matchesAny) return false
		}
		//如果starts_with存在，则要求type_key匹配这个前缀（忽略大小写）
		val startsWithConfig = subtypeConfig.startsWith
		if(startsWithConfig != null && startsWithConfig.isNotEmpty()) {
			if(!rootKey.startsWith(startsWithConfig, true)) return false
		}
		//如果type_key_filter存在，则通过type_key进行过滤（忽略大小写）
		val typeKeyFilterConfig = subtypeConfig.typeKeyFilter
		if(typeKeyFilterConfig != null && typeKeyFilterConfig.isNotEmpty()) {
			val filterResult = typeKeyFilterConfig.contains(rootKey)
			if(!filterResult) return false
		}
		//根据config对property进行内容匹配
		val elementConfig = subtypeConfig.config
		return CwtConfigHandler.matchesDefinitionProperty(element, elementConfig, this)
	}
}
