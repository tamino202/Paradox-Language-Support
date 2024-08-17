package icu.windea.pls.lang

import com.intellij.openapi.project.*
import com.intellij.openapi.vcs.*
import com.intellij.openapi.vcs.changes.*
import icu.windea.pls.*
import icu.windea.pls.lang.util.*
import javax.swing.*

/**
 * 用于在VCS提交记录中直接基于文件路径为（可能已经不存在的）文件提供正确的图标。
 */
class ParadoxFilePathIconProvider: FilePathIconProvider {
	override fun getIcon(filePath: FilePath, project: Project?): Icon? {
		val fileName = filePath.name
		val fileExtension = fileName.substringAfterLast('.', "")
		val fileInfo = when {
			fileName.equals(PlsConstants.descriptorFileName, true) -> {
				ParadoxCoreManager.getFileInfo(filePath)
			}
			fileExtension in PlsConstants.scriptFileExtensions -> {
				ParadoxCoreManager.getFileInfo(filePath)
			}
			fileExtension in PlsConstants.localisationFileExtensions -> {
				ParadoxCoreManager.getFileInfo(filePath)
			}
			else -> null
		}
		return fileInfo?.getIcon()
	}
}
