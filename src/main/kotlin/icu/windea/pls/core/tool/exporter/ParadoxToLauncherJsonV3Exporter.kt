package icu.windea.pls.core.tool.exporter

import com.intellij.openapi.application.*
import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.ui.table.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.actions.*
import icu.windea.pls.core.settings.*
import icu.windea.pls.core.tool.*
import icu.windea.pls.core.tool.model.*
import icu.windea.pls.lang.model.*

private const val defaultSavedName = "playlist.json"

/**
 * 导出模组配置到启动器JSON配置文件。（>= 2021.10）
 *
 * * See: [ParadoxLauncherExporter202110.cs](https://github.com/bcssov/IronyModManager/blob/master/src/IronyModManager.IO/Mods/Exporter/ParadoxLauncherExporter202110.cs)
 */
class ParadoxToLauncherJsonV3Exporter : ParadoxModExporter {
    var defaultSelected: VirtualFile? = null
    
    override val text: String = PlsBundle.message("mod.exporter.launcherJson.v3")
    
    override fun execute(project: Project, tableView: TableView<ParadoxModDependencySettingsState>, tableModel: ParadoxModDependenciesTableModel) {
        val settings = tableModel.settings
        val gameType = settings.gameType.orDefault()
        if(defaultSelected == null) {
            val gameDataPath = getGameDataPath(gameType.name)?.toPathOrNull()
            val playlistsPath = gameDataPath?.resolve("playlists")
            val playlistsFile = playlistsPath?.toVirtualFile(false)
            if(playlistsFile != null) defaultSelected = playlistsFile
        }
        val descriptor = FileSaverDescriptor(PlsBundle.message("mod.exporter.launcherJson.v3.title"), "", "json")
            .apply { putUserData(PlsDataKeys.gameTypeKey, gameType) }
        val saved = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, tableView).save(defaultSavedName)
        val savedFile = saved?.getVirtualFile(true) ?: return
        //使用正在编辑的模组依赖
        //不导出本地模组
        val validModDependencies = tableModel.modDependencies.filter { it.source != ParadoxModSource.Local }
        val json = ParadoxLauncherJsonV3(
            game = gameType.id,
            mods = validModDependencies.mapIndexed t@{ i, s ->
                ParadoxLauncherJsonV3.Mod(
                    displayName = s.name.orEmpty(),
                    enabled = s.enabled,
                    position = i,
                    steamId = s.remoteId?.takeIf { s.source == ParadoxModSource.Steam },
                    pdxId = s.remoteId?.takeIf { s.source == ParadoxModSource.Paradox },
                )
            },
            name = savedFile.nameWithoutExtension,
        )
        runWriteAction {
            jsonMapper.writeValue(savedFile.getOutputStream(this), json)
        }
        val count = validModDependencies.size
        notify(settings, project, PlsBundle.message("mod.exporter.info", savedFile.nameWithoutExtension, count))
    }
}