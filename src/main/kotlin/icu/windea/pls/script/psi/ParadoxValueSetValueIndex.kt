package icu.windea.pls.script.psi

import com.intellij.psi.stubs.*

object ParadoxValueSetValueIndex : StringStubIndexExtension<ParadoxScriptString>() {
	private val key = StubIndexKey.createIndexKey<String, ParadoxScriptString>("paradox.valueSetValue.index")
	private const val version = 10 //0.7.4
	private const val cacheSize = 2 * 1024
	
	override fun getKey() = key
	
	override fun getVersion() = version
	
	override fun getCacheSize() = cacheSize
}
