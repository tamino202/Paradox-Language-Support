package icu.windea.pls.script.exp

import icu.windea.pls.core.*
import java.text.*

/**
 * @see ParadoxDataExpression
 */
enum class ParadoxDataType(
	val id: String,
	val text: String
) {
	UnknownType("unknown", "(unknown)"),
	BooleanType("boolean", "boolean"),
	IntType("int", "int"),
	FloatType("float", "float"),
	StringType("string", "string"),
	ColorType("color", "color"),
	BlockType("block", "block"),
	
	ParameterType("parameter", "parameter"),
	InlineMathType("inline_math", "inline math");
	
	override fun toString(): String {
		return text
	}
	
	companion object {
		fun resolve(expression: String): ParadoxDataType {
			return when {
				isBooleanYesNo(expression) -> BooleanType
				isInt(expression) -> IntType
				isFloat(expression) -> FloatType
				else -> StringType
			}
		}
		
		fun isBooleanYesNo(expression: String): Boolean {
			return expression == "yes" || expression == "no"
		}
		
		fun isInt(expression: String): Boolean {
			var isFirstChar = true
			val chars = expression.toCharArray()
			for(char in chars) {
				if(char.isExactDigit()) continue
				if(isFirstChar) {
					isFirstChar = false
					if(char == '+' || char == '-') continue
				}
				return false
			}
			return true
		}
		
		fun isFloat(expression: String): Boolean {
			var isFirstChar = true
			var missingDot = true
			val chars = expression.toCharArray()
			for(char in chars) {
				if(char.isExactDigit()) continue
				if(isFirstChar) {
					isFirstChar = false
					if(char == '+' || char == '-') continue
				}
				if(missingDot) {
					if(char == '.') {
						missingDot = false
						continue
					}
				}
				return false
			}
			return true
		}
		
		fun isPercentageField(expression: String): Boolean {
			val chars = expression.toCharArray()
			for(i in expression.indices) {
				val char = chars[i]
				if(i == expression.lastIndex) {
					if(char != '%') return false
				} else {
					if(!char.isExactDigit()) return false
				}
			}
			return true
		}
		
		private val isColorRegex = """(?:rgb|rgba|hsb|hsv|hsl)[ \t]*\{[\d. \t]*}""".toRegex()
		
		fun isColorField(expression: String): Boolean {
			return expression.matches(isColorRegex)
		}
		
		private val threadLocalDateFormat by lazy { ThreadLocal.withInitial { SimpleDateFormat("yyyy.MM.dd") } }
		
		fun isDateField(expression: String): Boolean {
			return try {
				threadLocalDateFormat.get().parse(expression)
				true
			} catch(e: Exception) {
				false
			}
		}
	}
}

fun ParadoxDataType.isBooleanType() = this == ParadoxDataType.BooleanType

fun ParadoxDataType.isIntType() = this == ParadoxDataType.UnknownType || this == ParadoxDataType.IntType || this == ParadoxDataType.ParameterType || this == ParadoxDataType.InlineMathType

fun ParadoxDataType.isFloatType() = this == ParadoxDataType.UnknownType || this == ParadoxDataType.IntType || this == ParadoxDataType.FloatType || this == ParadoxDataType.ParameterType || this == ParadoxDataType.InlineMathType

fun ParadoxDataType.isStringType() = this == ParadoxDataType.UnknownType || this == ParadoxDataType.StringType || this == ParadoxDataType.ParameterType

fun ParadoxDataType.isColorType() = this == ParadoxDataType.ColorType

fun ParadoxDataType.canBeScriptedVariableValue() = this == ParadoxDataType.BooleanType || this == ParadoxDataType.IntType || this == ParadoxDataType.FloatType || this == ParadoxDataType.StringType
