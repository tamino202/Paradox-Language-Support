package icu.windea.pls.core.annotations

import icu.windea.pls.config.core.*
import icu.windea.pls.config.core.config.*
import icu.windea.pls.core.model.*

/**
 * 注明此功能仅限于指定的游戏类型。
 * @property value 游戏类型。
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class WithGameType(
	vararg val value: ParadoxGameType
)