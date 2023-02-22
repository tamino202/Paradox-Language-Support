package icu.windea.pls.core.tool

import com.intellij.openapi.observable.properties.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.dsl.builder.*
import icu.windea.pls.*
import icu.windea.pls.core.actions.*
import icu.windea.pls.core.settings.*
import icu.windea.pls.lang.model.*
import java.awt.*

class ParadoxModDependencySettingsDialog(
    val project: Project,
    val settings: ParadoxModDependencySettingsState,
    parentComponent: Component? = null
) : DialogWrapper(project, parentComponent, true, IdeModalityType.PROJECT) {
    val graph = PropertyGraph()
    val gameTypeProperty = graph.property(settings.gameType ?: getSettings().defaultGameType)
    
    init {
        title = PlsBundle.message("mod.dependency.settings")
        init()
    }
    
    override fun createCenterPanel(): DialogPanel {
        return panel {
            row {
                //name
                label(PlsBundle.message("mod.dependency.settings.name")).widthGroup("left")
                textField()
                    .text(settings.name.orEmpty())
                    .columns(36)
                    .align(Align.FILL)
                    .enabled(false)
            }
            row {
                //version
                label(PlsBundle.message("mod.dependency.settings.version")).widthGroup("left")
                textField()
                    .text(settings.version.orEmpty())
                    .columns(18)
                    .enabled(false)
                //supportedVersion
                label(PlsBundle.message("mod.dependency.settings.supportedVersion")).widthGroup("right")
                textField()
                    .text(settings.supportedVersion.orEmpty())
                    .columns(18)
                    .enabled(false)
                    .visible(settings.supportedVersion.orEmpty().isNotEmpty())
            }
            row {
                //gameType
                label(PlsBundle.message("mod.dependency.settings.gameType")).widthGroup("left")
                comboBox(ParadoxGameType.valueList)
                    .bindItem(gameTypeProperty)
                    .columns(18)
                    .enabled(false)
            }
            row {
                //modDirectory
                label(PlsBundle.message("mod.dependency.settings.modDirectory")).widthGroup("left")
                val descriptor = ParadoxRootDirectoryDescriptor()
                    .withTitle(PlsBundle.message("mod.dependency.settings.modDirectory.title"))
                    .apply { putUserData(PlsDataKeys.gameTypePropertyKey, gameTypeProperty) }
                textFieldWithBrowseButton(null, project, descriptor) { it.path }
                    .text(settings.modDirectory.orEmpty())
                    .columns(36)
                    .align(Align.FILL)
                    .enabled(false)
            }
        }
    }
    
    //override fun createDefaultActions() {
    //    okAction.isEnabled = false
    //    cancelAction.putValue(Action.NAME, IdeBundle.message("action.close"))
    //}
}