package icu.windea.pls.script.navigation

import icons.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.navigation.*
import icu.windea.pls.model.*
import icu.windea.pls.model.expressionInfo.*
import icu.windea.pls.script.psi.*
import javax.swing.*

class ParadoxScriptFilePresentation(
    element: ParadoxScriptFile
) : ParadoxItemPresentation<ParadoxScriptFile>(element)

class ParadoxScriptScriptedVariablePresentation(
    element: ParadoxScriptScriptedVariable
) : ParadoxItemPresentation<ParadoxScriptScriptedVariable>(element)

class ParadoxDefinitionPresentation(
    element: ParadoxScriptProperty,
    private val definitionInfo: ParadoxDefinitionInfo
) : ParadoxItemPresentation<ParadoxScriptProperty>(element) {
    override fun getIcon(unused: Boolean): Icon {
        return PlsIcons.Nodes.Definition(definitionInfo.type)
    }

    override fun getPresentableText(): String {
        return definitionInfo.name.orAnonymous()
    }
}

class ParadoxComplexEnumValuePresentation(
    element: ParadoxScriptStringExpressionElement,
    private val complexEnumValueInfo: ParadoxComplexEnumValueInfo
) : ParadoxItemPresentation<ParadoxScriptStringExpressionElement>(element) {
    override fun getIcon(unused: Boolean): Icon {
        return PlsIcons.Nodes.ComplexEnumValue
    }

    override fun getPresentableText(): String {
        return complexEnumValueInfo.name
    }
}
