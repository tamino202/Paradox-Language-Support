package icu.windea.pls.ep.scope

import icu.windea.pls.lang.psi.*
import icu.windea.pls.model.*

class ParadoxBaseDynamicValueInferredScopeContextProvider : ParadoxDynamicValueInferredScopeContextProvider {
    override fun supports(element: ParadoxDynamicValueElement): Boolean {
        return true
    }

    override fun getScopeContext(element: ParadoxDynamicValueElement): ParadoxScopeContextInferenceInfo? {
        //TODO 1.1.10+
        return null
    }
}

