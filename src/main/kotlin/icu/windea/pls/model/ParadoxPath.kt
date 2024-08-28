package icu.windea.pls.model

import icu.windea.pls.core.*
import icu.windea.pls.lang.util.*

/**
 * 文件路径，相对于游戏或模组目录，或者入口目录。保留大小写。
 *
 * 示例：
 * * `common/buildings/00_capital_buildings.txt`
 * * `localisation/simp_chinese/l_simp_chinese.yml`
 *
 * @property path 使用"/"分割的路径。
 */
interface ParadoxPath : Iterable<String> {
    val path: String
    val subPaths: List<String>
    val parent: String
    val root: String
    val fileName: String
    val fileExtension: String?
    val length: Int
    
    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0
    
    override fun iterator(): Iterator<String> = subPaths.iterator()
    
    companion object Resolver {
        val Empty: ParadoxPath = EmptyParadoxPath
        
        fun resolve(path: String): ParadoxPath = doResolve(path)
        
        fun resolve(subPaths: List<String>): ParadoxPath = doResolve(subPaths)
    }
}

//Implementations (interned)

private fun doResolve(path: String): ParadoxPath {
    if(path.isEmpty()) return EmptyParadoxPath
    return ParadoxPathImplA(path)
}

private fun doResolve(subPaths: List<String>): ParadoxPath {
    if(subPaths.isEmpty()) return EmptyParadoxPath
    return ParadoxPathImplB(subPaths)
}

//12 + 5 * 4 = 32 -> 32
private class ParadoxPathImplA(
    path: String
) : ParadoxPath {
    override val path: String = path.intern()
    override val subPaths: List<String> = path.split('/').map { it.intern() }
    override val parent: String = path.substringBeforeLast('/', "").intern()
    override val root: String get() = subPaths.firstOrNull().orEmpty()
    override val fileName: String = subPaths.lastOrNull().orEmpty()
    override val fileExtension: String? = fileName.substringAfterLast('.', "").intern().orNull()
    override val length: Int get() = subPaths.size
    
    override fun equals(other: Any?) = this === other || other is ParadoxPath && path == other.path
    override fun hashCode() = path.hashCode()
    override fun toString() = path
}

//12 + 5 * 4 = 32 -> 32
private class ParadoxPathImplB(
    subPaths: List<String>
) : ParadoxPath {
    override val subPaths: List<String> = subPaths.map { it.intern() }
    override val path: String = subPaths.joinToString("/").intern()
    override val parent: String = path.substringBeforeLast('/', "").intern()
    override val root: String get() = subPaths.firstOrNull().orEmpty()
    override val fileName: String = subPaths.lastOrNull().orEmpty()
    override val fileExtension: String? = fileName.substringAfterLast('.', "").intern().orNull()
    override val length: Int get() = subPaths.size
    
    override fun equals(other: Any?) = this === other || other is ParadoxPath && path == other.path
    override fun hashCode() = path.hashCode()
    override fun toString() = path
}

private object EmptyParadoxPath: ParadoxPath {
    override val subPaths: List<String> = emptyList()
    override val path: String = ""
    override val parent: String = ""
    override val root: String = ""
    override val fileName: String = ""
    override val fileExtension: String? = null
    override val length: Int = 0
    
    override fun equals(other: Any?) = this === other || other is ParadoxPath && path == other.path
    override fun hashCode() = path.hashCode()
    override fun toString() = path
}
