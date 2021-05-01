package com.windea.plugin.idea.pls

import com.intellij.util.*
import java.io.*
import java.net.*
import java.nio.file.*
import java.util.*
import javax.swing.*
import kotlin.math.*

fun String.toUrl(locationClass: Class<*>): URL = locationClass.getResource(this)!!

@Suppress("UNCHECKED_CAST")
fun <T> Array<out T?>.cast() = this as Array<T>

inline fun <T, reified R> List<T>.mapArray(block: (T) -> R): Array<R> {
	return Array(size) { block(this[it]) }
}

inline fun <T, reified R> Array<out T>.mapArray(block: (T) -> R): Array<R> {
	return Array(size) { block(this[it]) }
}

inline fun <T, reified R> Sequence<T>.mapArray(block: (T) -> R): Array<R> {
	return this.toList().mapArray(block)
}

fun CharSequence.surroundsWith(prefix:Char,suffix:Char,ignoreCase: Boolean = false): Boolean {
	return this.startsWith(prefix,ignoreCase) && this.endsWith(suffix,ignoreCase)
}

fun CharSequence.surroundsWith(prefix:CharSequence,suffix:CharSequence,ignoreCase: Boolean = false): Boolean {
	return this.startsWith(prefix,ignoreCase) && this.endsWith(suffix,ignoreCase)
}

fun String.containsBlank(): Boolean {
	return any { it.isWhitespace() }
}

fun String.containsLineBreak():Boolean{
	return any{ it == '\n' || it == '\r' }
}

fun String.containsBlankLine(): Boolean {
	var newLine = 0
	val chars = toCharArray()
	for(i in chars.indices) {
		val char = chars[i]
		if((char == '\r' && chars[i + 1] != '\n') || char == '\n') newLine++
		if(newLine >= 2) return true
	}
	forEach {
		if(it == '\r' || it == '\n') newLine++
	}
	return false
}

fun String.quote() = if(startsWith('"') && endsWith('"')) this else "\"$this\""

fun String.quoteIfNecessary() = if(containsBlank()) quote() else this

private val wildcardBooleanValues = arrayOf("true", "false", "yes", "no")

fun String.quoteAsStringLike() = if(this in wildcardBooleanValues || isFloat() || containsBlank()) quote() else this

fun String.onlyQuoteIfNecessary() = this.unquote().quoteIfNecessary()

fun String.unquote() = if(length >= 2 && startsWith('"') && endsWith('"')) substring(1, length - 1) else this

fun String.truncate(limit: Int) = if(this.length <= limit) this else this.take(limit) + "..."

fun String.toCapitalizedWord(): String {
	return if(isEmpty()) this else this[0].toUpperCase() + this.substring(1)
}

fun String.toCapitalizedWords(): String {
	return buildString {
		var isWordStart = true
		for(c in this@toCapitalizedWords.toCharArray()) {
			when {
				isWordStart -> {
					isWordStart = false
					append(c.toUpperCase())
				}
				c == '_' || c == '-' || c == '.' -> {
					isWordStart = true
					append(' ')
				}
				else -> append(c.toLowerCase())
			}
		}
	}
}

fun CharSequence.indicesOf(char: Char, ignoreCase: Boolean = false): MutableList<Int> {
	val indices = mutableListOf<Int>()
	var lastIndex = indexOf(char, 0, ignoreCase)
	while(lastIndex != -1) {
		indices += lastIndex
		lastIndex = indexOf(char, lastIndex + 1, ignoreCase)
	}
	return indices
}

fun <K,V> Map<K,V>.find(predicate:(Map.Entry<K,V>)->Boolean):V?{
	for(entry in this) {
		if(predicate(entry)) return entry.value
	}
	throw NoSuchElementException()
}

fun <K,V> Map<K,V>.findOrNull(predicate:(Map.Entry<K,V>)->Boolean):V?{
	for(entry in this) {
		if(predicate(entry)) return entry.value
	}
	return null
}

inline fun <reified T> Any?.cast(): T = this as T

inline fun <reified T> Any?.castOrNull(): T? = this as? T

fun Icon.resize(width: Int, height: Int = width): Icon {
	return IconUtil.toSize(this, width, height)
}

fun <C : CharSequence> C.ifNotEmpty(block: (C) -> Unit) {
	if(this.isNotEmpty()) block(this)
}

/**
 * 判断当前路径是否匹配另一个路径（等于或者是另一个路径的父路径）。
 */
infix fun String.matchesPath(other: String): Boolean {
	if(this == other) return true
	if(this == other.take(length) && other[length] == '/') return true
	return false
}

inline fun <reified T> T.toSingletonArray(): Array<T> {
	return arrayOf(this)
}

inline fun <reified T> Sequence<T>.toArray(): Array<T> {
	return this.toList().toTypedArray()
}

fun <T> T.toSingletonList(): List<T> {
	return Collections.singletonList(this)
}

fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
	return if(this == null) Collections.emptyList() else Collections.singletonList(this)
}

//Is Extensions

private val isColorRegex = """(rgb|rgba|hsb|hsv|hsl)[ \u00a0\t]*\{[0-9. \u00a0\t]*}""".toRegex()

fun String.isBoolean() = this == "yes" || this == "no"

fun String.isInt():Boolean{
	var isFirstChar = true
	for(char in this.toCharArray()) {
		if(char.isDigit()) continue
		if(isFirstChar) {
			isFirstChar = false
			if(char == '+' || char == '-') continue
		}
		return false
	}
	return true
}

fun String.isFloat(): Boolean {
	var isFirstChar = true
	var missingDot = true
	for(char in this.toCharArray()) {
		if(char.isDigit()) continue
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

fun String.isColor():Boolean{
	return this.matches(isColorRegex)
}

fun String.isTypeOf(type:String):Boolean{
	return (type=="boolean" && isBoolean()) || (type == "int" && isInt()) || (type =="float" && isFloat())
		|| (type == "color" && isColor()) || type == "string"
}

//To Extensions

fun Boolean.toInt() = if(this) 1 else 0

fun Boolean.toStringYesNo() = if(this) "yes" else "no"

fun String.toBooleanYesNo() = this == "yes"

fun URL.toFile(): File {
	return File(this.toURI())
}

fun URL.toPath(): Path {
	return Paths.get(this.toURI())
}

//Specific Collections

class ReversibleList<T>(list:List<T>, val reverse:Boolean = false):List<T> by list

class ReversibleMap<K,V>(map:Map<K,V>, val reverse:Boolean = false):Map<K,V> by map

//Specific Expressions

interface Expression : CharSequence {
	val expression: String
}

abstract class AbstractExpression(override val expression: String):Expression{
	override val length get() = expression.length
	
	override fun get(index: Int) = expression.get(index)
	
	override fun subSequence(startIndex: Int, endIndex: Int) = expression.subSequence(startIndex, endIndex)
	
	override fun equals(other: Any?): Boolean = other?.javaClass == javaClass && expression == (other as Expression).expression
	
	override fun hashCode(): Int = expression.hashCode()
	
	override fun toString(): String = expression
}

/**
 * 范围表达式，用于表示限定脚本文件的属性的出现数量。
 *
 * @property min 最小值
 * @property max 最大值，null表示无限
 * @property limitMax 如果值为`false`，则表示出现数量超出最大值时不警告
 */
class RangeExpression(expression: String):AbstractExpression(expression){
	val min:Int
	val max:Int?
	val limitMax:Boolean
	
	init {
		when{
			expression.isEmpty() -> {
				min=0
				max = null
				limitMax = false
			}
			expression.first() == '~' -> {
				val firstDotIndex = expression.indexOf('.')
				min = expression.substring(1,firstDotIndex).toIntOrNull()?: 0
				max = expression.substring(firstDotIndex+2).toIntOrNull()?:0
				limitMax = true
			}
			else -> {
				val firstDotIndex = expression.indexOf('.')
				min = expression.substring(0,firstDotIndex).toIntOrNull()?: 0
				max = expression.substring(firstDotIndex+2).toIntOrNull()?:0
				limitMax = false
			}
		}
	}
	
	operator fun component1():Int = min
	
	operator fun component2():Int? = max
	
	operator fun component3():Boolean = limitMax
}

class ConfigExpression(expression: String):AbstractExpression(expression){

}

/**
 * 条件表达式，如：`name?`, `name!`。
 */
class ConditionalExpression(expression: String):AbstractExpression(expression){
	companion object {
		private val markers = charArrayOf('?', '!', '*', '+')
	}
	
	val marker: Char? = expression.lastOrNull()?.takeIf { it in markers }
	val value: String = if(marker != null) expression.dropLast(1) else expression
	val optional: Boolean = marker == '?' || marker == '*'
	val required: Boolean = marker == '!' || marker == '+'
	val multiple: Boolean = marker == '*' || marker == '+'
	
	operator fun component1(): String = value
	
	operator fun component2(): Boolean = optional
	
	operator fun component3(): Boolean = required
	
	operator fun component4(): Boolean = multiple
}

/**
 * 预测表达式，如：`isValid`, `!isValid`。
 */
class PredicateExpression(expression: String):AbstractExpression(expression){
	val marker: Char? = expression.firstOrNull()?.takeIf { it == '!' }
	val value: String = if(marker != null) expression.drop(1) else expression
	val invert: Boolean = marker == '!'
	
	operator fun component1(): String = value
	
	operator fun component2(): Boolean = invert
	
	fun matches(other:String):Boolean{
		return if(invert) value != other else value == other
	}
	
	fun matches(other:List<String>):Boolean{
		return if(invert) value !in other else value !in other
	}
	
	inline fun <T> matches(other:List<T>,selector:(T)->String):Boolean{
		return if(invert) other.all{ value != selector(it) } else other.any { value == selector(it) }
	}
}

/**
 * 类型表达式，如：`weapon`, `weapon.sword`, `weapon.(sword|spear)`
 */
class TypeExpression(expression: String):AbstractExpression(expression){
	private val dotIndex = expression.indexOf('.').let{ if(it == -1) expression.length else it }
	 val type = expression.take(dotIndex)
	 val subtypes = expression.drop(dotIndex).let{
		 if(it.surroundsWith('(',')')) it.substring(1,it.length-1).split('|').map { s -> s.trim() } else listOf(it)
	 }
	
	operator fun component1(): String = type
	
	operator fun component2(): List<String> = subtypes
}