package icu.windea.pls.script.psi

import com.intellij.psi.stubs.*
import icu.windea.pls.core.model.*

interface ParadoxScriptStringExpressionElementStub<T : ParadoxScriptStringExpressionElement> : StubElement<T> {
	val complexEnumValueInfo: ParadoxComplexEnumValueInfo?
	val gameType: ParadoxGameType?
}