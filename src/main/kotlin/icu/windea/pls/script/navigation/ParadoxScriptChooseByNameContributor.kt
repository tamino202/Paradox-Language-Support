package icu.windea.pls.script.navigation

import com.intellij.navigation.*
import com.intellij.psi.search.*
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.*
import com.intellij.util.indexing.*
import icu.windea.pls.script.psi.*

//com.intellij.ide.util.gotoByName.JavaModuleNavigationContributor

/**
 * 用于让`Navigate | Class or Navigate | Symbol`可以查找到匹配名字的定义。
 */
class ParadoxScriptChooseByNameContributor: ChooseByNameContributorEx {
	override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
		StubIndex.getInstance().processAllKeys(ParadoxDefinitionNameIndex.key, processor, scope, filter)
	}
	
	override fun processElementsWithName(name: String, processor: Processor<in NavigationItem>, parameters: FindSymbolParameters) {
		StubIndex.getInstance().processElements(ParadoxDefinitionNameIndex.key, name, parameters.project, parameters.searchScope, parameters.idFilter, ParadoxDefinitionProperty::class.java, processor)
	}
}