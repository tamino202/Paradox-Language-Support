package icu.windea.pls.ep.configGroup

import com.intellij.openapi.editor.toolbar.floating.*
import com.intellij.psi.impl.*
import icu.windea.pls.config.configGroup.*
import icu.windea.pls.core.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.model.*

class CwtConfigGroupPsiTreeChangePreprocessor : PsiTreeChangePreprocessor {
    //这个方法应当尽可能地快
    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if(!PsiModificationTrackerImpl.canAffectPsi(event)) return
        
        val file = event.file ?: return
        if(file !is CwtFile) return
        val vFile = file.virtualFile ?: return
        val project = file.project
        val fileProviders = CwtConfigGroupFileProvider.EP_NAME.extensionList
        val configGroups = mutableSetOf<CwtConfigGroup>()
        fileProviders.forEach f@{ fileProvider ->
            if(fileProvider.isBuiltIn()) return@f
            val configGroup = fileProvider.getContainingConfigGroup(vFile, project) ?: return@f
            if(configGroup.gameType == null) {
                ParadoxGameType.values.forEach { gameType ->
                    configGroups += getConfigGroup(project, gameType)
                }
            } else {
                configGroups += configGroup
            }
        }
        val configGroupsToChange = configGroups.filter { !it.changed.get() }
        if(configGroupsToChange.isNotEmpty()) {
            configGroupsToChange.forEach { configGroup -> configGroup.changed.set(true) }
            FloatingToolbarProvider.getProvider<ConfigGroupRefreshFloatingProvider>()
                .updateToolbarComponents(project)
        }
    }
}