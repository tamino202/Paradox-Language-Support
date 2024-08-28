package icu.windea.pls.lang.util

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vfs.*
import com.intellij.testFramework.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.lang.*
import icu.windea.pls.model.*
import java.lang.invoke.*

object ParadoxFilePathManager {
    private val logger = Logger.getInstance(MethodHandles.lookup().lookupClass())
    
    const val scriptedVariablesPath = "common/scripted_variables"
    
    fun getRootDirectory(contextFile: VirtualFile): VirtualFile? {
        return contextFile.fileInfo?.rootInfo?.gameRootFile
    }
    
    fun getScriptedVariablesDirectory(contextFile: VirtualFile): VirtualFile? {
        val root = getRootDirectory(contextFile) ?: return null
        VfsUtil.createDirectoryIfMissing(root, scriptedVariablesPath)
        return root.findFileByRelativePath(scriptedVariablesPath)
    }
    
    fun canBeScriptOrLocalisationFile(filePath: FilePath): Boolean {
        val fileName = filePath.name.lowercase()
        val fileExtension = filePath.name.substringAfterLast('.').orNull()?.lowercase() ?: return false
        return when {
            fileName == PlsConstants.descriptorFileName -> true
            fileExtension in PlsConstants.scriptFileExtensions -> true
            fileExtension in PlsConstants.localisationFileExtensions -> true
            else -> false
        }
    }
    
    fun canBeScriptOrLocalisationFile(file: VirtualFile): Boolean {
        //require pre-check from user data
        if(file is VirtualFileWithoutContent) return false
        if(file is VirtualFileWindow) return true //require further check for VirtualFileWindow (injected PSI)
        val fileName = file.name.lowercase()
        val fileExtension = file.extension?.lowercase() ?: return false
        return when {
            fileName == PlsConstants.descriptorFileName -> true
            fileExtension in PlsConstants.scriptFileExtensions -> true
            fileExtension in PlsConstants.localisationFileExtensions -> true
            else -> false
        }
    }
    
    fun canBeScriptFilePath(path: ParadoxPath): Boolean {
        if(inLocalisationPath(path)) return false
        val fileExtension = path.fileExtension?.lowercase() ?: return false
        if(fileExtension !in PlsConstants.scriptFileExtensions) return false
        return true
    }
    
    fun canBeLocalisationFilePath(path: ParadoxPath): Boolean {
        if(!inLocalisationPath(path)) return false
        val fileExtension = path.fileExtension?.lowercase() ?: return false
        if(fileExtension !in PlsConstants.localisationFileExtensions) return false
        return true
    }
    
    fun inLocalisationPath(path: ParadoxPath, synced: Boolean? = null): Boolean {
        val root = path.root
        if(synced != true) {
            if(root == "localisation" || root == "localization") return true
        }
        if(synced != false) {
            if(root == "localisation_synced" || root == "localization_synced") return true
        }
        return false
    }
}
