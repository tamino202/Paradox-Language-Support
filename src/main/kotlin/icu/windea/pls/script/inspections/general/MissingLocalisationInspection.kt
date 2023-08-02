package icu.windea.pls.script.inspections.general

import com.intellij.codeInspection.*
import com.intellij.openapi.progress.*
import com.intellij.psi.*
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.*
import com.intellij.util.xmlb.annotations.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.core.codeInsight.generation.*
import icu.windea.pls.core.ui.*
import icu.windea.pls.lang.*
import icu.windea.pls.lang.cwt.config.*
import icu.windea.pls.model.codeInsight.*
import icu.windea.pls.script.psi.*
import javax.swing.*

/**
 * 缺失的本地化的检查。
 * @property locales 要检查的语言区域。默认检查英文。
 * @property checkForPreferredLocale 是否同样检查主要的语言区域。默认为true。
 * @property checkForDefinitions 是否检查定义。默认为true。
 * @property checkPrimaryForDefinitions 是否同样检查定义的主要的相关本地化，默认为true。
 * @property checkOptionalForDefinitions 是否同样检查定义的可选的相关本地化，默认为false。
 * @property checkForModifiers 是否检查修正。默认为false。
 */
class MissingLocalisationInspection : LocalInspectionTool() {
    @JvmField var checkForPreferredLocale = true
    @JvmField var checkForSpecificLocales = true
    @OptionTag(converter = CommaDelimitedStringSetConverter::class)
    @JvmField var locales = mutableSetOf<String>()
    @JvmField var checkForDefinitions = true
    @JvmField var checkPrimaryForDefinitions = false
    @JvmField var checkOptionalForDefinitions = false
    @JvmField var checkGeneratedModifierNamesForDefinitions = true
    @JvmField var checkGeneratedModifierDescriptionsForDefinitions = false
    @JvmField var checkForModifiers = true
    @JvmField var checkModifierNames = true
    @JvmField var checkModifierDescriptions = false
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val allLocaleMap = ParadoxLocaleHandler.getLocaleConfigMapById()
        val locales = mutableSetOf<CwtLocalisationLocaleConfig>()
        if(checkForPreferredLocale) locales.add(ParadoxLocaleHandler.getPreferredLocale())
        if(checkForSpecificLocales) this.locales.mapNotNullTo(locales) { allLocaleMap.get(it) }
        if(locales.isEmpty()) return PsiElementVisitor.EMPTY_VISITOR
        return object : PsiElementVisitor() {
            var inFileContext: GenerateLocalisationsInFileContext? = null
            
            override fun visitElement(element: PsiElement) {
                ProgressManager.checkCanceled()
                when(element) {
                    is ParadoxScriptDefinitionElement -> visitDefinition(element)
                    is ParadoxScriptStringExpressionElement -> visitStringExpressionElement(element)
                }
            }
            
            private fun visitDefinition(definition: ParadoxScriptDefinitionElement) {
                val codeInsightInfos = ParadoxLocalisationCodeInsightInfo.fromDefinition(definition, locales, this@MissingLocalisationInspection)
                if(codeInsightInfos.isNullOrEmpty()) return
                val location = if(definition is ParadoxScriptProperty) definition.propertyKey else definition
                registerProblems(codeInsightInfos, location, holder)
            }
            
            private fun visitStringExpressionElement(element: ParadoxScriptStringExpressionElement) {
                val codeInsightInfos = ParadoxLocalisationCodeInsightInfo.fromModifier(element, locales, this@MissingLocalisationInspection)
                if(codeInsightInfos.isNullOrEmpty()) return
                val location = element
                registerProblems(codeInsightInfos, location, holder)
            }
            
            private fun registerProblems(codeInsightInfos: List<ParadoxLocalisationCodeInsightInfo>, element: PsiElement, holder: ProblemsHolder) {
                val messages = getMessages(codeInsightInfos)
                if(messages.isEmpty()) return
                val fixes = getFixes(codeInsightInfos, element).toTypedArray()
                for(message in messages) {
                    //显示为WEAK_WARNING
                    holder.registerProblem(element, message, ProblemHighlightType.WEAK_WARNING, *fixes)
                }
            }
            
            private fun getMessages(codeInsightInfos: List<ParadoxLocalisationCodeInsightInfo>): List<String> {
                val includeMap = mutableMapOf<String, ParadoxLocalisationCodeInsightInfo>()
                val excludeKeys = mutableSetOf<String>()
                for(codeInsightInfo in codeInsightInfos) {
                    if(!codeInsightInfo.check) continue
                    val key = codeInsightInfo.key ?: continue
                    if(excludeKeys.contains(key)) continue
                    if(codeInsightInfo.missing) {
                        includeMap.putIfAbsent(key, codeInsightInfo)
                    } else {
                        includeMap.remove(key)
                        excludeKeys.add(key)
                    }
                }
                return includeMap.values.mapNotNull { getMessage(it) }
            }
            
            private fun getMessage(codeInsightInfo: ParadoxLocalisationCodeInsightInfo): String? {
                val locationExpression = codeInsightInfo.relatedLocalisationInfo?.locationExpression
                val from = locationExpression?.propertyName?.let { PlsBundle.message("inspection.script.general.missingLocalisation.from.2", it) }
                    ?: codeInsightInfo.name?.let { PlsBundle.message("inspection.script.general.missingLocalisation.from.1", it) }
                    ?: return null
                val localeId = codeInsightInfo.locale.id
                return PlsBundle.message("inspection.script.general.missingLocalisation.description", from, localeId)
            }
            
            private fun getFixes(codeInsightInfos: List<ParadoxLocalisationCodeInsightInfo>, element: PsiElement): List<LocalQuickFix> {
                return emptyList()
                //return buildList {
                //    val context = GenerateLocalisationsContext(definitionInfo.name, contextMap.mapNotNullTo(mutableSetOf()) { it.value.key })
                //    add(GenerateLocalisationsFix(context, definition))
                //    if(inFileContext == null) {
                //        val fileName = definition.containingFile.name
                //        inFileContext = GenerateLocalisationsInFileContext(fileName, mutableListOf())
                //    }
                //    val inFileContext = inFileContext!!
                //    inFileContext.contextList.add(context)
                //    add(GenerateLocalisationsInFileFix(inFileContext, definition))
                //}
            }
        }
    }
    
    override fun createOptionsPanel(): JComponent {
        lateinit var checkForDefinitionsCb: Cell<JBCheckBox>
        lateinit var checkForModifiersCb: Cell<JBCheckBox>
        return panel {
            //checkForPreferredLocale
            row {
                checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForPreferredLocale"))
                    .bindSelected(::checkForPreferredLocale)
                    .actionListener { _, component -> checkForPreferredLocale = component.isSelected }
                cell(ActionLink(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForPreferredLocale.configure")) {
                    //ShowSettingsUtil.getInstance().showSettingsDialog(null, ParadoxSettingsConfigurable::class.java)
                    val dialog = ParadoxPreferredLocaleDialog()
                    dialog.showAndGet()
                })
            }
            //checkForSpecificLocales
            row {
                checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForSpecificLocales"))
                    .bindSelected(::checkForSpecificLocales)
                    .actionListener { _, component -> checkForSpecificLocales = component.isSelected }
                cell(ActionLink(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForSpecificLocales.configure")) {
                    val allLocaleMap = ParadoxLocaleHandler.getLocaleConfigMapById(pingPreferred = false)
                    val selectedLocales = locales.mapNotNull { allLocaleMap.get(it) }
                    val dialog = ParadoxLocaleCheckBoxDialog(selectedLocales, allLocaleMap.values)
                    if(dialog.showAndGet()) {
                        val newLocales = dialog.localeStatusMap.mapNotNullTo(mutableSetOf()) { (k, v) -> if(v) k.id else null }
                        locales = newLocales
                    }
                })
            }
            //checkForDefinitions
            row {
                checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForDefinitions"))
                    .bindSelected(::checkForDefinitions)
                    .actionListener { _, component -> checkForDefinitions = component.isSelected }
                    .also { checkForDefinitionsCb = it }
            }
            indent {
                //checkRequiredForDefinitions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkRequiredForDefinitions"))
                        .selected(true)
                        .enabled(false)
                }
                //checkPrimaryForDefinitions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkPrimaryForDefinitions"))
                        .bindSelected(::checkPrimaryForDefinitions)
                        .actionListener { _, component -> checkPrimaryForDefinitions = component.isSelected }
                        .enabledIf(checkForDefinitionsCb.selected)
                }
                //checkOptionalForDefinitions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkOptionalForDefinitions")).apply {
                        bindSelected(::checkOptionalForDefinitions)
                            .actionListener { _, component -> checkOptionalForDefinitions = component.isSelected }
                            .enabledIf(checkForDefinitionsCb.selected)
                    }
                }
                //checkGeneratedModifierNamesForDefinitions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkGeneratedModifierNamesForDefinitions")).apply {
                        bindSelected(::checkGeneratedModifierNamesForDefinitions)
                            .actionListener { _, component -> checkGeneratedModifierNamesForDefinitions = component.isSelected }
                            .enabledIf(checkForDefinitionsCb.selected)
                    }
                }
                //checkGeneratedModifierDescriptionsForDefinitions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkGeneratedModifierDescriptionsForDefinitions")).apply {
                        bindSelected(::checkGeneratedModifierDescriptionsForDefinitions)
                            .actionListener { _, component -> checkGeneratedModifierDescriptionsForDefinitions = component.isSelected }
                            .enabledIf(checkForDefinitionsCb.selected)
                    }
                }
            }
            //checkForModifiers
            row {
                checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkForModifiers"))
                    .bindSelected(::checkForModifiers)
                    .actionListener { _, component -> checkForModifiers = component.isSelected }
                    .also { checkForModifiersCb = it }
            }
            indent {
                //checkModifierNames
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkModifierNames"))
                        .bindSelected(::checkModifierNames)
                        .actionListener { _, component -> checkModifierNames = component.isSelected }
                        .enabledIf(checkForModifiersCb.selected)
                }
                //checkModifierDescriptions
                row {
                    checkBox(PlsBundle.message("inspection.script.general.missingLocalisation.option.checkModifierDescriptions"))
                        .bindSelected(::checkModifierDescriptions)
                        .actionListener { _, component -> checkModifierDescriptions = component.isSelected }
                        .enabledIf(checkForModifiersCb.selected)
                }
            }
        }
    }
}
