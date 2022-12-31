package icu.windea.pls.config.cwt.expression

import com.google.common.cache.*
import icu.windea.pls.core.*

/**
 * CWT值表达式。
 * @property type 类型
 * @property value 值
 */
class CwtValueExpression private constructor(
	expressionString: String,
	override val type: CwtDataType,
	override val value: String? = null,
	override val extraValue: Any? = null
) : AbstractExpression(expressionString), CwtDataExpression {
	companion object Resolver {
		val EmptyExpression = CwtValueExpression("", CwtDataType.Constant, "")
		val BlockExpression = CwtValueExpression("{...}", CwtDataType.Any, "{...}")
		
		val cache by lazy { CacheBuilder.newBuilder().buildCache<String, CwtValueExpression> { doResolve(it) } }
		
		/**
		 * @param isParameterValue 参数的值的类型在规则文件中写作"scalar"的场合，实际上只要不是子句就可以。
		 */
		fun resolve(expressionString: String) = cache.getUnchecked(expressionString)
		
		private fun doResolve(expressionString: String) = when {
			expressionString.isEmpty() -> EmptyExpression
			expressionString == "any" -> {
				CwtValueExpression(expressionString, CwtDataType.Any)
			}
			expressionString == "bool" -> {
				CwtValueExpression(expressionString, CwtDataType.Bool)
			}
			expressionString == "int" -> {
				CwtValueExpression(expressionString, CwtDataType.Int)
			}
			expressionString.surroundsWith("int[", "]") -> {
				val range = expressionString.substring(4, expressionString.length - 1)
					.split("..", limit = 2)
					.let { tupleOf(it.getOrNull(0)?.toIntOrNull() ?: 0, it.getOrNull(1)?.toIntOrNull()) }
				CwtValueExpression(expressionString, CwtDataType.Int, null, range)
			}
			expressionString == "float" -> {
				CwtValueExpression(expressionString, CwtDataType.Float)
			}
			expressionString.surroundsWith("float[", "]") -> {
				val range = expressionString.substring(6, expressionString.length - 1)
					.split("..", limit = 2)
					.let { tupleOf(it.getOrNull(0)?.toIntOrNull() ?: 0, it.getOrNull(1)?.toIntOrNull()) }
				CwtValueExpression(expressionString, CwtDataType.Float, null, range)
			}
			expressionString == "scalar" -> {
				CwtValueExpression(expressionString, CwtDataType.Scalar)
			}
			expressionString == "colour_field" -> {
				CwtValueExpression(expressionString, CwtDataType.ColorField)
			}
			expressionString.surroundsWith("colour[", "]") -> {
				val value = expressionString.substring(7, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.ColorField, value)
			}
			expressionString == "percentage_field" -> {
				CwtValueExpression(expressionString, CwtDataType.PercentageField)
			}
			expressionString == "date_field" -> {
				CwtValueExpression(expressionString, CwtDataType.DateField)
			}
			expressionString == "localisation" -> {
				CwtValueExpression(expressionString, CwtDataType.Localisation)
			}
			expressionString == "localisation_synced" -> {
				CwtValueExpression(expressionString, CwtDataType.SyncedLocalisation)
			}
			expressionString == "localisation_inline" -> {
				CwtValueExpression(expressionString, CwtDataType.InlineLocalisation)
			}
			//for stellaris
			expressionString.surroundsWith("stellaris_name_format[", "]") -> {
				val value = expressionString.substring(22, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.StellarisNameFormat, value)
			}
			//EXTENDED BY PLS
			expressionString == "abs_filepath" -> {
				CwtValueExpression(expressionString, CwtDataType.AbsoluteFilePath)
			}
			expressionString == "filepath" -> {
				CwtValueExpression(expressionString, CwtDataType.FilePath)
			}
			expressionString.surroundsWith("filepath[", "]") -> {
				val value = expressionString.substring(9, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.FilePath, value)
			}
			expressionString.surroundsWith("icon[", "]") -> {
				val value = expressionString.substring(5, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.Icon, value)
			}
			expressionString.surroundsWith('<', '>') -> {
				val value = expressionString.substring(1, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.TypeExpression, value)
			}
			expressionString.indexOf('<').let { it > 0 && it < expressionString.indexOf('>') } -> {
				val value = expressionString.substring(expressionString.indexOf('<'), expressionString.indexOf('>'))
				val extraValue = expressionString.substringBefore('<') to expressionString.substringAfterLast('>')
				CwtValueExpression(expressionString, CwtDataType.TypeExpressionString, value, extraValue)
			}
			expressionString.surroundsWith("value[", "]") -> {
				val value = expressionString.substring(6, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.Value, value)
			}
			expressionString.surroundsWith("value_set[", "]") -> {
				val value = expressionString.substring(10, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.ValueSet, value)
			}
			expressionString.surroundsWith("enum[", "]") -> {
				val value = expressionString.substring(5, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.Enum, value)
			}
			expressionString == "scope_field" -> {
				CwtValueExpression(expressionString, CwtDataType.ScopeField)
			}
			expressionString.surroundsWith("scope[", "]") -> {
				//value需要是有效的scope_type
				val value = expressionString.substring(6, expressionString.length - 1).takeIf { it != "any" }
				CwtValueExpression(expressionString, CwtDataType.Scope, value)
			}
			expressionString.surroundsWith("scope_group[", "]") -> {
				val value = expressionString.substring(12, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.ScopeGroup, value)
			}
			expressionString == "value_field" -> {
				CwtValueExpression(expressionString, CwtDataType.ValueField)
			}
			expressionString.surroundsWith("value_field[", "]") -> {
				val value = expressionString.substring(12, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.ValueField, value)
			}
			expressionString == "int_value_field" -> {
				CwtValueExpression(expressionString, CwtDataType.IntValueField)
			}
			expressionString.surroundsWith("int_value_field[", "]") -> {
				val value = expressionString.substring(16, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.IntValueField, value)
			}
			expressionString == "variable_field" -> {
				CwtValueExpression(expressionString, CwtDataType.VariableField)
			}
			expressionString == "variable_field_32" -> {
				CwtValueExpression(expressionString, CwtDataType.VariableField)
			}
			expressionString.surroundsWith("variable_field[", "]") -> {
				val value = expressionString.substring(15, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.VariableField, value)
			}
			expressionString == "int_variable_field" -> {
				CwtValueExpression(expressionString, CwtDataType.IntVariableField)
			}
			expressionString == "int_variable_field_32" -> {
				CwtValueExpression(expressionString, CwtDataType.IntVariableField)
			}
			expressionString.surroundsWith("int_variable_field[", "]") -> {
				val value = expressionString.substring(19, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.IntVariableField, value)
			}
			expressionString.surroundsWith("single_alias_right[", "]") -> {
				val value = expressionString.substring(19, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.SingleAliasRight, value)
			}
			expressionString.surroundsWith("alias_keys_field[", "]") -> {
				val value = expressionString.substring(17, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.AliasKeysField, value)
			}
			expressionString.surroundsWith("alias_match_left[", "]") -> {
				val value = expressionString.substring(17, expressionString.length - 1)
				CwtValueExpression(expressionString, CwtDataType.AliasMatchLeft, value)
			}
			expressionString.endsWith(']') -> {
				CwtValueExpression(expressionString, CwtDataType.Other)
			}
			else -> {
				CwtValueExpression(expressionString, CwtDataType.Constant, expressionString)
			}
		}
	}
	
	operator fun component1() = type
	
	operator fun component2() = value
}
