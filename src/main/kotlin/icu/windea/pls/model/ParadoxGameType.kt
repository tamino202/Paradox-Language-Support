package icu.windea.pls.model

import icu.windea.pls.lang.*

/**
 * @property id ID。
 * @property title 标题。
 * @property gameId `launcher-settings.json`中使用到的游戏ID。
 * @property steamId Steam使用到的游戏ID。
 * @property entryPaths 作为入口的根目录相对于游戏根目录的路径。匹配CWT规则时，除了游戏根目录之外，也需要基于这些目录。
 */
enum class ParadoxGameType(
    val id: String,
    val title: String,
    val gameId: String,
    val steamId: String,
) {
    Stellaris("stellaris", "Stellaris", "stellaris", "281990") {
        override val entryPaths = setOf("pdx_launcher/game", "pdx_launcher/common", "pdx_online_assets", "previewer_assets", "tweakergui_assets")
    },
    Ck2("ck2", "Crusader Kings II", "ck2", "203770") {
        override val entryPaths = setOf("jomini")
    },
    Ck3("ck3", "Crusader Kings III", "ck3", "1158310") {
        override val entryPaths = setOf("jomini")
    },
    Eu4("eu4", "Europa Universalis IV", "eu4", "236850") {
        override val entryPaths = setOf("tweakergui_assets")
    },
    Hoi4("hoi4", "Hearts of Iron IV", "hoi4", "394360") {
        override val entryPaths = setOf("jomini")
    },
    Ir("ir", "Imperator: Rome", "ir", "859580") {
        override val entryPaths = setOf("jomini")
    },
    Vic2("vic2", "Victoria 2", "victoria2", "42960") {
        override val entryPaths = setOf("jomini")
    },
    Vic3("vic3", "Victoria 3", "victoria3", "529340") {
        override val entryPaths = setOf("jomini")
    },
    ;

    open val entryPaths: Set<String> = emptySet()

    override fun toString() = title

    companion object {
        private val valueMap = entries.associateBy { it.id }

        @JvmStatic
        fun resolve(id: String) = valueMap[id]

        @JvmStatic
        fun canResolve(id: String) = id == "core" || valueMap.containsKey(id)

        @JvmStatic
        fun placeholder() = Stellaris
    }
}

val ParadoxGameType?.id get() = this?.id ?: "core"

val ParadoxGameType?.title get() = this?.title ?: "Core"

val ParadoxGameType?.prefix get() = if (this == null) "" else "${id}:"

fun ParadoxGameType?.orDefault() = this ?: getSettings().defaultGameType
