@file:Suppress("NOTHING_TO_INLINE")

package icu.windea.pls

import com.intellij.codeInsight.documentation.*
import com.intellij.openapi.components.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.util.*
import icu.windea.pls.core.settings.*
import icu.windea.pls.cwt.config.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.model.*
import icu.windea.pls.script.psi.*
import icu.windea.pls.util.*
import org.jetbrains.annotations.*

//Misc Extensions

fun getDefaultProject() = ProjectManager.getInstance().defaultProject

fun getSettings() = ParadoxSettingsState.getInstance()

fun getConfig(): CwtConfigGroups {
	return ServiceManager.getService(getDefaultProject(), CwtConfigProvider::class.java).configGroups
}

fun getConfig(project: Project) = ServiceManager.getService(project, CwtConfigProvider::class.java).configGroups

fun inferParadoxLocale() = when(System.getProperty("user.language")) {
	"zh" -> getConfig().localeMap.getValue("l_simp_chinese")
	"en" -> getConfig().localeMap.getValue("l_english")
	"pt" -> getConfig().localeMap.getValue("l_braz_por")
	"fr" -> getConfig().localeMap.getValue("l_french")
	"de" -> getConfig().localeMap.getValue("l_german")
	"pl" -> getConfig().localeMap.getValue("l_ponish")
	"ru" -> getConfig().localeMap.getValue("l_russian")
	"es" -> getConfig().localeMap.getValue("l_spanish")
	else -> getConfig().localeMap.getValue("l_english")
}

/**得到指定元素之前的所有直接的注释的文本，作为文档注释，跳过空白。*/
fun getDocTextFromPreviousComment(element: PsiElement): String {
	//我们认为当前元素之前，之间没有空行的非行尾行注释，可以视为文档注释，但这并非文档注释的全部
	val lines = mutableListOf<String>()
	var prevElement = element.prevSibling ?: element.parent?.prevSibling
	while(prevElement != null) {
		val text = prevElement.text
		if(prevElement !is PsiWhiteSpace) {
			if(!isPreviousComment(prevElement)) break
			val documentText = text.trimStart('#').trim().escapeXml()
			lines.add(0, documentText)
		} else {
			if(text.containsBlankLine()) break
		}
		// 兼容comment在rootBlock之外的特殊情况
		prevElement = prevElement.prevSibling
	}
	return lines.joinToString("<br>")
}

/**判断指定的注释是否可认为是之前的注释。*/
fun isPreviousComment(element: PsiElement): Boolean {
	val elementType = element.elementType
	return elementType == ParadoxLocalisationTypes.COMMENT || elementType == ParadoxScriptTypes.COMMENT
}

//Keys

val paradoxFileInfoKey = Key<ParadoxFileInfo>("paradoxFileInfo")
val cachedParadoxFileInfoKey = Key<CachedValue<ParadoxFileInfo>>("cachedParadoxFileInfo")
val cachedParadoxDefinitionInfoKey = Key<CachedValue<ParadoxDefinitionInfo>>("cachedParadoxDefinitionInfo")
val cachedParadoxDefinitionPropertyInfoKey = Key<CachedValue<ParadoxDefinitionPropertyInfo>>("cachedParadoxDefinitionPropertyInfo")
val cachedParadoxLocalisationInfoKey = Key<CachedValue<ParadoxLocalisationInfo>>("cachedParadoxLocalisationInfo")

//PsiElement Extensions

val ParadoxLocalisationLocale.paradoxLocale: ParadoxLocale?
	get() {
		val name = this.name
		return getConfig().localeMap[name]
	}

val ParadoxLocalisationPropertyReference.paradoxColor: ParadoxColor?
	get() {
		val colorId = this.propertyReferenceParameter?.text?.firstOrNull()
		if(colorId != null && colorId.isUpperCase()) {
			return getConfig().colorMap[colorId.toString()]
		}
		return null
	}

val ParadoxLocalisationSequentialNumber.paradoxSequentialNumber: ParadoxSequentialNumber?
	get() {
		val name = this.name
		return getConfig().sequentialNumberMap[name]
	}

val ParadoxLocalisationColorfulText.paradoxColor: ParadoxColor?
	get() {
		val name = this.name
		return getConfig().colorMap[name]
	}

//val ParadoxLocalisationCommandScope.paradoxCommandScope: ParadoxCommandScope?
//	get() {
//		val name = this.name.toCapitalizedWord() //忽略大小写，首字母大写
//		if(name.startsWith(eventTargetPrefix)) return null
//		return config.commandScopeMap[name]
//	}
//
//val ParadoxLocalisationCommandField.paradoxCommandField: ParadoxCommandField?
//	get() {
//		val name = this.name
//		return config.commandFieldMap[name]
//	}

val PsiElement.paradoxLocale: ParadoxLocale? get() = doGetLocale(this)

private fun doGetLocale(element: PsiElement): ParadoxLocale? {
	return when(val file = element.containingFile) {
		is ParadoxScriptFile -> inferParadoxLocale()
		is ParadoxLocalisationFile -> file.locale?.paradoxLocale
		else -> null
	}
}

val VirtualFile.paradoxFileInfo: ParadoxFileInfo? get() = this.getUserData(paradoxFileInfoKey)

val PsiFile.paradoxFileInfo: ParadoxFileInfo? get() = doGetFileInfo(this.originalFile) //使用原始文件

val PsiElement.paradoxFileInfo: ParadoxFileInfo? get() = doGetFileInfo(this.containingFile.originalFile) //使用原始文件

internal fun canGetFileInfo(file: PsiFile): Boolean {
	//paradoxScriptFile, paradoxLocalisationFile, ddsFile
	if(file is ParadoxScriptFile || file is ParadoxLocalisationFile) return true
	val extension = file.name.substringAfterLast('.').lowercase()
	if(extension == "dds") return true
	return false
}

private fun doGetFileInfo(file: PsiFile): ParadoxFileInfo? {
	if(!canGetFileInfo(file)) return null
	//尝试基于fileViewProvider得到fileInfo
	val quickFileInfo = file.getUserData(paradoxFileInfoKey)
	if(quickFileInfo != null) return quickFileInfo
	return CachedValuesManager.getCachedValue(file, cachedParadoxFileInfoKey) {
		val value = file.virtualFile?.getUserData(paradoxFileInfoKey) ?: resolveFileInfo(file)
		CachedValueProvider.Result.create(value, file)
	}
}

private fun resolveFileInfo(file: PsiFile): ParadoxFileInfo? {
	val fileType = getFileType(file) ?: return null
	val fileName = file.name
	val subPaths = mutableListOf(fileName)
	var currentFile = file.parent
	while(currentFile != null) {
		val rootType = getRootType(currentFile)
		val rootPath = currentFile.virtualFile.toNioPath()
		if(rootType != null) {
			val path = getPath(subPaths)
			val gameType = getGameType(currentFile) ?: ParadoxGameType.defaultValue()
			return ParadoxFileInfo(fileName, path, rootPath, fileType, rootType, gameType)
		}
		subPaths.add(0, currentFile.name)
		currentFile = currentFile.parent
	}
	return null
}

private fun getPath(subPaths: List<String>): ParadoxPath {
	return ParadoxPath(subPaths)
}

private fun getFileType(file: PsiFile): ParadoxFileType? {
	val fileName = file.name.lowercase()
	val fileExtension = fileName.substringAfterLast('.')
	return when {
		fileExtension in scriptFileExtensions -> ParadoxFileType.ParadoxScript
		fileExtension in localisationFileExtensions -> ParadoxFileType.ParadoxLocalisation
		else -> null
	}
}

private fun getRootType(file: PsiDirectory): ParadoxRootType? {
	if(!file.isDirectory) return null
	val fileName = file.name.lowercase()
	for(child in file.files) {
		val childName = child.name.lowercase()
		val childExpression = childName.substringAfterLast('.', "")
		when {
			//TODO 可能并不是这样命名，需要检查
			//childName in ParadoxGameType.exeFileNames -> return ParadoxRootType.Stdlib
			childExpression == "exe" -> return ParadoxRootType.Stdlib
			childName == descriptorFileName -> return ParadoxRootType.Mod
			fileName == ParadoxRootType.PdxLauncher.key -> return ParadoxRootType.PdxLauncher
			fileName == ParadoxRootType.PdxOnlineAssets.key -> return ParadoxRootType.PdxOnlineAssets
			fileName == ParadoxRootType.TweakerGuiAssets.key -> return ParadoxRootType.TweakerGuiAssets
		}
	}
	return null
}

private fun getGameType(file: PsiDirectory): ParadoxGameType? {
	if(!file.isDirectory) return null
	for(child in file.files) {
		val childName = child.name
		if(childName.startsWith('.')) {
			val gameType = ParadoxGameType.resolve(childName.drop(1))
			if(gameType != null) return gameType
		}
	}
	return null
}

val ParadoxDefinitionProperty.paradoxDefinitionInfo: ParadoxDefinitionInfo? get() = doGetDefinitionInfo(this)

private fun doGetDefinitionInfo(element: ParadoxDefinitionProperty): ParadoxDefinitionInfo? {
	return CachedValuesManager.getCachedValue(element, cachedParadoxDefinitionInfoKey) {
		val value = resolveDefinitionInfo(element)
		CachedValueProvider.Result.create(value, element)
	}
}

//这个方法有可能导致ProcessCanceledException，因为调用element.name导致！
private fun resolveDefinitionInfo(element: ParadoxDefinitionProperty): ParadoxDefinitionInfo? {
	//NOTE cwt文件中定义的definition的properyPath的minDepth是4（跳过3个rootKey）
	val propertyPath = element.resolvePropertyPath(4) ?: return null
	val fileInfo = element.paradoxFileInfo ?: return null
	val path = fileInfo.path
	val gameType = fileInfo.gameType
	val elementName = element.name ?: return null
	val project = element.project
	val configGroup = getConfig(project).getValue(gameType) //这里需要指定project
	return configGroup.resolveDefinitionInfo(element, elementName, path, propertyPath, fileInfo)
}

val ParadoxDefinitionProperty.paradoxDefinitionPropertyInfo: ParadoxDefinitionPropertyInfo? get() = doGetDefinitionPropertyInfo(this)

private fun doGetDefinitionPropertyInfo(element: ParadoxDefinitionProperty): ParadoxDefinitionPropertyInfo? {
	val definition = element.findParentDefinition() ?: return null
	return CachedValuesManager.getCachedValue(element, cachedParadoxDefinitionPropertyInfoKey) {
		val value = resolveDefinitionPropertyInfo(element)
		CachedValueProvider.Result.create(value, element, definition)
	}
}

private fun resolveDefinitionPropertyInfo(element: ParadoxDefinitionProperty): ParadoxDefinitionPropertyInfo? {
	//注意这里要获得的definitionProperty可能是scriptFile也可能是scriptProperty
	val subPaths = mutableListOf<String>()
	val subPathInfos = mutableListOf<ParadoxPropertyPathInfo>()
	var current: PsiElement = element
	do {
		if(current is ParadoxDefinitionProperty) {
			val definitionInfo = current.paradoxDefinitionInfo
			val name = current.name ?: return null
			if(definitionInfo != null) {
				val propertiesCardinality = current.properties.groupAndCountBy { it.name.lowercase() } //这里名字要忽略大小写
				val path = ParadoxPropertyPath(subPaths, subPathInfos)
				return ParadoxDefinitionPropertyInfo(name, path, propertiesCardinality, definitionInfo)
			}
			subPaths.add(0, name)
		}
		current = current.parent ?: break
	} while(current !is PsiFile)
	return null
}

fun ParadoxScriptValue.getType(): String? {
	return when(this) {
		is ParadoxScriptBlock -> when {
			this.isEmpty -> "array | object"
			this.isArray -> "array"
			this.isObject -> "object"
			else -> null
		}
		is ParadoxScriptString -> "string"
		is ParadoxScriptBoolean -> "boolean"
		is ParadoxScriptInt -> "int"
		is ParadoxScriptFloat -> "float"
		is ParadoxScriptNumber -> "number"
		is ParadoxScriptColor -> "color"
		is ParadoxScriptCode -> "code"
		else -> null
	}
}

fun ParadoxScriptValue.checkType(type: String): Boolean {
	return when(type) {
		"block", "array | object" -> this is ParadoxScriptBlock
		"array" -> this is ParadoxScriptBlock && isArray
		"object" -> this is ParadoxScriptBlock && isObject
		"string" -> this is ParadoxScriptString
		"boolean" -> this is ParadoxScriptBoolean
		"int" -> this is ParadoxScriptInt
		"float" -> this is ParadoxScriptFloat
		"number" -> this is ParadoxScriptNumber
		"color" -> this is ParadoxScriptColor
		"code" -> this is ParadoxScriptCode
		else -> false
	}
}

fun ParadoxScriptValue.isNullLike(): Boolean {
	return when {
		this is ParadoxScriptBlock -> this.isEmpty || this.isAlwaysYes() //兼容always=yes
		this is ParadoxScriptString -> this.textMatches("")
		this is ParadoxScriptNumber -> this.text.toIntOrNull() == 0 //兼容0.0和0.00这样的情况
		this is ParadoxScriptBoolean -> this.textMatches("no")
		else -> false
	}
}

fun ParadoxScriptBlock.isAlwaysYes(): Boolean {
	return this.isObject && this.propertyList.singleOrNull()?.let { it.name == "always" && it.value == "yes" } ?: false
}

fun PsiElement.resolvePath(): ParadoxPath? {
	val subPaths = mutableListOf<String>()
	var current = this
	while(current !is PsiFile && current !is ParadoxScriptRootBlock) {
		when {
			current is ParadoxScriptProperty -> {
				subPaths.add(0, current.name)
			}
			current is ParadoxScriptValue -> {
				val parent = current.parent ?: break
				if(parent is ParadoxScriptBlock) {
					subPaths.add(0, parent.indexOfChild(current).toString())
				}
				current = parent
			}
		}
		current = current.parent ?: break
	}
	return if(subPaths.isEmpty()) null else ParadoxPath(subPaths)
}

fun PsiElement.resolvePropertyPath(maxDepth: Int = -1): ParadoxPropertyPath? {
	val subPaths = mutableListOf<String>()
	val subPathInfos = emptyList<ParadoxPropertyPathInfo>() //TODO 目前不需要获取
	var current = this
	var depth = 0
	while(current !is PsiFile && current !is ParadoxScriptRootBlock) {
		when {
			current is ParadoxScriptProperty -> {
				subPaths.add(0, current.name)
				depth++
			}
			//忽略scriptValue
		}
		//如果发现深度超出指定的最大深度，则直接返回null
		if(maxDepth != -1 && maxDepth < depth) return null
		current = current.parent ?: break
	}
	return ParadoxPropertyPath(subPaths, subPathInfos)
}

val ParadoxLocalisationProperty.paradoxLocalisationInfo: ParadoxLocalisationInfo? get() = doGetLocalisationInfo(this)

private fun doGetLocalisationInfo(element: ParadoxLocalisationProperty): ParadoxLocalisationInfo? {
	return CachedValuesManager.getCachedValue(element, cachedParadoxLocalisationInfoKey) {
		val value = resolveLocalisationInfo(element)
		CachedValueProvider.Result.create(value, element)
	}
}

private fun resolveLocalisationInfo(element: ParadoxLocalisationProperty): ParadoxLocalisationInfo? {
	val name = element.name
	val type = ParadoxLocalisationCategory.resolve(element) ?: return null
	return ParadoxLocalisationInfo(name, type)
}

/**
 * 判断当前localisation所在的根目录是否是"localisation"或"localisation_synced"
 */
fun ParadoxLocalisationProperty.isInValidDirectory(): Boolean {
	return this.paradoxFileInfo?.path?.root.let { it != null && it == "localisation" || it == "localisation_synced" }
}

/**
 * 判断当前localisation所在的根目录是否是"localisation"
 */
fun ParadoxLocalisationProperty.isLocalisation(): Boolean {
	return this.paradoxFileInfo?.path?.root == "localisation"
}

/**
 * 判断当前localisation所在的根目录是否是"localisation_synced"
 */
fun ParadoxLocalisationProperty.isLocalisationSynced(): Boolean {
	return this.paradoxFileInfo?.path?.root == "localisation_synced"
}

fun PsiElement.isQuoted(): Boolean {
	return firstLeafOrSelf.text.startsWith('"') //判断第一个叶子节点或本身的文本是否以引号开头
}

//PsiElement Find Extensions

fun ParadoxDefinitionProperty.findProperty(propertyName: String, ignoreCase: Boolean = false): ParadoxScriptProperty? {
	return properties.find { it.name.equals(propertyName, ignoreCase) }
}

fun ParadoxDefinitionProperty.findProperties(propertyName: String, ignoreCase: Boolean = false): List<ParadoxScriptProperty> {
	return properties.filter { it.name.equals(propertyName, ignoreCase) }
}

fun ParadoxDefinitionProperty.findValue(value: String, ignoreCase: Boolean = false): ParadoxScriptValue? {
	return values.find { it.value.equals(value, ignoreCase) }
}

fun ParadoxDefinitionProperty.findValues(value: String, ignoreCase: Boolean = false): List<ParadoxScriptValue> {
	return values.filter { it.value.equals(value, ignoreCase) }
}

fun ParadoxScriptBlock.findProperty(propertyName: String, ignoreCase: Boolean = false): ParadoxScriptProperty? {
	return propertyList.find { it.name.equals(propertyName, ignoreCase) }
}

fun ParadoxScriptBlock.findProperties(propertyName: String, ignoreCase: Boolean = false): List<ParadoxScriptProperty> {
	return propertyList.filter { it.name.equals(propertyName, ignoreCase) }
}

fun ParadoxScriptBlock.findValue(value: String, ignoreCase: Boolean = false): ParadoxScriptValue? {
	return valueList.find { it.value.equals(value, ignoreCase) }
}

fun ParadoxScriptBlock.findValues(value: String, ignoreCase: Boolean = false): List<ParadoxScriptValue> {
	return valueList.filter { it.value.equals(value, ignoreCase) }
}

/**
 * 得到上一级definitionProperty，可能为自身，可能为null，可能也是definition。
 */
fun PsiElement.findParentDefinitionProperty(): ParadoxDefinitionProperty? {
	var current: PsiElement = this
	do {
		if(current is ParadoxScriptRootBlock) {
			return (current.parent ?: break) as ParadoxDefinitionProperty
		} else if(current is ParadoxScriptBlock) {
			return (current.parent.parent ?: break) as ParadoxDefinitionProperty
		}
		current = current.parent ?: break
	} while(current !is PsiFile)
	return null
}

/**
 * 得到上一级definition，可能为自身，可能为null。
 */
fun PsiElement.findParentDefinition(): ParadoxDefinitionProperty? {
	var current: PsiElement = this
	do {
		if(current is ParadoxDefinitionProperty) {
			val definitionInfo = current.paradoxDefinitionInfo
			if(definitionInfo != null) return current
		}
		current = current.parent ?: break
	} while(current !is PsiFile)
	return null
}

//Find Extensions

/**
 * 根据名字在当前文件中递归查找脚本变量（scriptedVariable）。（不一定定义在顶层）
 */
fun findScriptVariableInFile(name: String, file: PsiFile): ParadoxScriptVariable? {
	if(file !is ParadoxScriptFile) return null
	return file.descendantsOfType<ParadoxScriptVariable>().find { it.name == name }
}

/**
 * 根据名字在当前文件中递归查找所有的脚本变量（scriptedVariable）。（不一定定义在顶层）
 */
fun findScriptVariablesInFile(name: String, file: PsiFile): List<ParadoxScriptVariable> {
	if(file !is ParadoxScriptFile) return emptyList()
	return file.descendantsOfType<ParadoxScriptVariable>().filter { it.name == name }.toList()
}

/**
 * 在当前文件中递归查找所有的脚本变量（scriptedVariable）。（不一定定义在顶层）
 */
fun findScriptVariablesInFile(file: PsiFile): List<ParadoxScriptVariable> {
	//在所在文件中递归查找（不一定定义在顶层）
	if(file !is ParadoxScriptFile) return emptyList()
	return file.descendantsOfType<ParadoxScriptVariable>().toList()
}

/**
 * 基于脚本变量名字索引，根据名字查找脚本变量（scriptedVariable）。
 */
fun findScriptVariable(
	name: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): ParadoxScriptVariable? {
	return ParadoxScriptVariableNameIndex.getOne(name, project, scope, !getSettings().preferOverridden)
}

/**
 * 基于脚本变量名字索引，根据名字查找所有的脚本变量（scriptedVariable）。
 */
fun findScriptVariables(
	name: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptVariable> {
	return ParadoxScriptVariableNameIndex.getAll(name, project, scope)
}

/**
 * 基于脚本变量名字索引，查找所有的脚本变量（scriptedVariable）。
 */
fun findScriptVariables(
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptVariable> {
	return ParadoxScriptVariableNameIndex.getAll(project, scope)
}

/**
 * 基于脚本变量名字索引，过滤所有的脚本变量（scriptedVariable）。
 */
fun filterScriptVariables(
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	predicate: (String) -> Boolean
): List<ParadoxScriptVariable> {
	return ParadoxScriptVariableNameIndex.filter(project, scope, predicate)
}

/**
 * 基于定义名字索引，根据名字、类型表达式判断是否存在脚本文件的定义（definition）。
 */
fun hasDefinition(
	name: String,
	typeExpression: String? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): Boolean {
	return ParadoxDefinitionNameIndex.exists(name, typeExpression, project, scope)
}

/**
 * 基于定义名字索引，根据名字、类型表达式查找脚本文件的定义（definition）。
 */
fun findDefinition(
	name: String,
	typeExpression: String? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): ParadoxScriptProperty? {
	return ParadoxDefinitionNameIndex.getOne(name, typeExpression, project, scope, !getSettings().preferOverridden)
}

/**
 * 基于定义名字索引，根据名字、类型表达式查找所有的脚本文件的定义（definition）。
 */
fun findDefinitions(
	name: String,
	typeExpression: String? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionNameIndex.getAll(name, typeExpression, project, scope)
}

/**
 * 基于定义名字索引，根据类型表达式查找所有的脚本文件的定义（definition）。
 */
fun findDefinitions(
	typeExpression: String? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionNameIndex.getAll(typeExpression, project, scope)
}

/**
 * 基于定义名字索引，根据类型表达式查找并根据名字过滤所有的脚本文件的定义（definition）。
 */
fun filterDefinitions(
	typeExpression: String? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	predicate: (String) -> Boolean
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionNameIndex.filter(typeExpression, project, scope, predicate)
}

/**
 * 基于定义类型索引，根据名字和类型（不是类型表达式）查找所有的脚本文件的定义（definition）。
 */
fun findDefinitionByType(
	name: String,
	type: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): ParadoxScriptProperty? {
	return ParadoxDefinitionTypeIndex.getOne(name, type, project, scope, !getSettings().preferOverridden)
}

/**
 * 基于定义类型索引，根据名字和类型（不是类型表达式）查找所有的脚本文件的定义（definition）。
 */
fun findDefinitionsByType(
	name: String,
	type: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionTypeIndex.getAll(name, type, project, scope)
}

/**
 * 基于定义类型索引，根据类型（不是类型表达式）查找所有的脚本文件的定义（definition）。
 */
fun findDefinitionsByType(
	type: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionTypeIndex.getAll(type, project, scope)
}

/**
 * 基于定义蕾西索引，根据类型（不是类型表达式）查找并根据名字过滤所有的脚本文件的定义（definition）。
 */
fun filterDefinitionsByType(
	type: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	predicate: (String) -> Boolean
): List<ParadoxScriptProperty> {
	return ParadoxDefinitionTypeIndex.filter(type, project, scope, predicate)
}

/**
 * 基于本地化名字索引，根据名字、语言区域判断是否存在本地化（localisation）。
 */
fun hasLocalisation(
	name: String,
	locale: ParadoxLocale?,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
): Boolean {
	return ParadoxLocalisationNameIndex.exists(name, locale, project, scope)
}

/**
 * 基于本地化名字索引，根据名字、语言区域查找本地化（localisation）。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findLocalisation(
	name: String,
	locale: ParadoxLocale?,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false
): ParadoxLocalisationProperty? {
	return ParadoxLocalisationNameIndex.getOne(name, locale, project, scope, hasDefault, !getSettings().preferOverridden)
}

/**
 * 基于本地化名字索引，根据名字、语言区域查找所有的本地化（localisation）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findLocalisations(
	name: String,
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = true
): List<ParadoxLocalisationProperty> {
	return ParadoxLocalisationNameIndex.getAll(name, locale, project, scope, hasDefault)
}

/**
 * 基于本地化名字索引，根据语言区域查找所有的本地化（localisation）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findLocalisations(
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false
): List<ParadoxLocalisationProperty> {
	return ParadoxLocalisationNameIndex.getAll(locale, project, scope, hasDefault)
}

/**
 * 基于本地化名字索引，根据语言区域查找且根据名字过滤所有的本地化（localisation）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun filterLocalisations(
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false,
	predicate: (String) -> Boolean
): List<ParadoxLocalisationProperty> {
	return ParadoxLocalisationNameIndex.filter(locale, project, scope, hasDefault, predicate)
}

/**
 * 基于本地化名字索引，根据关键字查找所有的本地化（localisation）。
 * * 如果名字包含关键字（不忽略大小写），则放入结果。
 * * 返回的结果有数量限制。
 */
fun findLocalisationsByKeyword(
	keyword: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	maxSize: Int = -1
): List<ParadoxLocalisationProperty> {
	return ParadoxLocalisationNameIndex.findByKeyword(keyword, project, scope, maxSize)
}

/**
 * 基于本地化名字索引，根据一组名字、语言区域查找所有的本地化（localisation）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 * * 如果[keepOrder]为`true`，则根据这组名字排序查询结果。
 */
fun findLocalisationsByNames(
	names: Iterable<String>,
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false,
	keepOrder: Boolean = false
): List<ParadoxLocalisationProperty> {
	return ParadoxLocalisationNameIndex.findByNames(names, locale, project, scope, hasDefault, keepOrder)
}

/**
 * 基于本地化名字索引，根据名字、语言区域判断是否存在同步本地化（localisation_synced）。
 */
fun hasSyncedLocalisation(
	name: String,
	locale: ParadoxLocale?,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
): Boolean {
	return ParadoxSyncedLocalisationNameIndex.exists(name, locale, project, scope)
}

/**
 * 基于同步本地化名字索引，根据名字、语言区域查找同步本地化（localisation_synced）。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findSyncedLocalisation(
	name: String,
	locale: ParadoxLocale?,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false
): ParadoxLocalisationProperty? {
	return ParadoxSyncedLocalisationNameIndex.getOne(name, locale, project, scope, hasDefault, !getSettings().preferOverridden)
}

/**
 * 基于同步本地化名字索引，根据名字、语言区域查找所有的同步本地化（localisation_synced）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findSyncedLocalisations(
	name: String,
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = true
): List<ParadoxLocalisationProperty> {
	return ParadoxSyncedLocalisationNameIndex.getAll(name, locale, project, scope, hasDefault)
}

/**
 * 基于同步本地化名字索引，根据语言区域查找所有的同步本地化（localisation_synced）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun findSyncedLocalisations(
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false
): List<ParadoxLocalisationProperty> {
	return ParadoxSyncedLocalisationNameIndex.getAll(locale, project, scope, hasDefault)
}

/**
 * 基于同步本地化名字索引，根据语言区域查找且根据名字过滤所有的同步本地化（localisation_synced）。
 * * 如果[locale]为`null`，则将用户的语言区域对应的本地化放到该组的最前面。
 * * 如果[hasDefault]为`true`，且没有查找到对应语言区域的本地化，则忽略语言区域。
 */
fun filterSyncedLocalisations(
	locale: ParadoxLocale? = null,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	hasDefault: Boolean = false,
	predicate: (String) -> Boolean
): List<ParadoxLocalisationProperty> {
	return ParadoxSyncedLocalisationNameIndex.filter(locale, project, scope, hasDefault, predicate)
}

/**
 * 基于同步本地化名字索引，根据关键字查找所有的同步本地化（localisation_synced）。
 * * 如果名字包含关键字（不忽略大小写），则放入结果。
 * * 返回的结果有数量限制。
 */
fun findSyncedLocalisationsByKeyword(
	keyword: String,
	project: Project,
	scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
	maxSize: Int = -1
): List<ParadoxLocalisationProperty> {
	return ParadoxSyncedLocalisationNameIndex.findByKeyword(keyword, project, scope, maxSize)
}

//Link Extensions


fun resolveLink(link: String, context: PsiElement): PsiElement? {
	return when {
		link.startsWith('@') -> resolveCwtLink(link, context)
		link.startsWith('$') -> resolveScriptLink(link, context)
		link.startsWith('#') -> resolveLocalisationLink(link, context)
		else -> null
	}
}

//@stellaris.types.building
private fun resolveCwtLink(link: String, context: PsiElement): CwtProperty? {
	return runCatching {
		val project = context.project
		val tokens = link.drop(1).split('.')
		val gameType = tokens[0]
		val configType = tokens[1]
		val name = tokens[2]
		val extraName = tokens.getOrNull(3) //可能是subtypeName
		//如果configType是types且extraName存在，需要特殊处理，从而兼容subtype
		if(configType == "types" && extraName != null) {
			getConfig(project).getValue(gameType).types.getValue(name).subtypes.getValue(extraName).pointer.element
		} else {
			getConfig(project).getValue(gameType).getValue(configType).getValue(name).pointer.element
		}
	}.getOrNull()
}

//$ethos.ethic_authoritarian, $job.head_researcher
private fun resolveScriptLink(link: String, context: PsiElement): ParadoxScriptProperty? {
	return runCatching {
		val project = context.project
		val tokens = link.drop(1).split('.')
		val type = tokens[0]
		val name = tokens[1]
		findDefinitionByType(name, type, project)
	}.getOrNull()
}

//#NAME, #KEY
private fun resolveLocalisationLink(link: String, context: PsiElement): ParadoxLocalisationProperty? {
	return runCatching {
		val token = link.drop(1)
		return findLocalisation(token, context.paradoxLocale, context.project, hasDefault = true)
	}.getOrNull()
}

//Build String Extensions

fun StringBuilder.appendIf(condition: Boolean, text: String): StringBuilder {
	if(condition) append(text)
	return this
}

fun StringBuilder.appendPsiLink(refText: String, label: String, plainLink: Boolean = false): StringBuilder {
	DocumentationManagerUtil.createHyperlink(this, refText, label, plainLink)
	return this
}

fun StringBuilder.appendScriptLink(name: String, type: String): StringBuilder {
	if(name.isEmpty()) return append(unresolvedEscapedString) //如果target为空，需要特殊处理
	return appendPsiLink("$$type.$name", name)
}

fun StringBuilder.appendLocalisationLink(name: String): StringBuilder {
	if(name.isEmpty()) return append(unresolvedEscapedString) //如果target为空，需要特殊处理
	return appendPsiLink("#$name", name)
}

fun StringBuilder.appendIconTag(url: String, local: Boolean = true): StringBuilder {
	return append("<img src=\"").appendIf(local, "file:/").append(url).append("\" hspace=\"1\"/>")
}

fun StringBuilder.appendIconTag(url: String, size: Int, local: Boolean = true): StringBuilder {
	return append("<img src=\"").appendIf(local, "file:/").append(url)
		.append("\" width=\"").append(size).append("\" height=\"").append(size).append("\" hspace=\"1\"/>")
}

fun StringBuilder.appendFileInfo(fileInfo: ParadoxFileInfo): StringBuilder {
	return append("[").append(fileInfo.path).append("]")
}

fun StringBuilder.appendBr(): StringBuilder {
	return append("<br>")
}

//Inline Extensions

inline fun message(@PropertyKey(resourceBundle = bundleName) key: String, vararg params: Any): String {
	return PlsBundle.getMessage(key, *params)
}

inline fun String.resolveIconUrl(project: Project, defaultToUnknown: Boolean = true): String {
	return ParadoxIconUrlResolver.resolveByName(this, project, defaultToUnknown)
}

inline fun ParadoxScriptProperty.resolveIconUrl(defaultToUnknown: Boolean = true): String {
	return ParadoxIconUrlResolver.resolveBySprite(this, defaultToUnknown)
}

inline fun VirtualFile.resolveIconUrl(defaultToUnknown: Boolean = true): String {
	return ParadoxIconUrlResolver.resolveByFile(this, defaultToUnknown)
}

inline fun PsiFile.resolveIconUrl(defaultToUnknown: Boolean = true): String {
	return ParadoxIconUrlResolver.resolveByFile(this, defaultToUnknown)
}

inline fun ParadoxLocalisationProperty.renderText(): String {
	return ParadoxLocalisationTextRenderer.render(this)
}

inline fun ParadoxLocalisationProperty.renderTextTo(buffer: StringBuilder) {
	ParadoxLocalisationTextRenderer.renderTo(this, buffer)
}

inline fun ParadoxLocalisationProperty.extractText(): String {
	return ParadoxLocalisationTextExtractor.extract(this)
}

inline fun ParadoxLocalisationProperty.extractTextTo(buffer: StringBuilder) {
	ParadoxLocalisationTextExtractor.extractTo(this, buffer)
}

inline fun CwtFile.resolveConfig(): CwtFileConfig {
	return CwtConfigResolver.resolve(this)
}

inline fun ParadoxScriptFile.resolveData(): List<Any> {
	return ParadoxScriptDataResolver.resolve(this)
}

inline fun ParadoxLocalisationFile.resolveData(): Map<String, String> {
	return ParadoxLocalisationDataResolver.resolve(this)
}