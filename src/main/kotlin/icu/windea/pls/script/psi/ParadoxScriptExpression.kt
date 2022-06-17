package icu.windea.pls.script.psi

import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.model.*

/**
 * @property definitionType 定义的类型。基于CWT规则。
 * @property configExpression 定义元素对应的规则表达式。基于CWT规则。
 * @property valueType 值的类型。基于PSI元素的类型。
 * @see icu.windea.pls.core.ParadoxValueType
 */
interface ParadoxScriptExpression : PsiElement {
	val definitionType: String? get() = null
	val configExpression: String? get() = null
	val valueType: ParadoxValueType? get() = null
}