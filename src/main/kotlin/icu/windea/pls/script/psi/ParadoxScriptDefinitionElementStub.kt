package icu.windea.pls.script.psi

import com.intellij.psi.stubs.*
import icu.windea.pls.config.core.config.*

interface ParadoxScriptDefinitionElementStub<T : ParadoxScriptDefinitionElement> : StubElement<T> {
	val name: String?
	val type: String?
	val subtypes: List<String>?
	val rootKey: String?
	val gameType: ParadoxGameType?
}
