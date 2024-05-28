package icu.windea.pls.lang.util

import com.intellij.lang.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.stubs.*
import com.intellij.psi.util.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.*
import icu.windea.pls.core.util.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.search.selector.*
import icu.windea.pls.lang.util.renderer.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.model.*
import icu.windea.pls.model.expression.*
import icu.windea.pls.model.path.*
import icu.windea.pls.script.psi.*
import icu.windea.pls.script.psi.ParadoxScriptElementTypes.*
import icu.windea.pls.script.psi.impl.*

/**
 * 用于处理定义。
 *
 * @see ParadoxScriptDefinitionElement
 * @see ParadoxDefinitionInfo
 */
@Suppress("unused", "UNUSED_PARAMETER")
object ParadoxDefinitionHandler {
    //get info & match methods
    
    fun getInfo(element: ParadoxScriptDefinitionElement): ParadoxDefinitionInfo? {
        //快速判断
        if(runCatchingCancelable { element.greenStub }.getOrNull()?.isValidDefinition == false) return null
        //从缓存中获取
        return doGetInfoFromCache(element)
    }
    
    private fun doGetInfoFromCache(element: ParadoxScriptDefinitionElement): ParadoxDefinitionInfo? {
        return CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionInfo) {
            ProgressManager.checkCanceled()
            val file = element.containingFile ?: return@getCachedValue null
            val value = doGetInfo(element, file)
            //invalidated on file modification
            CachedValueProvider.Result.create(value, file)
        }
    }
    
    private fun doGetInfo(element: ParadoxScriptDefinitionElement, file: PsiFile = element.containingFile): ParadoxDefinitionInfo? {
        val rootKey = element.name.let { if(element is ParadoxScriptFile) it.substringBeforeLast('.') else it } //如果是文件名，不要包含扩展名
        if(element is ParadoxScriptProperty) {
            if(rootKey.isParameterized()) return null //排除可能带参数的情况
            if(rootKey.isInlineUsage()) return null //排除是内联调用的情况
        }
        val project = file.project
        
        //首先尝试直接基于stub进行解析
        getInfoFromStub(element, project)?.let { return it }
        
        val fileInfo = file.fileInfo ?: return null
        val path = fileInfo.pathToEntry //这里使用pathToEntry
        val elementPath = ParadoxElementPathHandler.get(element, PlsConstants.maxDefinitionDepth) ?: return null
        if(elementPath.path.isParameterized()) return null //忽略元素路径带参数的情况
        val gameType = fileInfo.rootInfo.gameType //这里还是基于fileInfo获取gameType
        val configGroup = getConfigGroup(project, gameType) //这里需要指定project
        val typeConfig = getMatchedTypeConfig(element, path, elementPath, rootKey, configGroup)
        if(typeConfig == null) return null
        return ParadoxDefinitionInfo(null, typeConfig, null, rootKey, elementPath, gameType, configGroup, element)
    }
    
    fun getMatchedTypeConfig(
        element: ParadoxScriptDefinitionElement,
        path: ParadoxPath,
        elementPath: ParadoxElementPath,
        rootKey: String,
        configGroup: CwtConfigGroup
    ): CwtTypeConfig? {
        for(typeConfig in configGroup.types.values) {
            if(matchesType(element, path, elementPath, rootKey, typeConfig, configGroup)) {
                return typeConfig
            }
        }
        return null
    }
    
    fun getMatchedTypeConfig(
        node: LighterASTNode,
        tree: LighterAST,
        path: ParadoxPath,
        elementPath: ParadoxElementPath,
        rootKey: String,
        configGroup: CwtConfigGroup
    ): CwtTypeConfig? {
        for(typeConfig in configGroup.types.values) {
            if(matchesType(node, tree, path, elementPath, rootKey, typeConfig, configGroup)) {
                return typeConfig
            }
        }
        return null
    }
    
    fun matchesType(
        element: ParadoxScriptDefinitionElement,
        path: ParadoxPath,
        elementPath: ParadoxElementPath,
        rootKey: String,
        typeConfig: CwtTypeConfig,
        configGroup: CwtConfigGroup
    ): Boolean {
        //判断definition是否需要是scriptFile还是scriptProperty
        val typePerFileConfig = typeConfig.typePerFile
        if(typePerFileConfig) {
            if(element !is ParadoxScriptFile) return false
        } else {
            if(element !is ParadoxScriptProperty) return false
        }
        
        val fastResult = matchesTypeFast(path, elementPath, rootKey, typeConfig, configGroup)
        if(fastResult != null) return fastResult
        
        //判断definition的propertyValue是否需要是block
        run {
            val declarationConfig = configGroup.declarations.get(typeConfig.name)?.config ?: return@run
            val propertyValue = element.castOrNull<ParadoxScriptProperty>()?.propertyValue ?: return@run
            //兼容进行代码补全时用户输入未完成的情况
            val isIncomplete = propertyValue.elementType == STRING
                && propertyValue.text == PlsConstants.dummyIdentifier
                && propertyValue.isIncomplete()
            if(isIncomplete) return@run
            val isBlock = propertyValue.elementType == BLOCK
            val isBlockConfig = declarationConfig.valueExpression.type == CwtDataTypes.Block
            if(isBlockConfig != isBlock) return false
        }
        
        return true
    }
    
    fun matchesType(
        node: LighterASTNode,
        tree: LighterAST,
        path: ParadoxPath,
        elementPath: ParadoxElementPath,
        rootKey: String,
        typeConfig: CwtTypeConfig,
        configGroup: CwtConfigGroup
    ): Boolean {
        //判断definition是否需要是scriptFile还是scriptProperty
        val elementType = node.tokenType
        val typePerFileConfig = typeConfig.typePerFile
        if(typePerFileConfig) {
            if(elementType != ParadoxScriptStubElementTypes.FILE) return false
        } else {
            if(elementType != PROPERTY) return false
        }
        
        val fastResult = matchesTypeFast(path, elementPath, rootKey, typeConfig, configGroup)
        if(fastResult != null) return fastResult
        
        //判断definition的propertyValue是否需要是block
        run {
            val declarationConfig = configGroup.declarations.get(typeConfig.name)?.config ?: return@run
            val propertyValue = node.firstChild(tree, ParadoxScriptTokenSets.VALUES) ?: return@run
            val isBlock = propertyValue.tokenType == BLOCK
            val isBlockConfig = declarationConfig.valueExpression.type == CwtDataTypes.Block
            if(isBlockConfig != isBlock) return false
        }
        
        return true
    }
    
    fun matchesTypeFast(
        path: ParadoxPath,
        elementPath: ParadoxElementPath,
        rootKey: String,
        typeConfig: CwtTypeConfig,
        configGroup: CwtConfigGroup
    ): Boolean? {
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
            if(pathExtensionConfig != path.fileExtension) return false
        }
        
        //如果skip_root_key = any，则要判断是否需要跳过rootKey，如果为any，则任何情况都要跳过（忽略大小写）
        //skip_root_key可以为列表（如果是列表，其中的每一个root_key都要依次匹配）
        //skip_root_key可以重复（其中之一匹配即可）
        val skipRootKeyConfig = typeConfig.skipRootKey
        if(skipRootKeyConfig.isNullOrEmpty()) {
            if(elementPath.length > 1) return false
        } else {
            val result = skipRootKeyConfig.any { elementPath.matchEntire(it, useParentPath = true) }
            if(!result) return false
        }
        //如果starts_with存在，则要求type_key匹配这个前缀（不忽略大小写）
        val startsWithConfig = typeConfig.startsWith
        if(!startsWithConfig.isNullOrEmpty()) {
            val result = rootKey.startsWith(startsWithConfig, true)
            if(!result) return false
        }
        //如果type_key_regex存在，则要求type_key匹配
        val typeKeyRegexConfig = typeConfig.typeKeyRegex
        if(typeKeyRegexConfig != null) {
            val result = typeKeyRegexConfig.matches(rootKey)
            if(!result) return false
        }
        //如果type_key_filter存在，则通过type_key进行过滤（忽略大小写）
        val typeKeyFilterConfig = typeConfig.typeKeyFilter
        if(typeKeyFilterConfig != null && typeKeyFilterConfig.value.isNotEmpty()) {
            val filterResult = typeKeyFilterConfig.where { it.contains(rootKey) }
            if(!filterResult) return false
        }
        //如果name_field存在，则要求root_key必须是由type_key_filter指定的所有可能的root_key之一，或者没有指定任何root_key
        val nameFieldConfig = typeConfig.nameField
        if(nameFieldConfig != null) {
            val result = typeConfig.possibleRootKeys.isEmpty() || typeConfig.possibleRootKeys.contains(rootKey)
            if(!result) return false
        }
        return null //需要进一步匹配
    }
    
    fun matchesTypeByUnknownDeclaration(
        path: ParadoxPath,
        elementPath: ParadoxElementPath?,
        rootKey: String?,
        typeConfig: CwtTypeConfig
    ): Boolean {
        //判断element是否需要是scriptFile还是scriptProperty
        val typePerFileConfig = typeConfig.typePerFile
        if(typePerFileConfig) return false
        
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
            if(pathExtensionConfig != path.fileExtension) return false
        }
        
        if(elementPath != null) {
            //如果skip_root_key = any，则要判断是否需要跳过rootKey，如果为any，则任何情况都要跳过（忽略大小写）
            //skip_root_key可以为列表（如果是列表，其中的每一个root_key都要依次匹配）
            //skip_root_key可以重复（其中之一匹配即可）
            val skipRootKeyConfig = typeConfig.skipRootKey
            if(skipRootKeyConfig.isNullOrEmpty()) {
                if(elementPath.length > 1) return false
            } else {
                val skipResult = skipRootKeyConfig.any { elementPath.matchEntire(it, useParentPath = true) }
                if(!skipResult) return false
            }
        }
        
        if(rootKey != null) {
            //如果starts_with存在，则要求type_key匹配这个前缀（不忽略大小写）
            val startsWithConfig = typeConfig.startsWith
            if(!startsWithConfig.isNullOrEmpty()) {
                val result = rootKey.startsWith(startsWithConfig, true)
                if(!result) return false
            }
            //如果type_key_regex存在，则要求type_key匹配
            val typeKeyRegexConfig = typeConfig.typeKeyRegex
            if(typeKeyRegexConfig != null) {
                val result = typeKeyRegexConfig.matches(rootKey)
                if(!result) return false
            }
            //如果type_key_filter存在，则通过type_key进行过滤（忽略大小写）
            val typeKeyFilterConfig = typeConfig.typeKeyFilter
            if(typeKeyFilterConfig != null && typeKeyFilterConfig.value.isNotEmpty()) {
                val filterResult = typeKeyFilterConfig.where { it.contains(rootKey) }
                if(!filterResult) return false
            }
            //如果name_field存在，则要求root_key必须是由type_key_filter指定的所有可能的root_key之一，或者没有指定任何root_key
            val nameFieldConfig = typeConfig.nameField
            if(nameFieldConfig != null) {
                val result = typeConfig.possibleRootKeys.isEmpty() || typeConfig.possibleRootKeys.contains(rootKey)
                if(!result) return false
            }
        }
        
        return true
    }
    
    fun matchesSubtype(
        element: ParadoxScriptDefinitionElement,
        rootKey: String,
        subtypeConfig: CwtSubtypeConfig,
        subtypeConfigs: MutableList<CwtSubtypeConfig>,
        configGroup: CwtConfigGroup,
        matchOptions: Int = CwtConfigMatcher.Options.Default
    ): Boolean {
        val fastResult = matchesSubtypeFast(rootKey, subtypeConfig, subtypeConfigs, configGroup)
        if(fastResult != null) return fastResult
        
        //根据config对property进行内容匹配
        val elementConfig = subtypeConfig.config
        if(elementConfig.configs.isNullOrEmpty()) return true
        return doMatchDefinition(element, elementConfig, configGroup, matchOptions)
    }
    
    fun matchesSubtypeFast(
        rootKey: String,
        subtypeConfig: CwtSubtypeConfig,
        subtypeConfigs: MutableList<CwtSubtypeConfig>,
        configGroup: CwtConfigGroup
    ): Boolean? {
        //如果only_if_not存在，且已经匹配指定的任意子类型，则不匹配
        val onlyIfNotConfig = subtypeConfig.onlyIfNot
        if(!onlyIfNotConfig.isNullOrEmpty()) {
            val matchesAny = subtypeConfigs.any { it.name in onlyIfNotConfig }
            if(matchesAny) return false
        }
        //如果starts_with存在，则要求type_key匹配这个前缀（不忽略大小写）
        val startsWithConfig = subtypeConfig.startsWith
        if(!startsWithConfig.isNullOrEmpty()) {
            val result = rootKey.startsWith(startsWithConfig, false)
            if(!result) return false
        }
        //如果type_key_regex存在，则要求type_key匹配
        val typeKeyRegexConfig = subtypeConfig.typeKeyRegex
        if(typeKeyRegexConfig != null) {
            val result = typeKeyRegexConfig.matches(rootKey)
            if(!result) return false
        }
        //如果type_key_filter存在，则通过type_key进行过滤（忽略大小写）
        val typeKeyFilterConfig = subtypeConfig.typeKeyFilter
        if(typeKeyFilterConfig != null && typeKeyFilterConfig.value.isNotEmpty()) {
            val filterResult = typeKeyFilterConfig.where { it.contains(rootKey) }
            if(!filterResult) return false
        }
        //根据config对property进行内容匹配
        val elementConfig = subtypeConfig.config
        if(elementConfig.configs.isNullOrEmpty()) return true
        return null //需要进一步匹配
    }
    
    private fun doMatchDefinition(
        definitionElement: ParadoxScriptDefinitionElement,
        propertyConfig: CwtPropertyConfig,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        //这里不能基于内联后的声明结构，否则可能会导致SOE
        //也不要参数条件表达式中的声明结构判断，
        val childValueConfigs = propertyConfig.values.orEmpty()
        val blockElement = definitionElement.block
        if(childValueConfigs.isNotEmpty()) {
            //匹配值列表
            if(!doMatchValues(definitionElement, blockElement, childValueConfigs, configGroup, matchOptions)) return false //继续匹配
        }
        val childPropertyConfigs = propertyConfig.properties.orEmpty()
        if(childPropertyConfigs.isNotEmpty()) {
            //匹配属性列表
            if(!doMatchProperties(definitionElement, blockElement, childPropertyConfigs, configGroup, matchOptions)) return false //继续匹配
        }
        return true
    }
    
    private fun doMatchProperty(
        definitionElement: ParadoxScriptDefinitionElement,
        propertyElement: ParadoxScriptProperty,
        propertyConfig: CwtPropertyConfig,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        val propValue = propertyElement.propertyValue
        if(propValue == null) {
            //对于propertyValue同样这样判断（可能脚本没有写完）
            return propertyConfig.cardinality?.min == 0
        } else {
            when {
                //匹配布尔值
                propertyConfig.booleanValue != null -> {
                    if(propValue !is ParadoxScriptBoolean || propValue.booleanValue != propertyConfig.booleanValue) return false
                }
                //匹配值
                propertyConfig.stringValue != null -> {
                    val expression = ParadoxDataExpression.resolve(propValue, matchOptions)
                    return CwtConfigMatcher.matches(propValue, expression, propertyConfig.valueExpression, propertyConfig, configGroup, matchOptions).get(matchOptions)
                }
                //匹配single_alias
                CwtConfigHandler.isSingleAliasEntryConfig(propertyConfig) -> {
                    return doMatchSingleAlias(definitionElement, propertyElement, propertyConfig, configGroup, matchOptions)
                }
                //匹配alias
                CwtConfigHandler.isAliasEntryConfig(propertyConfig) -> {
                    return doMatchAlias(definitionElement, propertyElement, propertyConfig, configGroup, matchOptions)
                }
                propertyConfig.configs.orEmpty().isNotEmpty() -> {
                    val blockElement = propertyElement.block
                    //匹配值列表
                    if(!doMatchValues(definitionElement, blockElement, propertyConfig.values.orEmpty(), configGroup, matchOptions)) return false
                    //匹配属性列表
                    if(!doMatchProperties(definitionElement, blockElement, propertyConfig.properties.orEmpty(), configGroup, matchOptions)) return false
                }
            }
        }
        return true
    }
    
    private fun doMatchProperties(
        definitionElement: ParadoxScriptDefinitionElement,
        blockElement: ParadoxScriptBlockElement?,
        propertyConfigs: List<CwtPropertyConfig>,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        if(propertyConfigs.isEmpty()) return true
        if(blockElement == null) return false
        
        val occurrenceMap = propertyConfigs.associateByTo(mutableMapOf(), { it.key }, { it.toOccurrence(definitionElement, configGroup.project) })
        
        //注意：propConfig.key可能有重复，这种情况下只要有其中一个匹配即可
        val matched = blockElement.processProperty { propertyElement ->
            val keyElement = propertyElement.propertyKey
            val expression = ParadoxDataExpression.resolve(keyElement, matchOptions)
            val propConfigs = propertyConfigs.filter {
                CwtConfigMatcher.matches(keyElement, expression, it.keyExpression, it, configGroup, matchOptions).get(matchOptions)
            }
            //如果没有匹配的规则则忽略
            if(propConfigs.isNotEmpty()) {
                val matched = propConfigs.any { propConfig ->
                    val matched = doMatchProperty(definitionElement, propertyElement, propConfig, configGroup, matchOptions)
                    if(matched) occurrenceMap.get(propConfig.key)?.let { it.actual++ }
                    matched
                }
                matched
            } else {
                true
            }
        }
        if(!matched) return false
        
        return occurrenceMap.values.all { (it.actual >= (it.min ?: 1)) && (it.max == null || (it.actual <= (it.max ?: 1))) }
    }
    
    private fun doMatchValues(
        definitionElement: ParadoxScriptDefinitionElement,
        blockElement: ParadoxScriptBlockElement?,
        valueConfigs: List<CwtValueConfig>,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        if(valueConfigs.isEmpty()) return true
        if(blockElement == null) return false
        //要求其中所有的value的值在最终都会小于等于指定值
        val minMap = valueConfigs.associateByTo(mutableMapOf(), { it.value }, { it.cardinality?.min ?: 1 }) //默认为1
        
        val matched = blockElement.processValue { valueElement ->
            //如果没有匹配的规则则忽略
            val expression = ParadoxDataExpression.resolve(valueElement, matchOptions)
            
            val matched = valueConfigs.any { valueConfig ->
                val matched = CwtConfigMatcher.matches(valueElement, expression, valueConfig.valueExpression, valueConfig, configGroup, matchOptions).get(matchOptions)
                if(matched) minMap.compute(valueConfig.value) { _, v -> if(v == null) 1 else v - 1 }
                matched
            }
            matched
        }
        if(!matched) return false
        
        return minMap.values.all { it <= 0 }
    }
    
    private fun doMatchSingleAlias(
        definitionElement: ParadoxScriptDefinitionElement,
        propertyElement: ParadoxScriptProperty,
        propertyConfig: CwtPropertyConfig,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        val singleAliasName = propertyConfig.valueExpression.value ?: return false
        val singleAlias = configGroup.singleAliases[singleAliasName] ?: return false
        return doMatchProperty(definitionElement, propertyElement, singleAlias.config, configGroup, matchOptions)
    }
    
    private fun doMatchAlias(
        definitionElement: ParadoxScriptDefinitionElement,
        propertyElement: ParadoxScriptProperty,
        propertyConfig: CwtPropertyConfig,
        configGroup: CwtConfigGroup,
        matchOptions: Int
    ): Boolean {
        //aliasName和aliasSubName需要匹配
        val aliasName = propertyConfig.keyExpression.value ?: return false
        val key = propertyElement.name
        val quoted = propertyElement.propertyKey.text.isLeftQuoted()
        val aliasSubName = CwtConfigHandler.getAliasSubName(propertyElement, key, quoted, aliasName, configGroup, matchOptions) ?: return false
        val aliasGroup = configGroup.aliasGroups[aliasName] ?: return false
        val aliases = aliasGroup[aliasSubName] ?: return false
        return aliases.any { alias ->
            doMatchProperty(definitionElement, propertyElement, alias.config, configGroup, matchOptions)
        }
    }
    
    fun getName(element: ParadoxScriptDefinitionElement): String? {
        return runCatchingCancelable { element.greenStub }.getOrNull()?.name ?: element.definitionInfo?.name
    }
    
    fun getType(element: ParadoxScriptDefinitionElement): String? {
        return runCatchingCancelable { element.greenStub }.getOrNull()?.type ?: element.definitionInfo?.type
    }
    
    fun getSubtypes(element: ParadoxScriptDefinitionElement): List<String>? {
        //定义的subtype可能需要通过访问索引获取，不能在索引时就获取
        return element.definitionInfo?.subtypes
    }
    
    //stub methods
    
    fun createStubForFile(file: PsiFile, tree: LighterAST): StubElement<*>? {
        if(file !is ParadoxScriptFile) return null
        val node = tree.root
        val rootKey = file.name.substringBeforeLast('.')
        val project = file.project
        val vFile = selectFile(file) ?: return null
        val fileInfo = vFile.fileInfo ?: return null
        val gameType = selectGameType(vFile) ?: return null
        val path = fileInfo.pathToEntry //这里使用pathToEntry
        val elementPath = ParadoxElementPathHandler.get(node, tree, vFile) ?: return null
        val configGroup = getConfigGroup(project, gameType) //这里需要指定project
        val typeConfig = getMatchedTypeConfig(node, tree, path, elementPath, rootKey, configGroup)
        if(typeConfig == null) return null
        //NOTE 这里不处理需要内联的情况
        val name = getNameWhenCreateDefinitionStub(typeConfig, rootKey, node, tree)
        val type = typeConfig.name
        val subtypes = getSubtypesWhenCreateDefinitionStub(typeConfig, rootKey, configGroup) //如果无法在索引时获取，之后再懒加载
        return ParadoxScriptFileStubImpl(file, name, type, subtypes, gameType)
    }
    
    fun createStub(psi: ParadoxScriptProperty, parentStub: StubElement<*>): ParadoxScriptPropertyStub? {
        val rootKey = psi.name
        if(rootKey.isParameterized()) return null //排除可能带参数的情况
        if(rootKey.isInlineUsage()) return null //排除是内联调用的情况
        if(!checkParentStubWhenCreateDefinitionStub(parentStub)) return null
        if(!checkRootKeyWhenCreateDefinitionStub(rootKey, parentStub)) return null
        val definitionInfo = psi.definitionInfo ?: return null
        val name = definitionInfo.name
        val type = definitionInfo.type
        val subtypes = runCatchingCancelable { definitionInfo.subtypes }.getOrNull() //如果无法在索引时获取，之后再懒加载
        val elementPath = definitionInfo.elementPath
        val gameType = definitionInfo.gameType
        return ParadoxScriptPropertyStubImpl(parentStub, name, type, subtypes, rootKey, elementPath, gameType)
    }
    
    fun createStub(tree: LighterAST, node: LighterASTNode, parentStub: StubElement<*>): ParadoxScriptPropertyStub? {
        val rootKey = getNameFromNode(node, tree) ?: return null
        if(rootKey.isParameterized()) return null //排除可能带参数的情况
        if(rootKey.isInlineUsage()) return null //排除是内联调用的情况
        if(!checkParentStubWhenCreateDefinitionStub(parentStub)) return null
        if(!checkRootKeyWhenCreateDefinitionStub(rootKey, parentStub)) return null
        val psi = parentStub.psi
        val file = psi.containingFile
        val project = file.project
        val vFile = selectFile(psi) ?: return null
        val fileInfo = vFile.fileInfo ?: return null
        val gameType = selectGameType(vFile) ?: return null
        val path = fileInfo.pathToEntry //这里使用pathToEntry
        val elementPath = ParadoxElementPathHandler.get(node, tree, vFile) ?: return null
        val configGroup = getConfigGroup(project, gameType) //这里需要指定project
        val typeConfig = getMatchedTypeConfig(node, tree, path, elementPath, rootKey, configGroup)
        if(typeConfig == null) return null
        //NOTE 这里不处理需要内联的情况
        val name = getNameWhenCreateDefinitionStub(typeConfig, rootKey, node, tree)
        val type = typeConfig.name
        val subtypes = getSubtypesWhenCreateDefinitionStub(typeConfig, rootKey, configGroup) //如果无法在索引时获取，之后再懒加载
        return ParadoxScriptPropertyStubImpl(parentStub, name, type, subtypes, rootKey, elementPath, gameType)
    }
    
    private fun checkParentStubWhenCreateDefinitionStub(parentStub: StubElement<*>): Boolean {
        //优化：parentStub必须对应一个定义且该定义可以嵌套定义且stub可能对应这些嵌套定义，或者对应一个非定义的文件，或者对应一个非定义的属性且在定义之外
        if(parentStub is ParadoxScriptDefinitionElementStub<*>) {
            if(parentStub.isValidDefinition) {
                if(parentStub.nestedTypeRootKeys.isEmpty()) return false
            } else {
                var stub: StubElement<PsiElement>? = parentStub.parentStub
                while(stub != null) {
                    if(stub is ParadoxScriptDefinitionElementStub<*> && stub.isValidDefinition) return false
                    stub = stub.parentStub
                }
            }
        }
        return true
    }
    
    private fun checkRootKeyWhenCreateDefinitionStub(rootKey: String, parentStub: StubElement<*>): Boolean {
        //优化：如果parentStub对应一个定义，该定义必须可以嵌套定义且stub可能对应这些嵌套定义
        if(parentStub is ParadoxScriptDefinitionElementStub<*>) {
            if(parentStub.isValidDefinition) {
                if(!parentStub.nestedTypeRootKeys.contains(rootKey)) return false
            }
        }
        return true
    }
    
    private fun getNameWhenCreateDefinitionStub(typeConfig: CwtTypeConfig, rootKey: String, node: LighterASTNode, tree: LighterAST): String {
        return when {
            typeConfig.nameFromFile -> rootKey
            typeConfig.nameField == null -> rootKey
            typeConfig.nameField == "" -> ""
            typeConfig.nameField == "-" -> getValueFromNode(node, tree).orEmpty()
            else -> node.firstChild(tree, ParadoxScriptTokenSets.BLOCK_OR_ROOT_BLOCK)
                ?.firstChild(tree) { it.tokenType == PROPERTY && getNameFromNode(it, tree)?.equals(typeConfig.nameField, true) == true }
                ?.let { getValueFromNode(it, tree) }
                .orEmpty()
        }
    }
    
    private fun getSubtypesWhenCreateDefinitionStub(typeConfig: CwtTypeConfig, rootKey: String, configGroup: CwtConfigGroup): List<String>? {
        val subtypesConfig = typeConfig.subtypes
        val result = mutableListOf<CwtSubtypeConfig>()
        for(subtypeConfig in subtypesConfig.values) {
            if(matchesSubtypeFast(rootKey, subtypeConfig, result, configGroup) ?: return null) {
                result.add(subtypeConfig)
            }
        }
        return result.map { it.name }
    }
    
    private fun getNameFromNode(node: LighterASTNode, tree: LighterAST): String? {
        return node.firstChild(tree, PROPERTY_KEY)?.firstChild(tree, ParadoxScriptTokenSets.PROPERTY_KEY_TOKENS)?.internNode(tree)?.toString()?.unquote()
    }
    
    private fun getValueFromNode(node: LighterASTNode, tree: LighterAST): String? {
        return node.firstChild(tree, STRING)?.firstChild(tree, ParadoxScriptTokenSets.STRING_TOKENS)?.internNode(tree)?.toString()?.unquote()
    }
    
    fun shouldCreateStub(node: ASTNode): Boolean {
        return true
    }
    
    fun shouldCreateStub(tree: LighterAST, node: LighterASTNode, parentStub: StubElement<*>): Boolean {
        return true
    }
    
    fun getInfoFromStub(element: ParadoxScriptDefinitionElement, project: Project): ParadoxDefinitionInfo? {
        val stub = runCatchingCancelable { element.greenStub }.getOrNull() ?: return null
        //if(!stub.isValid()) return null //这里不用再次判断
        val name = stub.name
        val type = stub.type
        val gameType = stub.gameType
        val configGroup = getConfigGroup(project, gameType) //这里需要指定project
        val typeConfig = configGroup.types[type] ?: return null
        val subtypes = stub.subtypes
        val subtypeConfigs = subtypes?.mapNotNull { typeConfig.subtypes[it] }
        val rootKey = stub.rootKey
        val elementPath = stub.elementPath
        return ParadoxDefinitionInfo(name, typeConfig, subtypeConfigs, rootKey, elementPath, gameType, configGroup, element)
    }
    
    //related localisations & images methods
    
    fun getPrimaryLocalisationKey(element: ParadoxScriptDefinitionElement): String? {
        return doGetPrimaryLocalisationKeyFromCache(element)
    }
    
    private fun doGetPrimaryLocalisationKeyFromCache(element: ParadoxScriptDefinitionElement): String? {
        return CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionPrimaryLocalisationKey) {
            ProgressManager.checkCanceled()
            val value = doGetPrimaryLocalisationKey(element)
            val tracker = ParadoxModificationTrackers.LocalisationFileTracker
            CachedValueProvider.Result.create(value, element, tracker)
        }
    }
    
    private fun doGetPrimaryLocalisationKey(element: ParadoxScriptDefinitionElement): String? {
        val definitionInfo = element.definitionInfo ?: return null
        val primaryLocalisations = definitionInfo.primaryLocalisations
        if(primaryLocalisations.isEmpty()) return null //没有或者CWT规则不完善
        val project = definitionInfo.project
        for(primaryLocalisation in primaryLocalisations) {
            val selector = localisationSelector(project, element).contextSensitive().preferLocale(ParadoxLocaleHandler.getPreferredLocale())
            val resolved = primaryLocalisation.locationExpression.resolve(element, definitionInfo, selector)
            val key = resolved?.name ?: continue
            return key
        }
        return null
    }
    
    fun getPrimaryLocalisation(element: ParadoxScriptDefinitionElement): ParadoxLocalisationProperty? {
        return doGetPrimaryLocalisationFromCache(element)
    }
    
    private fun doGetPrimaryLocalisationFromCache(element: ParadoxScriptDefinitionElement): ParadoxLocalisationProperty? =
        CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionPrimaryLocalisation) {
            ProgressManager.checkCanceled()
            val value = doGetPrimaryLocalisation(element)
            val tracker = ParadoxModificationTrackers.LocalisationFileTracker
            CachedValueProvider.Result.create(value, element, tracker)
        }
    
    private fun doGetPrimaryLocalisation(element: ParadoxScriptDefinitionElement): ParadoxLocalisationProperty? {
        val definitionInfo = element.definitionInfo ?: return null
        val primaryLocalisations = definitionInfo.primaryLocalisations
        if(primaryLocalisations.isEmpty()) return null //没有或者CWT规则不完善
        val project = definitionInfo.project
        for(primaryLocalisation in primaryLocalisations) {
            val selector = localisationSelector(project, element).contextSensitive().preferLocale(ParadoxLocaleHandler.getPreferredLocale())
            val resolved = primaryLocalisation.locationExpression.resolve(element, definitionInfo, selector)
            val localisation = resolved?.element ?: continue
            return localisation
        }
        return null
    }
    
    fun getPrimaryLocalisations(element: ParadoxScriptDefinitionElement): Set<ParadoxLocalisationProperty> {
        return doGetPrimaryLocalisationsFromCache(element)
    }
    
    private fun doGetPrimaryLocalisationsFromCache(element: ParadoxScriptDefinitionElement): Set<ParadoxLocalisationProperty> {
        return CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionPrimaryLocalisations) {
            ProgressManager.checkCanceled()
            val value = doGetPrimaryLocalisations(element)
            val tracker = ParadoxModificationTrackers.LocalisationFileTracker
            CachedValueProvider.Result.create(value, element, tracker)
        }
    }
    
    private fun doGetPrimaryLocalisations(element: ParadoxScriptDefinitionElement): Set<ParadoxLocalisationProperty> {
        val definitionInfo = element.definitionInfo ?: return emptySet()
        val primaryLocalisations = definitionInfo.primaryLocalisations
        if(primaryLocalisations.isEmpty()) return emptySet() //没有或者CWT规则不完善
        val project = definitionInfo.project
        val result = mutableSetOf<ParadoxLocalisationProperty>()
        for(primaryLocalisation in primaryLocalisations) {
            val selector = localisationSelector(project, element).contextSensitive().preferLocale(ParadoxLocaleHandler.getPreferredLocale())
            val resolved = primaryLocalisation.locationExpression.resolveAll(element, definitionInfo, selector)
            val localisations = resolved?.elements ?: continue
            result.addAll(localisations)
        }
        return result
    }
    
    fun getPrimaryImage(element: ParadoxScriptDefinitionElement): PsiFile? {
        return doGetPrimaryImageFromCache(element)
    }
    
    private fun doGetPrimaryImageFromCache(element: ParadoxScriptDefinitionElement): PsiFile? {
        return CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionPrimaryImage) {
            ProgressManager.checkCanceled()
            val value = doGetPrimaryImage(element)
            val tracker = PsiModificationTracker.MODIFICATION_COUNT
            CachedValueProvider.Result.create(value, element, tracker)
        }
    }
    
    private fun doGetPrimaryImage(element: ParadoxScriptDefinitionElement): PsiFile? {
        val definitionInfo = element.definitionInfo ?: return null
        val primaryImages = definitionInfo.primaryImages
        if(primaryImages.isEmpty()) return null //没有或者CWT规则不完善
        for(primaryImage in primaryImages) {
            val resolved = primaryImage.locationExpression.resolve(element, definitionInfo, toFile = true)
            val file = resolved?.element?.castOrNull<PsiFile>()
            if(file == null) continue
            element.putUserData(PlsKeys.frameInfo, resolved.frameInfo)
            return file
        }
        return null
    }
    
    fun getLocalizedNames(element: ParadoxScriptDefinitionElement): Set<String> {
        return doGetLocalizedNamesFromCache(element)
    }
    
    private fun doGetLocalizedNamesFromCache(element: ParadoxScriptDefinitionElement): Set<String> {
        return CachedValuesManager.getCachedValue(element, PlsKeys.cachedDefinitionLocalizedNames) {
            ProgressManager.checkCanceled()
            val value = doGetLocalizedNames(element)
            val tracker = ParadoxModificationTrackers.LocalisationFileTracker
            CachedValueProvider.Result.create(value, element, tracker)
        }
    }
    
    private fun doGetLocalizedNames(element: ParadoxScriptDefinitionElement): Set<String> {
        //这里返回的是基于偏好语言区域的所有本地化名字（即使最终会被覆盖掉）
        val localizedNames = mutableSetOf<String>()
        val primaryLocalisations = getPrimaryLocalisations(element)
        primaryLocalisations.forEach { localisation ->
            ProgressManager.checkCanceled()
            val r = ParadoxLocalisationTextRenderer.render(localisation).orNull()
            if(r != null) localizedNames.add(r)
        }
        return localizedNames
    }
}
