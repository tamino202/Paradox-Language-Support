package icu.windea.pls.core.search.selector

import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import icu.windea.pls.*
import icu.windea.pls.config.config.*
import icu.windea.pls.core.search.scope.type.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.localisation.psi.*

class ParadoxWithGameTypeSelector<T>(
    val gameType: ParadoxGameType
) : ParadoxSelector<T> {
    override fun select(result: T): Boolean {
        return selectGameType(result) == gameType
    }
    
    override fun selectAll(result: T): Boolean {
        return selectGameType(result) == gameType
    }
}

interface ParadoxSearchScopeAwareSelector<T> : ParadoxSelector<T> {
    fun getGlobalSearchScope(): GlobalSearchScope?
}

class ParadoxWithSearchScopeSelector<T>(
    val scope: GlobalSearchScope
) : ParadoxSearchScopeAwareSelector<T> {
    override fun getGlobalSearchScope(): GlobalSearchScope {
        return scope
    }
}

class ParadoxWithSearchScopeTypeSelector<T : PsiElement>(
    searchScopeType: String,
    val project: Project,
    val context: PsiElement,
) : ParadoxSearchScopeAwareSelector<T> {
    private val type = ParadoxSearchScopeTypes.get(searchScopeType)
    
    private val root by lazy { type.findRoot(project, context) }
    
    override fun select(result: T): Boolean {
        return root == null || root == findRoot(result)
    }
    
    override fun selectAll(result: T): Boolean {
        return select(result)
    }
    
    fun findRoot(context: PsiElement): PsiElement? {
        return type.findRoot(project, context)
    }
    
    override fun getGlobalSearchScope(): GlobalSearchScope? {
        return type.getGlobalSearchScope(project, context)
    }
}

class ParadoxPreferFileSelector<T>(
    val file: VirtualFile
) : ParadoxSelector<T> {
    override fun select(result: T): Boolean {
        return file == selectFile(result)
    }
    
    override fun selectAll(result: T): Boolean {
        return true
    }
    
    override fun comparator(): Comparator<T> {
        return complexCompareBy({ it }, { null }, { file == selectFile(it) })
    }
}

class ParadoxPreferRootFileSelector<T>(
    val rootFile: VirtualFile
) : ParadoxSelector<T> {
    override fun select(result: T): Boolean {
        return rootFile == selectRootFile(result)
    }
    
    override fun selectAll(result: T): Boolean {
        return true
    }
    
    override fun comparator(): Comparator<T> {
        return complexCompareBy({ it }, { null }, { rootFile == selectRootFile(it) })
    }
}

class ParadoxDistinctSelector<T, K>(
    val keySelector: (T) -> K
) : ParadoxSelector<T> {
    val keys = mutableSetOf<K>()
    
    override fun selectAll(result: T): Boolean {
        val key = keySelector(result) ?: return false
        return keys.add(key)
    }
}

class ParadoxFilterSelector<T>(
    val predicate: (T) -> Boolean
) : ParadoxSelector<T> {
    override fun select(result: T): Boolean {
        return predicate(result)
    }
    
    override fun selectAll(result: T): Boolean {
        return predicate(result)
    }
}

class ParadoxLocaleSelector(
    val locale: CwtLocalisationLocaleConfig
) : ParadoxSelector<ParadoxLocalisationProperty> {
    override fun select(result: ParadoxLocalisationProperty): Boolean {
        return locale == result.localeConfig
    }
    
    override fun selectAll(result: ParadoxLocalisationProperty): Boolean {
        return select(result)
    }
}

class ParadoxPreferLocaleSelector(
    val locale: CwtLocalisationLocaleConfig
) : ParadoxSelector<ParadoxLocalisationProperty> {
    override fun select(result: ParadoxLocalisationProperty): Boolean {
        return locale == result.localeConfig
    }
    
    override fun selectAll(result: ParadoxLocalisationProperty): Boolean {
        return true
    }
    
    override fun comparator(): Comparator<ParadoxLocalisationProperty> {
        return complexCompareBy({ it.localeConfig }, { it.id }, { locale == it }) //同时也按照localeId来进行排序
    }
}