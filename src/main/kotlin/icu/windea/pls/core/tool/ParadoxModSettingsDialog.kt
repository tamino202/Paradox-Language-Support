package icu.windea.pls.core.tool

import com.intellij.openapi.application.*
import com.intellij.openapi.observable.properties.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.vfs.*
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.actions.*
import icu.windea.pls.core.listeners.*
import icu.windea.pls.core.settings.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.model.*

class ParadoxModSettingsDialog(
    val project: Project,
    val settings: ParadoxModSettingsState
) : DialogWrapper(project, true) {
    val oldGameType = settings.gameType ?: getSettings().defaultGameType
    
    val graph = PropertyGraph()
    val gameTypeProperty = graph.property(settings.gameType ?: getSettings().defaultGameType)
        .apply { afterChange { settings.gameType = it } }
    val gameVersionProperty = graph.property(settings.gameVersion.orEmpty())
        .apply { afterChange { settings.gameVersion = it.takeIfNotEmpty() } }
    val gameDirectoryProperty = graph.property(settings.gameDirectory.orEmpty())
        .apply {
            afterChange {
                settings.gameDirectory = it.takeIfNotEmpty()
                settings.gameVersion = getGameVersion()
            }
        }
    
    var gameType by gameTypeProperty
    var gameVersion by gameVersionProperty
    var gameDirectory by gameDirectoryProperty
    
    init {
        title = PlsBundle.message("mod.settings")
        handleModSettings()
        init()
    }
    
    override fun createCenterPanel(): DialogPanel {
        return panel {
            row {
                //name
                label(PlsBundle.message("mod.settings.name")).widthGroup("left")
                textField()
                    .text(settings.name.orEmpty())
                    .align(Align.FILL)
                    .columns(36)
                    .enabled(false)
            }
            row {
                //version
                label(PlsBundle.message("mod.settings.version")).widthGroup("left")
                textField()
                    .text(settings.version.orEmpty())
                    .align(Align.FILL)
                    .columns(18)
                    .enabled(false)
                //supportedVersion
                label(PlsBundle.message("mod.settings.supportedVersion")).widthGroup("right")
                textField()
                    .text(settings.supportedVersion.orEmpty())
                    .align(Align.FILL)
                    .columns(18)
                    .enabled(false)
                    .visible(settings.supportedVersion.orEmpty().isNotEmpty())
            }
            row {
                //gameType
                label(PlsBundle.message("mod.settings.gameType")).widthGroup("left")
                comboBox(ParadoxGameType.valueList)
                    .bindItem(gameTypeProperty)
                    .align(Align.FILL)
                    .columns(18)
                    .onApply { settings.gameType = gameTypeProperty.get() } //set game type to non-default on apply
                //gameVersion
                label(PlsBundle.message("mod.settings.gameVersion")).widthGroup("right")
                textField()
                    .bindText(gameVersionProperty)
                    .align(Align.FILL)
                    .columns(18)
                    .enabled(false)
            }
            row {
                //gameDirectory
                label(PlsBundle.message("mod.settings.gameDirectory")).widthGroup("left")
                val descriptor = ParadoxRootDirectoryDescriptor()
                    .withTitle(PlsBundle.message("mod.settings.gameDirectory.title"))
                    .apply { putUserData(PlsDataKeys.gameTypePropertyKey, gameTypeProperty) }
                textFieldWithBrowseButton(null, project, descriptor) { it.path }
                    .bindText(gameDirectoryProperty)
                    .align(Align.FILL)
                    .columns(36)
                    .validationOnApply { validateGameDirectory() }
            }
            row {
                //quickSelectGameDirectory
                link(PlsBundle.message("mod.settings.quickSelectGameDirectory")) { quickSelectGameDirectory() }
            }
            row {
                //modDirectory
                label(PlsBundle.message("mod.settings.modDirectory")).widthGroup("left")
                val descriptor = ParadoxRootDirectoryDescriptor()
                    .withTitle(PlsBundle.message("mod.settings.modDirectory.title"))
                    .apply { putUserData(PlsDataKeys.gameTypePropertyKey, gameTypeProperty) }
                textFieldWithBrowseButton(null, project, descriptor) { it.path }
                    .text(settings.modDirectory.orEmpty())
                    .align(Align.FILL)
                    .columns(36)
                    .enabled(false)
            }
            
            //modDependencies
            collapsibleGroup(PlsBundle.message("mod.settings.modDependencies"), false) {
                row {
                    cell(createModDependenciesPanel(project, settings)).align(Align.CENTER)
                }
            }
        }
    }
    
    private fun ValidationInfoBuilder.validateGameDirectory(): ValidationInfo? {
        //验证游戏目录是否合法
        //* 路径合法
        //* 路径对应的目录存在
        //* 路径是游戏目录（可以查找到对应的launcher-settings.json）
        val path = gameDirectory.toPathOrNull()
            ?: return error(PlsBundle.message("mod.settings.gameDirectory.error.1"))
        val rootFile = VfsUtil.findFile(path, false)?.takeIf { it.exists() }
            ?: return error(PlsBundle.message("mod.settings.gameDirectory.error.2"))
        val rootInfo = ParadoxCoreHandler.resolveRootInfo(rootFile)
        if(rootInfo !is ParadoxGameRootInfo) {
            return error(PlsBundle.message("mod.settings.gameDirectory.error.3", gameType.description))
        }
        return null
    }
    
    private fun quickSelectGameDirectory() {
        val targetPath = getSteamGamePath(gameType.gameSteamId, gameType.gameName) ?: return
        gameDirectory = targetPath
    }
    
    private fun getGameVersion(): String? {
        val gameDirectory = gameDirectory.takeIfNotEmpty() ?: return null
        val rootFile = gameDirectory.toVirtualFile(false)?.takeIf { it.exists() } ?: return null
        val rootInfo = ParadoxCoreHandler.resolveRootInfo(rootFile)
        if(rootInfo !is ParadoxGameRootInfo) return null
        return rootInfo.launcherSettingsInfo.rawVersion
    }
    
    private fun handleModSettings() {
        //如果需要，加上缺失的模组自身的模组依赖配置
        val modDependencies = settings.modDependencies
        if(modDependencies.find { it.modDirectory == settings.modDirectory } == null) {
            val newSettings = ParadoxModDependencySettingsState()
            newSettings.modDirectory = settings.modDirectory
            modDependencies.add(newSettings)
        }
    }
    
    override fun doOKAction() {
        getProfilesSettings().updateSettings()
        
        val messageBus = ApplicationManager.getApplication().messageBus
        messageBus.syncPublisher(ParadoxModSettingsListener.TOPIC).onChange(settings)
        
        if(oldGameType != settings.gameType) {
            messageBus.syncPublisher(ParadoxModGameTypeListener.TOPIC).onChange(settings)
        }
    
        super.doOKAction()
    }
}

