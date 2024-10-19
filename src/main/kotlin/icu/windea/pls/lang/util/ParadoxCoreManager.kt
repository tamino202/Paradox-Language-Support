package icu.windea.pls.lang.util

import com.fasterxml.jackson.module.kotlin.*
import com.intellij.codeInsight.daemon.*
import com.intellij.injected.editor.*
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.vcs.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.testFramework.*
import com.intellij.ui.layout.*
import com.intellij.util.*
import com.intellij.util.indexing.*
import icu.windea.pls.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.*
import icu.windea.pls.core.data.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.index.*
import icu.windea.pls.lang.listeners.*
import icu.windea.pls.lang.util.data.*
import icu.windea.pls.model.*
import icu.windea.pls.script.psi.*
import java.nio.file.*
import kotlin.io.path.*

object ParadoxCoreManager {
    fun getRootInfo(rootFile: VirtualFile): ParadoxRootInfo? {
        if (!rootFile.isDirectory) return null

        //首先尝试获取注入的rootInfo
        val injectedRootInfo = rootFile.getUserData(PlsKeys.injectedRootInfo)
        if (injectedRootInfo != null) return injectedRootInfo

        val cachedRootInfo = rootFile.getUserData(PlsKeys.rootInfo)
        if (cachedRootInfo != null) return cachedRootInfo.castOrNull()

        synchronized(rootFile) {
            val _cachedRootInfo = rootFile.getUserData(PlsKeys.rootInfo)
            if (_cachedRootInfo != null) return _cachedRootInfo.castOrNull()

            //resolve rootInfo
            try {
                val rootInfo = doGetRootInfo(rootFile)
                if (rootInfo != null) {
                    rootFile.tryPutUserData(PlsKeys.rootInfo, rootInfo)
                    run {
                        if (ParadoxFileManager.isLightFile(rootFile)) return@run
                        ApplicationManager.getApplication().messageBus.syncPublisher(ParadoxRootInfoListener.TOPIC).onAdd(rootInfo)
                    }
                } else {
                    rootFile.tryPutUserData(PlsKeys.rootInfo, EMPTY_OBJECT)
                }
                return rootInfo
            } catch (e: Exception) {
                if (e is ProcessCanceledException) throw e
                thisLogger().warn(e)
                rootFile.tryPutUserData(PlsKeys.rootInfo, EMPTY_OBJECT)
                return null
            }
        }
    }

    private fun doGetRootInfo(rootFile: VirtualFile): ParadoxRootInfo? {
        //尝试从此目录向下查找descriptor.mod
        run {
            val descriptorFile = disableLogger { getDescriptorFile(rootFile) } ?: return@run
            val descriptorInfo = getDescriptorInfo(descriptorFile) ?: return null
            return ParadoxModRootInfo(rootFile, descriptorFile, descriptorInfo)
        }

        //尝试从此目录向下查找launcher-settings.json，如果找到，再根据"dlcPath"的值获取游戏文件的根目录
        //注意游戏文件的根目录可能是此目录的game子目录，而非此目录自身
        run {
            val launcherSettingsFile = disableLogger { getLauncherSettingsFile(rootFile) } ?: return@run
            val launcherSettingsInfo = getLauncherSettingsInfo(launcherSettingsFile) ?: return null
            return ParadoxGameRootInfo(rootFile, launcherSettingsFile, launcherSettingsInfo)
        }

        return null
    }

    fun getFileInfo(element: PsiElement): ParadoxFileInfo? {
        val file = selectFile(element) ?: return null
        return getFileInfo(file)
    }

    fun getFileInfo(file: VirtualFile): ParadoxFileInfo? {
        //首先尝试获取注入的fileInfo
        val injectedFileInfo = file.getUserData(PlsKeys.injectedFileInfo)
        if (injectedFileInfo != null) return injectedFileInfo

        val cachedFileInfo = file.getUserData(PlsKeys.fileInfo)
        if (cachedFileInfo != null) return cachedFileInfo.castOrNull()

        synchronized(file) {
            val _cachedFileInfo = file.getUserData(PlsKeys.fileInfo)
            if (_cachedFileInfo != null) return _cachedFileInfo.castOrNull()

            //no fileInfo for VirtualFileWindow (injected PSI)
            if (file is VirtualFileWindow) {
                file.tryPutUserData(PlsKeys.fileInfo, EMPTY_OBJECT)
                return null
            }

            //resolve fileInfo by file path
            try {
                val filePath = file.path
                var currentFilePath = filePath.toPathOrNull() ?: return null
                var currentFile = doGetFile(file, currentFilePath)
                while (true) {
                    val rootInfo = if (currentFile == null) null else getRootInfo(currentFile)
                    if (rootInfo != null) {
                        val fileInfo = doGetFileInfo(file, filePath, rootInfo)
                        file.tryPutUserData(PlsKeys.fileInfo, fileInfo)
                        return fileInfo
                    }
                    currentFilePath = currentFilePath.parent ?: break
                    currentFile = doGetFile(currentFile?.parent, currentFilePath)
                }
                file.tryPutUserData(PlsKeys.fileInfo, EMPTY_OBJECT)
                return null
            } catch (e: Exception) {
                if (e is ProcessCanceledException) throw e
                thisLogger().warn(e)
                file.tryPutUserData(PlsKeys.fileInfo, EMPTY_OBJECT)
                return null
            }
        }
    }

    private fun doGetFile(file: VirtualFile?, filePath: Path): VirtualFile? {
        //尝试兼容某些file是LightVirtualFile的情况（例如，file位于VCS DIFF视图中）
        if (file is LightVirtualFile) {
            file.originalFile?.let { return it }
            //since 223-EAP, call 'VfsUtil.findFile(Path, boolean)' may cause:
            //java.lang.Throwable: Slow operations are prohibited on EDT. See SlowOperations.assertSlowOperationsAreAllowed javadoc.
            disableLogger { VfsUtil.findFile(filePath, false) }?.let { return it }
            return null
        }
        return file
    }

    private fun doGetFileInfo(file: VirtualFile, filePath: String, rootInfo: ParadoxRootInfo): ParadoxFileInfo {
        val relPath = ParadoxPath.resolve(filePath.removePrefix(rootInfo.rootFile.path).trimFast('/'))
        val path = resolvePath(relPath, rootInfo)
        val fileType = ParadoxFileType.resolve(file, path, rootInfo)
        val fileInfo = ParadoxFileInfo(path, relPath, fileType, rootInfo)
        return fileInfo
    }

    fun getFileInfo(filePath: FilePath): ParadoxFileInfo? {
        try {
            //直接尝试通过filePath获取fileInfo
            var currentFilePath = filePath.path.toPathOrNull() ?: return null
            var currentFile = VfsUtil.findFile(currentFilePath, false)
            while (true) {
                val rootInfo = if (currentFile == null) null else getRootInfo(currentFile)
                if (rootInfo != null) {
                    val newFileInfo = doGetFileInfo(filePath, rootInfo)
                    return newFileInfo
                }
                currentFilePath = currentFilePath.parent ?: break
                currentFile = VfsUtil.findFile(currentFilePath, false)
            }
            return null
        } catch (e: Exception) {
            if (e is ProcessCanceledException) throw e
            thisLogger().warn(e)
            return null
        }
    }

    private fun doGetFileInfo(filePath: FilePath, rootInfo: ParadoxRootInfo): ParadoxFileInfo {
        val relPath = ParadoxPath.resolve(filePath.path.removePrefix(rootInfo.rootFile.path).trimFast('/'))
        val path = resolvePath(relPath, rootInfo)
        val fileType = ParadoxFileType.resolve(filePath, path, rootInfo)
        val fileInfo = ParadoxFileInfo(path, relPath, fileType, rootInfo)
        return fileInfo
    }

    private fun resolvePath(relPath: ParadoxPath, rootInfo: ParadoxRootInfo): ParadoxPath {
        if (rootInfo is ParadoxModRootInfo) return relPath
        rootInfo.gameEntryPath?.let { entryPath ->
            relPath.path.removePrefixOrNull("$entryPath/")?.let { return ParadoxPath.resolve(it) }
        }
        rootInfo.gameType.entryPaths.forEach { entryPath ->
            relPath.path.removePrefixOrNull("$entryPath/")?.let { return ParadoxPath.resolve(it) }
        }
        return relPath
    }

    fun getLauncherSettingsFile(root: VirtualFile): VirtualFile? {
        if (root.name == "launcher") return null
        //launcher-settings.json
        root.findChild(PlsConstants.launcherSettingsFileName)
            ?.takeIf { !it.isDirectory }
            ?.let { return it }
        //launcher/launcher-settings.json
        root.findChild("launcher")
            ?.takeIf { it.isDirectory }
            ?.findChild(PlsConstants.launcherSettingsFileName)
            ?.takeIf { !it.isDirectory }
            ?.let { return it }
        return null
    }

    fun getDescriptorFile(rootFile: VirtualFile): VirtualFile? {
        //descriptor.mod
        rootFile.findChild(PlsConstants.descriptorFileName)
            ?.takeIf { !it.isDirectory }
            ?.let { return it }
        return null
    }

    fun getLauncherSettingsInfo(file: VirtualFile): ParadoxLauncherSettingsInfo? {
        try {
            return doGetLauncherSettingsInfo(file)
        } catch (e: Exception) {
            if (e is ProcessCanceledException) throw e
            thisLogger().warn(e)
            return null
        }
    }

    private fun doGetLauncherSettingsInfo(file: VirtualFile): ParadoxLauncherSettingsInfo {
        return jsonMapper.readValue(file.inputStream)
    }

    fun getDescriptorInfo(file: VirtualFile): ParadoxModDescriptorInfo? {
        //descriptor.mod
        try {
            return runReadAction { doGetDescriptorInfo(file) }
        } catch (e: Exception) {
            if (e is ProcessCanceledException) throw e
            thisLogger().warn(e)
            return null
        }
    }

    private fun doGetDescriptorInfo(file: VirtualFile): ParadoxModDescriptorInfo {
        //val psiFile = file.toPsiFile<ParadoxScriptFile>(getDefaultProject()) ?: return null //会导致StackOverflowError
        val psiFile = ParadoxScriptElementFactory.createDummyFile(getDefaultProject(), file.inputStream.reader().readText())
        val data = ParadoxScriptDataResolver.resolve(psiFile)
        val name = data?.getData("name")?.value?.stringValue() ?: file.parent?.name ?: "" //如果没有name属性，则使用根目录名
        val version = data?.getData("version")?.value?.stringValue()
        val picture = data?.getData("picture")?.value?.stringValue()
        val tags = data?.getAllData("tags")?.mapNotNull { it.value?.stringValue() }?.toSet()
        val supportedVersion = data?.getData("supported_version")?.value?.stringValue()
        val remoteFileId = data?.getData("remote_file_id")?.value?.stringValue()
        val path = data?.getData("path")?.value?.stringValue()
        return ParadoxModDescriptorInfo(name, version, picture, tags, supportedVersion, remoteFileId, path)
    }

    fun getLocaleConfig(file: VirtualFile, project: Project): CwtLocalisationLocaleConfig? {
        //使用简单缓存与文件索引以优化性能（避免直接访问PSI）

        //首先尝试获取注入的localeConfig
        val injectedLocaleConfig = file.getUserData(PlsKeys.injectedLocaleConfig)
        if (injectedLocaleConfig != null) return injectedLocaleConfig

        val cachedLocaleConfig = file.getUserData(PlsKeys.localeConfig)
        if (cachedLocaleConfig != null) return cachedLocaleConfig.castOrNull()

        synchronized(file) {
            val _cachedLocaleConfig = file.getUserData(PlsKeys.localeConfig)
            if (_cachedLocaleConfig != null) return _cachedLocaleConfig.castOrNull()

            val indexKey = ParadoxFileLocaleIndex.NAME
            val localeId = FileBasedIndex.getInstance().getFileData(indexKey, file, project).keys.singleOrNull() ?: return null
            val localeConfig = getConfigGroup(project, null).localisationLocalesById.get(localeId)
            file.tryPutUserData(PlsKeys.localeConfig, localeConfig ?: EMPTY_OBJECT)
            return localeConfig
        }
    }

    fun getInferredGameType(rootInfo: ParadoxModRootInfo): ParadoxGameType? {
        val parentDir = rootInfo.rootFile.parent
        runCatchingCancelable r@{
            //如果模组目录直接位于游戏创意工坊目录下，直接推断为对应的游戏类型
            val steamWorkshopDir = parentDir ?: return@r
            val steamId = steamWorkshopDir.name
            val gameType = ParadoxGameType.entries.find { it.steamId == steamId } ?: return@r
            if (getDataProvider().getSteamWorkshopPath(steamId) != steamWorkshopDir.toNioPath().absolutePathString()) return@r
            return gameType
        }
        runCatchingCancelable r@{
            //如果模组目录直接位于游戏数据目录下的mod子目录下，直接推断为对应的游戏类型
            val modDir = parentDir.takeIf { it.name == "mod" } ?: return@r
            val gameDataDir = modDir.parent ?: return@r
            val gameName = gameDataDir.name
            val gameType = ParadoxGameType.entries.find { it.title == gameName } ?: return@r
            if (getDataProvider().getGameDataPath(gameName) != gameDataDir.toNioPath().absolutePathString()) return@r
            return gameType
        }
        return null
    }
    
    fun isExcludedRootFilePath(rootFilePath: String): Boolean {
        //https://github.com/DragonKnightOfBreeze/Paradox-Language-Support/issues/90
        //exclude some specific root file paths to avoid parsing and indexing unexpected files
        return rootFilePath.isEmpty() || rootFilePath == "/"
    }
    
    fun findFilesByRootFilePaths(rootFilePaths: Set<String>): MutableSet<VirtualFile> {
        val files = mutableSetOf<VirtualFile>()
        runReadAction {
            rootFilePaths.forEach f@{ rootFilePath ->
                if(isExcludedRootFilePath(rootFilePath)) return@f
                val rootFile = VfsUtil.findFile(rootFilePath.toPathOrNull() ?: return@f, true) ?: return@f
                VfsUtil.visitChildrenRecursively(rootFile, object : VirtualFileVisitor<Void>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (file.isFile) files.add(file)
                        return true
                    }
                })
            }
        }
        return files
    }

    fun findFilesByFileNames(fileNames: Set<String>): Set<VirtualFile> {
        val files = mutableSetOf<VirtualFile>()
        runReadAction {
            val project = getTheOnlyOpenOrDefaultProject()
            FilenameIndex.processFilesByNames(fileNames, false, GlobalSearchScope.allScope(project), null) { file ->
                if (file.isFile) files.add(file)
                true
            }
        }
        return files
    }

    fun findOpenedFiles(onlyParadoxFiles: Boolean = true, predicate: ((VirtualFile, Project) -> Boolean)? = null): Set<VirtualFile> {
        val files = mutableSetOf<VirtualFile>()
        runReadAction {
            val openProjects = ProjectManager.getInstance().openProjects
            for (project in openProjects) {
                val allEditors = FileEditorManager.getInstance(project).allEditors
                for (fileEditor in allEditors) {
                    if (fileEditor is TextEditor) {
                        val file = fileEditor.file
                        if (onlyParadoxFiles && !file.fileType.isParadoxFileType()) continue
                        if (predicate == null || predicate(file, project)) {
                            files.add(file)
                        }
                    }
                }
            }
        }
        return files
    }

    fun reparseFiles(files: Set<VirtualFile>, reparse: Boolean = true, restartDaemon: Boolean = true) {
        if (files.isEmpty()) return

        runInEdt {
            try {
                //重新解析文件
                if (reparse) {
                    FileContentUtilCore.reparseFiles(files)
                }

                //刷新内嵌提示
                if (restartDaemon) {
                    val openProjects = ProjectManager.getInstance().openProjects
                    for (project in openProjects) {
                        val allEditors = FileEditorManager.getInstance(project).allEditors
                        for (fileEditor in allEditors) {
                            if (fileEditor is TextEditor) {
                                val file = fileEditor.file
                                if (file !in files) continue

                                val psiFile = file.toPsiFile(project) ?: continue
                                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)

                                //removed in IU-233
                                //InlayHintsPassFactory.clearModificationStamp(fileEditor.editor)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is ProcessCanceledException) throw e
                thisLogger().warn(e.message)
            }
        }
    }

    fun validateGameDirectory(builder: ValidationInfoBuilder, gameType: ParadoxGameType, gameDirectory: String?): ValidationInfo? {
        //验证游戏目录是否合法
        //* 路径合法
        //* 路径对应的目录存在
        //* 路径是游戏目录（可以查找到对应的launcher-settings.json）
        val gameDirectory0 = gameDirectory?.normalizePath()?.orNull() ?: return null
        val path = gameDirectory0.toPathOrNull()
        if (path == null) return builder.error(PlsBundle.message("gameDirectory.error.1"))
        val rootFile = VfsUtil.findFile(path, true)?.takeIf { it.exists() }
        if (rootFile == null) return builder.error(PlsBundle.message("gameDirectory.error.2"))
        val rootInfo = rootFile.rootInfo
        if (rootInfo !is ParadoxGameRootInfo) return builder.error(PlsBundle.message("gameDirectory.error.3", gameType.title))
        return null
    }

    fun getQuickGameDirectory(gameType: ParadoxGameType): String? {
        val path = getDataProvider().getSteamGamePath(gameType.steamId, gameType.title)
        if (path == null || path.toPathOrNull()?.takeIf { it.exists() } == null) return null
        return path
    }

    fun getGameVersionFromGameDirectory(gameDirectory: String?): String? {
        val gameDirectory0 = gameDirectory?.normalizePath()?.orNull() ?: return null
        val rootFile = gameDirectory0.toVirtualFile(true)?.takeIf { it.exists() } ?: return null
        val rootInfo = rootFile.rootInfo
        if (rootInfo !is ParadoxGameRootInfo) return null
        return rootInfo.launcherSettingsInfo.rawVersion
    }
}
