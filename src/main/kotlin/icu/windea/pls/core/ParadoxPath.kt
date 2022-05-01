package icu.windea.pls.core

import com.google.common.cache.LoadingCache
import icu.windea.pls.*

/**
 * 文件或目录相对于游戏或模组根路径的路径。保留大小写。
 *
 * 示例：
 * * `common/buildings/00_capital_buildings.txt`
 * * `localisation/simp_chinese/l_simp_chinese.yml`
 */
@Suppress("unused")
class ParadoxPath private constructor(
	val path: String
) : Iterable<String> {
	companion object Resolver {
		val EmptyPath = ParadoxPath("")
		
		private val cache: LoadingCache<String, ParadoxPath> = createCache { path -> ParadoxPath(path) }
		
		fun resolve(path: String) = cache[path]
		
		fun resolve(subPaths: List<String>) = cache[subPaths.joinToString("/")]
	}
	
	val subPaths = path.split("/")
	val parent = path.substringBeforeLast("/", "")
	val root = path.substringBefore("/", "")
	val fileName = subPaths.lastOrNull().orEmpty()
	val fileExtension = fileName.substringAfterLast('.', "")
	val length = subPaths.size
	
	fun isEmpty(): Boolean {
		return length == 0
	}
	
	override fun iterator(): Iterator<String> {
		return subPaths.iterator()
	}
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxPath && path == other.path
	}
	
	override fun hashCode(): Int {
		return path.hashCode()
	}
	
	override fun toString(): String {
		return path
	}
}
