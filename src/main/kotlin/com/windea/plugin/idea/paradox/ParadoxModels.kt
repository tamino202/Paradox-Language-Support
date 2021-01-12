package com.windea.plugin.idea.paradox

import com.intellij.util.ui.*
import java.awt.*
import java.util.*

//File

class ParadoxPath(
	val subPaths:List<String>
){
	val path = subPaths.joinToString("/")
	val fileName = subPaths.lastOrNull().orEmpty()
	val fileExtension = fileName.substringAfterLast('.')
	val parentSubPaths = subPaths.dropLast(1)
	val parent = parentSubPaths.joinToString("/")
	val root = parentSubPaths.firstOrNull().orEmpty()
	val length = subPaths.size
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxPath && path == other.path
	}
	
	override fun hashCode(): Int {
		return path.hashCode()
	}
	
	override fun toString(): String {
		return path
	}
}

enum class ParadoxFileType{
	Script,
	Localisation
}

enum class ParadoxGameType(
	val key: String,
	val text: String
) {
	Stellaris("stellaris", "Stellaris")
}

enum class ParadoxRootType(
	val key:String,
	val text: String
) {
	Stdlib("stdlib","Stdlib"),
	Mod("mod","Mod"),
	PdxLauncher("pdx_launcher","Paradox Launcher"),
	PdxOnlineAssets("pdx_online_assets","Paradox Online Assets"),
	TweakerGuiAssets("tweakergui_assets","Tweaker GUI Assets")
}

data class ParadoxFileInfo(
	val name: String,
	val path: ParadoxPath,
	val fileType: ParadoxFileType,
	val rootType: ParadoxRootType,
	val gameType: ParadoxGameType
)

//Script

data class ParadoxDefinitionInfo(
	val name:String,
	val type:String,
	val rootKey:String,
	val localisation: List<Pair<ConditionalKey,String>>,
	val scopes:Map<String,String>,
	val fromVersion:String
){
	val localisationKeys = localisation.mapTo(linkedSetOf()) { it.first }
	val localisationNames = localisation.mapTo(linkedSetOf()) { it.second }
	val hasLocalisation = localisation.isNotEmpty()
	val hasScopes = scopes.isNotEmpty()
}

//Localisation

class ParadoxLocale(data:Map<String,Any>) {
	val name:String by data
	val description:String by data
	val popupText = "$name - $description"
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxLocale && name == other.name
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return "ParadoxLocale: $name"
	}
}

class ParadoxSerialNumber(data:Map<String,Any>) {
	val name: String by data
	val description:String by data
	val placeholderText :String by data
	val popupText = "$name - $description"
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxSerialNumber && name == other.name 
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return "ParadoxSerialNumber: $name"
	}
}

class ParadoxColor(data:Map<String,Any>){
	val name: String by data
	val description: String by data
	val colorRgb:Int by data
	val colorText:String by data

	val popupText = "$name - $description"
	val color: Color = Color(colorRgb)
	val icon = ColorIcon(16, color)
	val gutterIcon = ColorIcon(12, color)
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxColor && name == other.name
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return "ParadoxColor: $name"
	}
}

class ParadoxCommandScope(data:Map<String,Any>) {
	val name:String by data
	val description: String by data
	val isPrimary:Boolean by data
	val isSecondary:Boolean by data
	
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxCommandScope && name == other.name
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return "ParadoxCommandScope: $name"
	}
}

class ParadoxCommandField(data:Map<String,Any>){
	val name:String by data
	val description:String by data
	
	override fun equals(other: Any?): Boolean {
		return this === other || other is ParadoxCommandField && name == other.name
	}
	
	override fun hashCode(): Int {
		return name.hashCode()
	}
	
	override fun toString(): String {
		return "ParadoxCommandField: $name"
	}
}