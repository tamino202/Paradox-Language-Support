package icu.windea.pls.config.cwt.setting

data class CwtPostfixTemplateSetting(
	override val id: String,
	val key: String,
	val example: String?,
	val variables: Map<String, String>, //variableName - defaultValue
	val expression: String
): CwtSetting