package icu.windea.pls.lang.cwt.expression

import com.google.common.cache.*
import com.intellij.psi.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.search.selector.*
import icu.windea.pls.core.util.*
import icu.windea.pls.dds.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.cwt.config.*
import icu.windea.pls.model.*
import icu.windea.pls.script.psi.*
import icu.windea.pls.tool.*

private val validValueTypes = arrayOf(
    CwtDataType.FilePath,
    CwtDataType.Icon,
    CwtDataType.Definition
)

/**
 * CWT图片位置表达式。
 *
 * 用于定位定义的相关图片。
 *
 * 如果包含占位符`$`，将其替换成定义的名字后，尝试得到对应路径的图片，否则尝试得到对应名字的属性的值对应的图片。
 *
 * 示例：`"gfx/interface/icons/modifiers/mod_$.dds"`, "GFX_$", `"icon"`, "icon|p1,p2"`
 *
 * @property placeholder 占位符文本。其中的`"$"`会在解析时被替换成定义的名字。
 * @property propertyName 属性名，用于获取图片的引用文本。
 * @property framePropertyNames 属性名，用于获取帧数。帧数用于后续切分图片。
 */
class CwtImageLocationExpression private constructor(
    expressionString: String,
    val placeholder: String? = null,
    val propertyName: String? = null,
    val framePropertyNames: List<String>? = null,
) : AbstractExpression(expressionString), CwtExpression {
    fun resolvePlaceholder(name: String): String? {
        if(placeholder == null) return null
        return buildString { for(c in placeholder) if(c == '$') append(name) else append(c) }
    }
    
    data class ResolveResult(
        val nameOrFilePath: String,
        val element: PsiElement?,
        val frameInfo: FrameInfo? = null,
        val message: String? = null
    )
    
    fun resolve(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, frameInfo: FrameInfo? = null, toFile: Boolean = false): ResolveResult? {
        val project = definitionInfo.project
        var newFrameInfo = frameInfo
        if(definitionInfo.type == "sprite") {
            newFrameInfo = newFrameInfo.merge(ParadoxSpriteHandler.getFrameInfo(definition))
        }
        if(placeholder != null) {
            if(definitionInfo.name.isEmpty()) return null //ignore anonymous definitions
            if(placeholder.startsWith("GFX_")) {
                val spriteName = resolvePlaceholder(definitionInfo.name)!!
                if(!toFile) {
                    val selector = definitionSelector(project, definition).contextSensitive()
                    val resolved = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).find()
                    return ResolveResult(spriteName, resolved, newFrameInfo)
                }
                val selector = definitionSelector(project, definition).contextSensitive()
                val resolved = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).find()
                val resolvedDefinition = resolved ?: return null
                val resolvedDefinitionInfo = resolved.definitionInfo ?: return null
                val primaryImageConfigs = resolvedDefinitionInfo.primaryImages
                if(primaryImageConfigs.isEmpty()) return null //没有或者CWT规则不完善
                return primaryImageConfigs.firstNotNullOfOrNull { primaryImageConfig ->
                    val locationExpression = primaryImageConfig.locationExpression
                    val r = locationExpression.resolve(resolvedDefinition, resolvedDefinitionInfo, newFrameInfo, toFile)
                    r?.takeIf { it.element != null || it.message != null }
                }
            }
            //假定这里的filePath以.dds结尾
            val filePath = resolvePlaceholder(definitionInfo.name)!!
            val selector = fileSelector(project, definition).contextSensitive()
            val file = ParadoxFilePathSearch.search(filePath, null, selector).find()?.toPsiFile(project)
            return ResolveResult(filePath, file, newFrameInfo)
        } else if(propertyName != null) {
            //propertyName可以为空字符串，这时直接查找定义的字符串类型的值（如果存在）
            val property = definition.findProperty(propertyName, conditional = true, inline = true) ?: return null
            val propertyValue = property.propertyValue ?: return null
            val config = ParadoxConfigHandler.getConfigs(propertyValue, orDefault = false).firstOrNull() as? CwtValueConfig ?: return null
            if(config.expression.type !in validValueTypes) {
                return ResolveResult("", null, null, PlsBundle.message("dynamic"))
            }
            if(propertyValue.text.isParameterized()) {
                return ResolveResult("", null, null, PlsBundle.message("parameterized"))
            }
            val name = propertyValue.value
            if(definitionInfo.name.equals(name, true)) return null //防止出现SOF
            val resolved = ParadoxConfigHandler.resolveScriptExpression(propertyValue, null, config, config.expression, config.info.configGroup, false)
            when {
                //由filePath解析为图片文件
                resolved is PsiFile && resolved.fileType == DdsFileType -> {
                    val filePath = resolved.fileInfo?.path?.path ?: return null
                    val selector = fileSelector(project, definition).contextSensitive()
                    val file = ParadoxFilePathSearch.search(filePath, null, selector).find()
                        ?.toPsiFile(project)
                    return ResolveResult(filePath, file, newFrameInfo)
                }
                //由name解析为定义（如果不是sprite，就继续向下解析）
                resolved is ParadoxScriptDefinitionElement -> {
                    val resolvedDefinition = resolved
                    val resolvedDefinitionInfo = resolved.definitionInfo ?: return null
                    if(!toFile && resolvedDefinitionInfo.type == "sprite") {
                        val spriteName = resolvedDefinitionInfo.name
                        val selector = definitionSelector(project, definition).contextSensitive()
                        val r = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).find() ?: return null
                        return ResolveResult(spriteName, r, newFrameInfo)
                    }
                    val primaryImageConfigs = resolvedDefinitionInfo.primaryImages
                    if(primaryImageConfigs.isEmpty()) return null //没有或者CWT规则不完善
                    return primaryImageConfigs.firstNotNullOfOrNull { primaryImageConfig ->
                        val locationExpression = primaryImageConfig.locationExpression
                        val r = locationExpression.resolve(resolvedDefinition, resolvedDefinitionInfo, newFrameInfo, toFile)
                        r?.takeIf { it.element != null || it.message != null }
                    }
                }
                else -> return null //解析失败或不支持
            }
        } else {
            throw IllegalStateException() //不期望的结果
        }
    }
    
    data class ResolveAllResult(
        val nameOrFilePath: String,
        val elements: Set<PsiElement>,
        val frameInfo: FrameInfo? = null,
        val message: String? = null
    )
    
    fun resolveAll(definition: ParadoxScriptDefinitionElement, definitionInfo: ParadoxDefinitionInfo, frameInfo: FrameInfo? = null, toFile: Boolean = false): ResolveAllResult? {
        val project = definitionInfo.project
        var newFrameInfo = frameInfo
        if(definitionInfo.type == "sprite") {
            newFrameInfo = newFrameInfo.merge(ParadoxSpriteHandler.getFrameInfo(definition))
        }
        if(placeholder != null) {
            if(definitionInfo.name.isEmpty()) return null //ignore anonymous definitions
            if(placeholder.startsWith("GFX_")) {
                val spriteName = resolvePlaceholder(definitionInfo.name)!!
                if(!toFile) {
                    val selector = definitionSelector(project, definition).contextSensitive()
                    val resolved = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).findAll()
                    return ResolveAllResult(spriteName, resolved, newFrameInfo)
                }
                val selector = definitionSelector(project, definition).contextSensitive()
                val resolved = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).find()
                val resolvedDefinition = resolved ?: return null
                val resolvedDefinitionInfo = resolved.definitionInfo ?: return null
                val primaryImageConfigs = resolvedDefinitionInfo.primaryImages
                if(primaryImageConfigs.isEmpty()) return null //没有或者CWT规则不完善
                var resolvedFilePath: String? = null
                var resolvedElements: MutableSet<PsiElement>? = null
                for(primaryImageConfig in primaryImageConfigs) {
                    val locationExpression = primaryImageConfig.locationExpression
                    val r = locationExpression.resolveAll(resolvedDefinition, resolvedDefinitionInfo, newFrameInfo, toFile) ?: continue
                    if(r.message != null) return r
                    val (filePath, elements) = r
                    if(resolvedFilePath == null) resolvedFilePath = filePath
                    if(resolvedElements == null) resolvedElements = mutableSetOf()
                    resolvedElements.addAll(elements)
                }
                if(resolvedFilePath == null) return null
                return ResolveAllResult(resolvedFilePath, resolvedElements ?: emptySet(), newFrameInfo)
            }
            //假定这里的filePath以.dds结尾
            val filePath = resolvePlaceholder(definitionInfo.name)!!
            val selector = fileSelector(project, definition).contextSensitive()
            val files = ParadoxFilePathSearch.search(filePath, null, selector).findAll()
                .mapNotNullTo(mutableSetOf()) { it.toPsiFile(project) }
            return ResolveAllResult(filePath, files, newFrameInfo)
        } else if(propertyName != null) {
            //dynamic -> returns ("", null, 0)
            val property = definition.findProperty(propertyName, inline = true) ?: return null
            val propertyValue = property.propertyValue ?: return null
            val config = ParadoxConfigHandler.getConfigs(propertyValue, orDefault = false).firstOrNull() as? CwtValueConfig ?: return null
            if(config.expression.type !in validValueTypes) {
                return ResolveAllResult("", emptySet(), null, PlsBundle.message("dynamic"))
            }
            if(propertyValue.text.isParameterized()) {
                return ResolveAllResult("", emptySet(), null, PlsBundle.message("parameterized"))
            }
            val name = propertyValue.value
            if(definitionInfo.name.equals(name, true)) return null //防止出现SOF
            val resolved = ParadoxConfigHandler.resolveScriptExpression(propertyValue, null, config, config.expression, config.info.configGroup, false)
            when {
                //由filePath解析为图片文件
                resolved is PsiFile && resolved.fileType == DdsFileType -> {
                    val filePath = resolved.fileInfo?.path?.path ?: return null
                    val selector = fileSelector(project, definition).contextSensitive()
                    val files = ParadoxFilePathSearch.search(filePath, null, selector).findAll()
                        .mapNotNullTo(mutableSetOf()) { it.toPsiFile(project) }
                    return ResolveAllResult(filePath, files, newFrameInfo)
                }
                //由name解析为定义（如果不是sprite，就继续向下解析）
                resolved is ParadoxScriptDefinitionElement -> {
                    val resolvedDefinition = resolved
                    val resolvedDefinitionInfo = resolved.definitionInfo ?: return null
                    if(!toFile && resolvedDefinitionInfo.type == "sprite") {
                        val spriteName = resolvedDefinitionInfo.name
                        val selector = definitionSelector(project, definition).contextSensitive()
                        val r = ParadoxDefinitionSearch.search(spriteName, "sprite", selector).findAll()
                        return ResolveAllResult(spriteName, r, newFrameInfo)
                    }
                    val primaryImageConfigs = resolvedDefinitionInfo.primaryImages
                    if(primaryImageConfigs.isEmpty()) return null //没有或者CWT规则不完善
                    var resolvedFilePath: String? = null
                    var resolvedElements: MutableSet<PsiElement>? = null
                    for(primaryImageConfig in primaryImageConfigs) {
                        val locationExpression = primaryImageConfig.locationExpression
                        val r = locationExpression.resolveAll(resolvedDefinition, resolvedDefinitionInfo, newFrameInfo, toFile) ?: continue
                        if(r.message != null) return r
                        val (filePath, elements) = r
                        if(resolvedFilePath == null) resolvedFilePath = filePath
                        if(resolvedElements == null) resolvedElements = mutableSetOf()
                        resolvedElements.addAll(elements)
                    }
                    if(resolvedFilePath == null) return null
                    return ResolveAllResult(resolvedFilePath, resolvedElements ?: emptySet(), newFrameInfo)
                }
                else -> return null //解析失败或不支持
            }
        } else {
            throw IllegalStateException() //不期望的结果
        }
    }
    
    companion object Resolver {
        val EmptyExpression = CwtImageLocationExpression("", propertyName = "")
        
        private val cache = CacheBuilder.newBuilder().buildCache<String, CwtImageLocationExpression> { doResolve(it) }
        
        fun resolve(expressionString: String): CwtImageLocationExpression {
            return cache.get(expressionString)
        }
        
        private fun doResolve(expressionString: String): CwtImageLocationExpression {
            return when {
                expressionString.isEmpty() -> EmptyExpression
                expressionString.contains('$') -> {
                    val placeholder = expressionString
                    CwtImageLocationExpression(expressionString, placeholder = placeholder)
                }
                else -> {
                    val propertyName = expressionString.substringBefore('|').intern()
                    val framePropertyNames = expressionString.substringAfter('|', "").takeIfNotEmpty()
                        ?.toCommaDelimitedStringList()
                    CwtImageLocationExpression(expressionString, propertyName = propertyName, framePropertyNames = framePropertyNames)
                }
            }
        }
    }
}
