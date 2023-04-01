package icu.windea.pls.core.actions

import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.ide.actions.CreateDirectoryCompletionContributor.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.util.Processor
import com.intellij.util.indexing.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.index.*

/**
 * 在游戏目录或模组根目录下创建目录时，可以提示目录名称。
 * 
 * @see icu.windea.pls.core.index.ParadoxDirectoryFilePathIndex
 */
class ParadoxCreateDirectoryCompletionContributor : CreateDirectoryCompletionContributor {
    val defaultVariants = setOf(
        "common",
        "events",
        "gfx",
        "interface",
        "localisation",
    )
    
    override fun getDescription(): String {
        return PlsBundle.message("create.directory.completion.description")
    }
    
    //基于已有的包含脚本文件、本地化文件或者DDS/PNG/TGA文件的目录
    
    override fun getVariants(directory: PsiDirectory): Collection<Variant> {
        val fileInfo = directory.fileInfo ?: return emptySet()
        val contextPath = fileInfo.entryPath.path //use entryPath here
        val contextGameType = fileInfo.rootInfo.gameType
        val pathPrefix = if(contextPath.isEmpty()) "" else contextPath + "/"
        val result = sortedSetOf<String>()
        if(contextPath.isEmpty()) {
            result.addAll(defaultVariants)
        }
        val project = directory.project
        //为了优化性能，使用另外的ParadoxDirectoryFilePathIndex而非ParadoxFilePathIndex
        val name = ParadoxDirectoryFilePathIndex.NAME
        FileBasedIndex.getInstance().processAllKeys(name, p@{ (path, gameType) ->
            ProgressManager.checkCanceled()
            if(contextGameType != gameType) return@p true
            
            var p = path.removePrefixOrNull(pathPrefix)
            if(p != null && p.isNotEmpty()) {
                result.add(p)
            }
            true
        }, project)
        return result.map { it.toVariant() }
    }
    

    
    private fun String.toVariant() = Variant(this, null)
}