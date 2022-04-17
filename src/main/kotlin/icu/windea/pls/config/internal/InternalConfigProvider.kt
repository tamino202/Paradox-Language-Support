package icu.windea.pls.config.internal

import com.intellij.openapi.application.*
import com.intellij.openapi.vfs.*
import icu.windea.pls.*
import icu.windea.pls.config.cwt.*
import org.slf4j.*
import org.yaml.snakeyaml.*

/**
 * 内置规则的提供器。
 *
 * 内置规则来自目录`config/internal`中的配置文件
 */
class InternalConfigProvider {
	companion object {
		private val logger = LoggerFactory.getLogger(CwtConfigProvider::class.java)
		private val yaml = Yaml()
		
		private const val internalConfigPath = "/config/internal"
	}
	
	val configGroup: InternalConfigGroup = initConfigGroup()
	
	@Synchronized
	private fun initConfigGroup(): InternalConfigGroup {
		val configMap = initConfigMap()
		val startTime = System.currentTimeMillis()
		logger.info("Init internal config group.")
		val configGroup = InternalConfigGroup(configMap)
		val endTime = System.currentTimeMillis()
		logger.info("Init internal config group finished. (${endTime - startTime} ms)")
		return configGroup
	}
	
	private fun initConfigMap(): InternalConfigMap {
		//不保存配置文件信息
		val startTime = System.currentTimeMillis()
		logger.info("Resolve internal config files.")
		val configMap: InternalConfigMap = mutableMapOf()
		val configUrl = internalConfigPath.toUrl(locationClass)
		//通过这种方式得到的virtualFile可以位于jar压缩包中，可以直接得到它的子节点
		val configDirectory = VfsUtil.findFileByURL(configUrl)
		if(configDirectory != null) {
			//这里必须使用ReadAction
			ReadAction.run<Exception> {
				resolveConfigFiles(configMap, configDirectory, configDirectory)
			}
		}
		val endTime = System.currentTimeMillis()
		logger.info("Resolve internal config files finished. (${endTime - startTime} ms)")
		return configMap
	}
	
	private fun resolveConfigFiles(configMap: InternalConfigMap, configDirectory: VirtualFile, configRootDirectory: VirtualFile) {
		for(configFile in configDirectory.children) {
			if(configFile.isDirectory){
				//继续解析子目录里面的配置文件
				resolveConfigFiles(configMap, configFile, configRootDirectory)
			} else {
				when(configFile.extension) {
					"yml" -> resolveYamlConfigFile(configMap, configFile, configRootDirectory) //解析yaml配置文件
					else -> pass() //不做处理
				}
			}
		}
	}
	
	private fun resolveYamlConfigFile(configMap: InternalConfigMap, configFile: VirtualFile, configRootDirectory: VirtualFile) {
		val relativePath = configFile.relativePathTo(configRootDirectory)
		logger.info("Resolve internal config file '$relativePath'.")
		val config = doResolveYamlFile(configFile)
		if(config == null) {
			logger.warn("Resolve internal config file '$relativePath' failed. Skip it.")
			return
		}
		configMap.putAll(config)
	}
	
	private fun doResolveYamlFile(configFile: VirtualFile): Map<String, List<Map<String, Any?>>>? {
		return try {
			yaml.load(configFile.inputStream)
		} catch(e: Exception) {
			logger.warn(e.message, e)
			null
		}
	}
}