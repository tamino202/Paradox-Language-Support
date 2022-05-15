package icu.windea.pls.tool

import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.dds.*
import icu.windea.pls.script.psi.*
import org.slf4j.*
import java.lang.invoke.*
import kotlin.io.path.*

/**
 * DDS图片地址的解析器。
 */
@Suppress("unused")
object ParadoxDdsUrlResolver {
	private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
	
	/**
	 * 基于定义进行解析。定义类型可以不为sprite。返回对应的PNG图片的绝对路径。
	 */
	fun resolveByDefinition(definition: ParadoxDefinitionProperty, defaultToUnknown: Boolean = false): String {
		val definitionInfo = definition.definitionInfo ?: return getDefaultUrl(defaultToUnknown)
		try {
			//如果无法解析为png文件地址，则返回默认的地址
			val url = doResolveByDefinition(definition, definitionInfo)
			if(url.isNullOrEmpty()) return getDefaultUrl(defaultToUnknown)
			return url
		} catch(e: Exception) {
			//如果出现异常，那么返回默认图标
			logger.warn(e) { "Resolve dds url failed. (definition name: ${definition.name})" }
			return getDefaultUrl(defaultToUnknown)
		}
	}
	
	/**
	 * 直接基于dds文件进行解析。返回对应的PNG图片的绝对路径。
	 */
	fun resolveByFile(file: VirtualFile, frame: Int = 0, defaultToUnknown: Boolean = false): String {
		try {
			//如果无法解析为png文件地址，则返回默认的地址
			val url = doResolveByFile(file, frame)
			if(url.isNullOrEmpty()) return getDefaultUrl(defaultToUnknown)
			return url
		} catch(e: Exception) {
			//如果出现异常，那么返回默认图标
			logger.warn(e) { "Resolve dds url failed. (dds file path: ${file.path})" }
			return getDefaultUrl(defaultToUnknown)
		}
	}
	
	/**
	 * 直接基于dds文件的相对于游戏或模组目录的路径进行解析。返回对应的PNG图片的绝对路径。
	 */
	fun resolveByFilePath(filePath: String, project: Project, frame: Int = 0, defaultToUnknown: Boolean = false): String {
		try {
			//如果无法解析为png文件地址，则返回默认的地址
			val url = doResolveByFilePath(filePath, project, frame)
			if(url.isNullOrEmpty()) return getDefaultUrl(defaultToUnknown)
			return url
		} catch(e: Exception) {
			//如果出现异常，那么返回默认图标
			logger.warn(e) { "Resolve dds url failed. (dds file path: ${filePath})" }
			return getDefaultUrl(defaultToUnknown)
		}
	}
	
	private fun doResolveByDefinition(definition: ParadoxDefinitionProperty, definitionInfo: ParadoxDefinitionInfo): String? {
		//兼容definition不是sprite的情况
		val (_,file,frame) = definitionInfo.primaryPictureConfigs.mapAndFirst { 
			it.location.resolve(definitionInfo, definition, definition.project)
		} ?: return null
		if(file == null) return null
		return doResolveByFile(file.virtualFile, frame)
	}
	
	/**
	 * 得到sprite定义的对应DDS文件的filePath。基于名为"textureFile"的定义属性（忽略大小写）。
	 */
	fun getSpriteDdsFilePath(sprite: ParadoxDefinitionProperty): String? {
		return sprite.findProperty("textureFile")?.propertyValue?.value?.castOrNull<ParadoxScriptString>()?.stringValue
	}
	
	//private fun doResolveByFile(fileName: String, project: Project, frame: Int): String? {
	//	val files = FilenameIndex.getVirtualFilesByName(fileName, false, GlobalSearchScope.allScope(project))
	//	val file = files.firstOrNull() ?: return null //直接取第一个
	//	return doResolveByFile(file, frame)
	//}
	
	private fun doResolveByFile(file: VirtualFile, frame: Int): String? {
		if(file.fileType != DdsFileType) return null
		//如果可以得到相对于游戏或模组根路径的文件路径，则使用绝对根路径+相对路径定位，否则直接使用绝对路径
		val fileInfo = file.fileInfo
		val rootPath = fileInfo?.rootPath
		val ddsRelPath = fileInfo?.path?.path
		val ddsAbsPath = if(rootPath != null && ddsRelPath != null) {
			rootPath.absolutePathString() + "/" + ddsRelPath
		} else {
			file.toNioPath().absolutePathString()
		}
		return DdsToPngConverter.convert(ddsAbsPath, ddsRelPath, frame)
	}
	
	private fun doResolveByFilePath(filePath: String, project: Project, frame: Int): String? {
		val file = findFileByFilePath(filePath, project) ?: return null
		return doResolveByFile(file, frame)
	}
	
	private fun getDefaultUrl(defaultToUnknown: Boolean): String {
		return if(defaultToUnknown) DdsToPngConverter.getUnknownPngPath() else ""
	}
	
	fun getPngFile(file: VirtualFile, frame: Int = 0): VirtualFile? {
		val absPngPath = doResolveByFile(file, frame) ?: return null
		return VfsUtil.findFile(absPngPath.toPath(), true)
	}
}