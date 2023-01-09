package icu.windea.pls.script.psi.impl

import com.intellij.psi.stubs.*
import icu.windea.pls.config.core.config.*
import icu.windea.pls.script.psi.*

class ParadoxScriptScriptedVariableStubImpl(
	parent: StubElement<*>,
	override val name: String?,
	override val gameType: ParadoxGameType?
) : StubBase<ParadoxScriptScriptedVariable>(parent, ParadoxScriptStubElementTypes.SCRIPTED_VARIABLE), ParadoxScriptScriptedVariableStub{
	override fun toString(): String {
		return "ParadoxScriptScriptedVariableStub: (name=$name, gameType=$gameType)"
	}
}

