package icu.windea.pls.lang.search

import com.intellij.openapi.application.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.search.*
import com.intellij.util.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.index.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.model.indexInfo.*
import icu.windea.pls.script.*

/**
 * 内联脚本使用的查询器。
 */
class ParadoxInlineScriptUsageSearcher : QueryExecutorBase<ParadoxInlineScriptUsageInfo.Compact, ParadoxInlineScriptUsageSearch.SearchParameters>() {
    override fun processQuery(queryParameters: ParadoxInlineScriptUsageSearch.SearchParameters, consumer: Processor<in ParadoxInlineScriptUsageInfo.Compact>) {
        ProgressManager.checkCanceled()
        val scope = queryParameters.selector.scope
        if (SearchScope.isEmptyScope(scope)) return
        val expression = queryParameters.expression
        val project = queryParameters.project
        val selector = queryParameters.selector
        val gameType = selector.gameType

        doProcessFiles(scope) p@{ file ->
            ProgressManager.checkCanceled()
            ParadoxCoreManager.getFileInfo(file) //ensure file info is resolved here
            if (selectGameType(file) != gameType) return@p true //check game type at file level

            val fileData = ParadoxInlineScriptUsageInfoIndex.INSTANCE.getFileData(file, project)
            if (fileData.isEmpty()) return@p true
            if(expression.isNotEmpty()) {
                val compactInfo = fileData[expression] ?: return@p true
                compactInfo.virtualFile = file
                val r = consumer.process(compactInfo)
                if (!r) return@p false
            } else {
                fileData.values.forEach { compactInfo ->
                    compactInfo.virtualFile = file
                    val r = consumer.process(compactInfo)
                    if (!r) return@p false
                }
            }
            
            true
        }
    }

    private fun doProcessFiles(scope: GlobalSearchScope, processor: Processor<VirtualFile>): Boolean {
        return FileTypeIndex.processFiles(ParadoxScriptFileType, processor, scope)
    }
}
