package icu.windea.pls.extension.diagram.settings.impl

import com.intellij.openapi.application.*
import com.intellij.openapi.components.*
import com.intellij.openapi.options.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.ThreeStateCheckBox.State.*
import com.intellij.util.xmlb.annotations.*
import icu.windea.pls.*
import icu.windea.pls.core.annotations.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.search.*
import icu.windea.pls.core.search.selector.chained.*
import icu.windea.pls.core.ui.*
import icu.windea.pls.extension.diagram.*
import icu.windea.pls.extension.diagram.provider.*
import icu.windea.pls.extension.diagram.settings.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.model.*
import icu.windea.pls.tool.localisation.*
import javax.swing.*

@WithGameType(ParadoxGameType.Stellaris)
@Service(Service.Level.PROJECT)
@State(name = "ParadoxDiagramSettings.Stellaris.TechnologyTree", storages = [Storage("paradox-language-support.xml")])
class StellarisTechnologyTreeDiagramSettings(
    val project: Project
) : ParadoxTechnologyTreeDiagramSettings<StellarisTechnologyTreeDiagramSettings.State>(State()) {
    companion object {
        const val ID = "pls.diagram.Stellaris.TechnologyTree"
    }
    
    override val id: String = ID
    override val configurableClass: Class<out Configurable> = StellarisTechnologyTreeDiagramSettingsConfigurable::class.java
    
    class State : ParadoxDiagramSettings.State() {
        override var scopeType by string()
        
        @get:XMap
        var type by linkedMap<String, Boolean>()
        @get:XMap
        var tier by linkedMap<String, Boolean>()
        @get:XMap
        var area by linkedMap<String, Boolean>()
        @get:XMap
        var category by linkedMap<String, Boolean>()
        
        val typeSettings = TypeSettings()
        
        val areaNames = mutableMapOf<String, () -> String?>()
        val categoryNames = mutableMapOf<String, () -> String?>()
        
        fun a(){}
        
        inner class TypeSettings {
            val start by type withDefault true
            val rare by type withDefault true
            val dangerous by type withDefault true
            val insight by type withDefault true
            val repeatable by type withDefault true
        }
    }
    
    override fun initSettings() {
        //it.name is ok here
        val tiers = StellarisTechnologyHandler.getTechnologyTiers(project, null)
        tiers.forEach { state.tier.putIfAbsent(it.name, true) }
        val areas = StellarisTechnologyHandler.getResearchAreas()
        areas.forEach { state.area.putIfAbsent(it, true) }
        val categories = StellarisTechnologyHandler.getTechnologyCategories(project, null)
        categories.forEach { state.category.putIfAbsent(it.name, true) }
        areas.forEach { state.areaNames.put(it) { ParadoxPresentationHandler.getText(it.uppercase(), project) } }
        categories.forEach { state.categoryNames.put(it.name) { ParadoxPresentationHandler.getNameText(it) } }
        super.initSettings()
    }
}