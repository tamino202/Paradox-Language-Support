package icu.windea.pls.core.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.*

@State(name = "ParadoxFoldingSettings", storages = [Storage("editor.xml")], category = SettingsCategory.CODE)
class ParadoxFoldingSettings : PersistentStateComponent<ParadoxFoldingSettings>{
	var collapseVariableOperationExpressions = false
	
	override fun getState() = this
	
	override fun loadState(state: ParadoxFoldingSettings) = XmlSerializerUtil.copyBean(state, this)
	
	companion object {
		@JvmStatic
		fun getInstance() = service<ParadoxFoldingSettings>()
	}
}
