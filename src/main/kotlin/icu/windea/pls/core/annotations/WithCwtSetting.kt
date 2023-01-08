package icu.windea.pls.core.annotations

import icu.windea.pls.config.cwt.config.setting.*
import icu.windea.pls.config.cwt.setting.*
import icu.windea.pls.core.model.*
import kotlin.reflect.*

/**
 * 注明此功能基于指定的CWT配置实现。
 * @property fileName CWT配置文件的文件名。
 * @property settingClass 对应的CWT配置类。
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class WithCwtSetting(
	val fileName: String,
	val settingClass: KClass<out CwtSetting>
)
