package icu.windea.pls.core.tool

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
import icu.windea.pls.core.settings.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.model.*
import java.awt.*
import javax.swing.*

class ParadoxModDependencyAddDialog(
    val project: Project,
    gameType: ParadoxGameType,
    parentComponent: Component? = null
) : DialogWrapper(project, parentComponent, true, IdeModalityType.PROJECT) {
    val graph = PropertyGraph()
    val gameTypeProperty = graph.property(gameType)
    val modDirectoryProperty = graph.property("")
    
    val gameType by gameTypeProperty
    val modDirectory by modDirectoryProperty
    
    var resultSettings: ParadoxModDependencySettingsState? = null
    
    init {
        title = PlsBundle.message("mod.dependency.add")
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                //gameType
                label(PlsBundle.message("mod.dependency.add.gameType")).widthGroup("left")
                comboBox(ParadoxGameType.valueList)
                    .bindItem(gameTypeProperty)
                    .align(Align.FILL)
                    .columns(18)
                    .enabled(false)
            }
            row {
                //modDirectory
                label(PlsBundle.message("mod.dependency.add.modDirectory")).widthGroup("left")
                val descriptor = ParadoxRootDirectoryDescriptor()
                    .withTitle(PlsBundle.message("mod.dependency.add.modDirectory.title"))
                    .apply { putUserData(PlsDataKeys.gameTypePropertyKey, gameTypeProperty) }
                textFieldWithBrowseButton(null, project, descriptor) { it.path }
                    .bindText(modDirectoryProperty)
                    .align(Align.FILL)
                    .columns(36)
                    .validationOnApply { validateModDirectory() }
            }
        }
    }
    
    private fun ValidationInfoBuilder.validateModDirectory(): ValidationInfo? {
        val path = modDirectory.toPathOrNull()
            ?: return error(PlsBundle.message("mod.dependency.add.modDirectory.error.1"))
        val rootFile = VfsUtil.findFile(path, false)?.takeIf { it.exists() }
            ?: return error(PlsBundle.message("mod.dependency.add.modDirectory.error.2"))
        val rootInfo = ParadoxCoreHandler.resolveRootInfo(rootFile)
        if(rootInfo !is ParadoxModRootInfo) {
            return error(PlsBundle.message("mod.dependency.add.modDirectory..error.3"))
        }
        return null
    }
    
    override fun doOKAction() {
        //这里点击确定按钮后会弹出模组依赖配置对话框，以便预览模组配置，再次点击确定按钮才会添加到模组依赖列表
        val settings = ParadoxModDependencySettingsState()
        settings.modDirectory = modDirectory
        settings.selected = true
        val editDialog = ParadoxModDependencySettingsDialog(project, settings, this.contentPanel)
        if(!editDialog.showAndGet()) return
        resultSettings = settings
        super.doOKAction()
    }
}
