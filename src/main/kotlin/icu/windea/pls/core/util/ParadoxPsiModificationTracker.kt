package icu.windea.pls.core.util

import com.intellij.openapi.util.*

//com.intellij.psi.util.PsiModificationTracker
//com.intellij.psi.impl.PsiModificationTrackerImpl

/**
 * 用于追踪PSI更改 - 具有更高的精确度，提高缓存命中率。
 */
@Service(Service.Level.PROJECT)
class ParadoxPsiModificationTracker(project: Project) {
    val ScriptFileTracker = DelegatedModificationTracker(PsiModificationTracker.getInstance(project).forLanguage(ParadoxScriptLanguage))
    val LocalisationFileTracker = DelegatedModificationTracker(PsiModificationTracker.getInstance(project).forLanguage(ParadoxLocalisationLanguage))
    
    val ScriptFileTrackers = ConcurrentHashMap<String, PathModificationTracker>()
    
    /**
     * 这里传入的扩展名应当是小写的且不包含"."。
     * @param keyString 例子：`path`, `path:txt`, `path1:txt|path2:txt,yml`
     */
    fun ScriptFileTracker(keyString: String): PathModificationTracker {
        return ScriptFileTrackers.computeIfAbsent(keyString) { PathModificationTracker(keyString) }
    }
    
    val ScriptedVariablesTracker = ScriptFileTracker("common/scripted_variables:txt")
    val InlineScriptsTracker = ScriptFileTracker("common/inline_scripts:txt")
    
    companion object {
        @JvmField val DefinitionScopeContextInferenceTracker = SimpleModificationTracker()
        
        @JvmStatic
        fun getInstance(project: Project) = project.service<ParadoxPsiModificationTracker>()
    }
}

class DelegatedModificationTracker(private val delegate: ModificationTracker): SimpleModificationTracker() {
    override fun getModificationCount(): Long {
        return super.getModificationCount() + delegate.modificationCount
    }
}

class PathModificationTracker(keyString: String) : SimpleModificationTracker() {
    val keys = keyString.split('|').map {
        val i = it.indexOf(':')
        if(i == -1) PathModificationTrackerKey(it, emptySet())
        else PathModificationTrackerKey(it.substring(0, i), it.substring(i + 1).lowercase().split(',').toSortedSet())
    }
}

class PathModificationTrackerKey(val path: String, val extensions: Set<String>)