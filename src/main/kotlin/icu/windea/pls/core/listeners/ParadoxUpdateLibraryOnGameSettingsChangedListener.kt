package icu.windea.pls.core.listeners

import com.intellij.openapi.application.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import icu.windea.pls.core.*
import icu.windea.pls.core.settings.*

/**
 * 当更改游戏配置后，刷新库信息。
 *
 * @see ParadoxLibrary
 * @see ParadoxLibraryProvider
 */
class ParadoxUpdateLibraryOnGameSettingsChangedListener : ParadoxGameSettingsListener {
    //目前不考虑onRemove的情况
    
    override fun onAdd(gameSettings: ParadoxGameSettingsState) {
        doUpdateLibrary(gameSettings.gameDirectory)
    }
    
    override fun onChange(gameSettings: ParadoxGameSettingsState) {
        doUpdateLibrary(gameSettings.gameDirectory)
    }
    
    //org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsUpdater.doUpdate
    
    private fun doUpdateLibrary(directory: String?) {
        val root = directory?.orNull()?.toVirtualFile(false) ?: return
        for(project in ProjectManager.getInstance().openProjects) {
            if(project.isDisposed) continue
            val isInProject = runReadAction { ProjectFileIndex.getInstance(project).isInContent(root) }
            if(!isInProject) continue
            val paradoxLibrary = project.paradoxLibrary
            paradoxLibrary.refreshRoots()
        }
    }
}