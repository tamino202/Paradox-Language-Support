package icu.windea.pls.config.cwt

import com.intellij.openapi.project.*
import icu.windea.pls.config.cwt.config.*
import icu.windea.pls.model.*
import java.util.concurrent.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@Suppress("unused")
class CwtConfigGroups(
	project: Project,
	cwtFileConfigGroups: Map<String, Map<String, CwtFileConfig>>
) {
	val groups: Map<String, CwtConfigGroup>
	
	init {
		//初始化各个游戏分组的CWT规则
		groups = ConcurrentHashMap()
		for((groupName, cwtFileConfigs) in cwtFileConfigGroups.entries) {
			val gameType = ParadoxGameType.resolve(groupName)
			if(gameType != null) {
				groups[groupName] = CwtConfigGroup(gameType, project, cwtFileConfigs)
			}
		}
	}
	
	val ck2 get() = getValue(ParadoxGameType.Ck2)
	val ck3 get() = getValue(ParadoxGameType.Ck3)
	val eu4 get() = getValue(ParadoxGameType.Eu4)
	val hoi4 get() = getValue(ParadoxGameType.Hoi4)
	val ir get() = getValue(ParadoxGameType.Ir)
	val stellaris get() = getValue(ParadoxGameType.Stellaris)
	val vic2 get() = getValue(ParadoxGameType.Vic2)
	
	operator fun get(key: String) = groups.get(key)
	fun getValue(key: String) = groups.getValue(key)
	
	operator fun get(key: ParadoxGameType) = groups.get(key.id)
	fun getValue(key: ParadoxGameType) = groups.getValue(key.id)
}