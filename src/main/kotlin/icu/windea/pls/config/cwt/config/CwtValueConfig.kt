package icu.windea.pls.config.cwt.config

import com.intellij.psi.*
import icu.windea.pls.config.cwt.expression.*
import icu.windea.pls.cwt.psi.*

data class CwtValueConfig(
	override val pointer: SmartPsiElementPointer<CwtValue>,
	override val info: CwtConfigInfo,
	override val value: String,
	override val booleanValue: Boolean? = null,
	override val intValue: Int? = null,
	override val floatValue: Float? = null,
	override val stringValue: String? = null,
	override val properties: List<CwtPropertyConfig>? = null,
	override val values: List<CwtValueConfig>? = null,
	override val documentation: String? = null,
	override val options: List<CwtOptionConfig>? = null,
	override val optionValues: List<CwtOptionValueConfig>? = null
) : CwtKvConfig<CwtValue>() {
	//val stringValues by lazy { values?.mapNotNull { it.stringValue } }
	//val stringValueOrValues by lazy { stringValue?.toSingletonList() ?: values?.mapNotNull { it.stringValue } }
	
	val valueExpression: CwtValueExpression = if(stringValue == null) CwtValueExpression.EmptyExpression else CwtValueExpression.resolve(stringValue)
	override val expression: CwtValueExpression get() = valueExpression
	
	override val resolved: CwtValueConfig get() = this
	
	val isTagConfig = optionValues?.any { it.stringValue == "tag" } == true
}