package icu.windea.pls.lang.codeInsight.completion

import com.intellij.application.options.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.*
import com.intellij.openapi.command.*
import com.intellij.openapi.command.impl.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.ui.*
import com.intellij.util.*
import icons.*
import icu.windea.pls.*
import icu.windea.pls.config.*
import icu.windea.pls.config.config.*
import icu.windea.pls.config.expression.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.lang.ui.*
import icu.windea.pls.lang.util.*
import icu.windea.pls.script.codeStyle.*
import icu.windea.pls.script.psi.*
import javax.swing.*

fun LookupElement.withPriority(priority: Double?, offset: Double = 0.0): LookupElement {
    val scopeMatched = getUserData(PlsKeys.scopeMismatched) != true
    if(priority == null && scopeMatched) return this
    val finalPriority = (priority ?: 0.0) + offset + (if(scopeMatched) 0 else PlsCompletionPriorities.scopeMismatchOffset)
    return PrioritizedLookupElement.withPriority(this, finalPriority)
}

fun LookupElement.withExplicitProximity(explicitProximity: Int): LookupElement {
    return PrioritizedLookupElement.withExplicitProximity(this, explicitProximity)
}

fun LookupElementBuilder.withScopeMatched(scopeMatched: Boolean): LookupElementBuilder {
    if(scopeMatched) return this
    putUserData(PlsKeys.scopeMismatched, true)
    return withItemTextForeground(JBColor.GRAY)
}

@Suppress("UnstableApiUsage", "DialogTitleCapitalization")
fun LookupElementBuilder.withExpandClauseTemplateInsertHandler(
    context: ProcessingContext,
    entryConfigs: List<CwtMemberConfig<*>>
): LookupElementBuilder {
    //如果补全位置所在的子句为空或者都不精确匹配，显示对话框时默认列出的属性/值应该有数种情况，因此这里需要传入entryConfigs
    //默认列出且仅允许选择直接的key为常量字符串的属性（忽略需要内联的情况）
    
    val file = context.parameters?.originalFile ?: return this
    val constantConfigGroupList = mutableListOf<Map<CwtDataExpression, List<CwtMemberConfig<*>>>>()
    val hasRemainList = mutableListOf<Boolean>()
    for(entry in entryConfigs) {
        val constantConfigGroup = entry.configs
            ?.filter { it is CwtPropertyConfig && it.expression.type == CwtDataTypes.Constant }
            ?.groupBy { it.expression }
            .orEmpty()
        if(constantConfigGroup.isEmpty()) continue //skip
        val configList = entry.configs
            ?.distinctBy { it.expression }
            .orEmpty()
        val hasRemain = constantConfigGroup.size != configList.size
        constantConfigGroupList.add(constantConfigGroup)
        hasRemainList.add(hasRemain)
    }
    if(constantConfigGroupList.isEmpty()) return this
    val config = context.config!!
    val propertyName = CwtConfigHandler.getEntryName(config)
    
    return this.withInsertHandler { c, _ ->
        if(context.isKey == true) {
            applyKeyAndValueInsertHandler(c, context, null, true)
        } else {
            applyValueInsertHandler(c, context, true)
        }
        
        c.laterRunnable = Runnable {
            val project = file.project
            val editor = c.editor
            val descriptorsInfoList = constantConfigGroupList.indices.map { i ->
                val descriptors = getDescriptors(constantConfigGroupList[i])
                val hasRemain = hasRemainList[i]
                ElementsInfo(descriptors, hasRemain)
            }
            val descriptorsContext = ElementsContext(project, editor, propertyName, descriptorsInfoList)
            
            val dialog = ExpandClauseTemplateDialog(project, editor, descriptorsContext)
            if(!dialog.showAndGet()) return@Runnable
            
            val descriptors = descriptorsContext.descriptorsInfo.resultDescriptors
            val hasRemain = descriptorsContext.descriptorsInfo.hasRemain
            
            val customSettings = CodeStyle.getCustomSettings(file, ParadoxScriptCodeStyleSettings::class.java)
            val multiline = descriptors.size > getSettings().completion.maxMemberCountInOneLine
            val around = customSettings.SPACE_AROUND_PROPERTY_SEPARATOR
            
            val documentManager = PsiDocumentManager.getInstance(project)
            val command = Runnable {
                documentManager.commitDocument(editor.document)
                val offset = editor.caretModel.offset
                val blockOffset = if(around) offset + 1 else offset
                
                val elementAtCaret = file.findElementAt(blockOffset)?.parent as ParadoxScriptValue
                val clauseText = buildString {
                    append("{")
                    if(multiline) append("\n")
                    descriptors.forEach {
                        when(it) {
                            is ValueDescriptor -> {
                                append(it.name.quoteIfNecessary())
                            }
                            is PropertyDescriptor -> {
                                append(it.name.quoteIfNecessary())
                                if(around) append(" ")
                                append(it.separator)
                                if(around) append(" ")
                                append(it.value.ifEmpty { "v" })
                            }
                        }
                        if(multiline) append("\n") else append(" ")
                    }
                    append("}")
                }
                val clauseElement = ParadoxScriptElementFactory.createValue(project, clauseText)
                val element = elementAtCaret.replace(clauseElement) as ParadoxScriptBlock
                documentManager.doPostponedOperationsAndUnblockDocument(editor.document) //提交文档更改
                
                val startAction = StartMarkAction.start(editor, project, "script.command.expandClauseTemplate.name")
                val templateBuilder = TemplateBuilderFactory.getInstance().createTemplateBuilder(element)
                var i = 0
                element.processChild { e ->
                    if(e is ParadoxScriptProperty || e is ParadoxScriptValue) {
                        val descriptor = descriptors[i]
                        if(descriptor.editInTemplate) {
                            if(e is ParadoxScriptProperty && descriptor is PropertyDescriptor) {
                                val expression = TextExpression(descriptor.value.ifNotEmpty { it.quoteIfNecessary() })
                                templateBuilder.replaceElement(e.propertyValue!!, "${descriptor.name}_$i", expression, true)
                            }
                        }
                        i++
                    }
                    true
                }
                val textRange = element.textRange
                val caretMarker = editor.document.createRangeMarker(textRange.startOffset, textRange.endOffset)
                caretMarker.isGreedyToRight = true
                editor.caretModel.moveToOffset(textRange.startOffset)
                val template = templateBuilder.buildInlineTemplate()
                TemplateManager.getInstance(project).startTemplate(editor, template, TemplateEditingFinishedListener { _, _ ->
                    try {
                        //如果从句中没有其他可能的元素，将光标移到子句之后的位置
                        if(!hasRemain) {
                            editor.caretModel.moveToOffset(caretMarker.endOffset)
                        }
                        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                    } finally {
                        FinishMarkAction.finish(project, editor, startAction)
                    }
                })
            }
            WriteCommandAction.runWriteCommandAction(project, PlsBundle.message("script.command.expandClauseTemplate.name"), null, command, file)
        }
    }
}

fun PlsLookupElementBuilder.build(context: ProcessingContext): PlsLookupElement? {
    if((!scopeMatched || !context.scopeMatched) && getSettings().completion.completeOnlyScopeIsMatched) return null
    
    val config = context.config
    val completeWithValue = getSettings().completion.completeWithValue
    val targetConfig = when {
        config is CwtPropertyConfig -> config
        config is CwtAliasConfig -> config.config
        config is CwtSingleAliasConfig -> config.config
        config is CwtInlineConfig -> config.config
        else -> null
    }
    val constantValue = when {
        completeWithValue -> targetConfig?.valueExpression?.takeIf { it.type == CwtDataTypes.Constant }?.value
        else -> null
    }
    val insertCurlyBraces = when {
        forceInsertCurlyBraces -> true
        completeWithValue -> targetConfig?.isBlock ?: false
        else -> false
    }
    
    val isKeyOrValueOnly = context.contextElement is ParadoxScriptPropertyKey || context.isKey != true
    val isKey = context.isKey == true
    val isBlock = targetConfig?.isBlock ?: false
    
    val finalLookupString = when {
        context.keywordOffset == 0 -> lookupString.quoteIfNecessary()
        else -> lookupString
    }
    //这里ID不一定等同于lookupString
    val id = when {
        isKeyOrValueOnly -> finalLookupString
        constantValue != null -> "$finalLookupString = $constantValue"
        insertCurlyBraces -> "$finalLookupString = {...}"
        else -> finalLookupString
    }
    //排除重复项
    if(context.completionIds?.add(id) == false) return null
    
    var lookupElement = when {
        element != null -> LookupElementBuilder.create(element, finalLookupString) //quote if necessary
        else -> LookupElementBuilder.create(finalLookupString) //quote if necessary
    }
    if(localizedNames.isNotEmpty()) {
        //这样就可以了
        lookupElement = lookupElement.withLookupStrings(localizedNames)
    }
    if(!scopeMatched) {
        lookupElement.putUserData(PlsKeys.scopeMismatched, true)
    }
    if(bold) {
        lookupElement = lookupElement.bold()
    }
    if(italic) {
        lookupElement = lookupElement.withItemTextItalic(true)
    }
    if(underlined) {
        lookupElement = lookupElement.withItemTextUnderlined(true)
    }
    if(strikeout) {
        lookupElement = lookupElement.strikeout()
    }
    if(!caseSensitive) {
        lookupElement = lookupElement.withCaseSensitivity(false)
    }
    if(icon != null) {
        lookupElement = lookupElement.withIcon(getIconToUse(icon, config))
    }
    if(presentableText == null && lookupString != finalLookupString) {
        presentableText = lookupString //always show unquoted lookup string
    }
    if(presentableText != null) {
        lookupElement = lookupElement.withPresentableText(presentableText!!)
    }
    val finalTailText = buildString {
        if(!isKeyOrValueOnly) {
            if(constantValue != null) append(" = ").append(constantValue)
            if(insertCurlyBraces) append(" = {...}")
        }
        if(tailText != null) append(tailText)
    }
    if(finalTailText.isNotEmpty()) {
        lookupElement = lookupElement.withTailText(finalTailText, true)
    }
    if(typeText != null) {
        lookupElement = lookupElement.withTypeText(typeText, typeIcon, true)
    }
    if(!scopeMatched) {
        lookupElement = lookupElement.withItemTextForeground(JBColor.GRAY)
    }
    
    if(isKeyOrValueOnly) {
        lookupElement = lookupElement.withInsertHandler { c, _ ->
            applyKeyOrValueInsertHandler(context, c)
        }
    } else if(isKey) {
        lookupElement = lookupElement.withInsertHandler { c, _ ->
            applyKeyAndValueInsertHandler(c, context, constantValue, insertCurlyBraces)
        }
    }
    
    val result = lookupElement.withPriority(priority)
    val extraElements = mutableListOf<LookupElement>()
    
    //进行提示并在提示后插入子句内联模版（仅当子句中允许键为常量字符串的属性时才会提示）
    if(isKey && !isKeyOrValueOnly && isBlock && config != null && getSettings().completion.completeWithClauseTemplate) {
        val entryConfigs = CwtConfigHandler.getEntryConfigs(config)
        if(entryConfigs.isNotEmpty()) {
            val tailText1 = buildString {
                append(" = { <generate via template> }")
                if(tailText != null) append(tailText)
            }
            val lookupElement1 = lookupElement
                .withTailText(tailText1)
                .withExpandClauseTemplateInsertHandler(context, entryConfigs)
                .withPriority(priority)
            extraElements.add(lookupElement1)
        }
    }
    
    return PlsLookupElement(result, extraElements)
}

private fun getIconToUse(icon: Icon?, config: CwtConfig<*>?): Icon? {
    if(icon == null) return null
    when(config) {
        is CwtValueConfig -> {
            if(config.isTagConfig) return PlsIcons.Nodes.Tag
        }
        is CwtAliasConfig -> {
            val aliasConfig = config
            val type = aliasConfig.expression.type
            if(type !in CwtDataTypeGroups.ConstantLike) return icon
            val aliasName = aliasConfig.name
            return when {
                aliasName == "modifier" -> PlsIcons.Nodes.Modifier
                aliasName == "trigger" -> PlsIcons.Nodes.Trigger
                aliasName == "effect" -> PlsIcons.Nodes.Effect
                else -> icon
            }
        }
    }
    return icon
}

private fun skipOrInsertRightQuote(context: ProcessingContext, editor: Editor) {
    if(context.quoted) {
        val offset = editor.caretModel.offset
        val charsSequence = editor.document.charsSequence
        if(charsSequence.get(offset) == '"' && charsSequence.get(offset - 1) != '\\') {
            //移到右边的双引号之后
            editor.caretModel.moveToOffset(offset + 1)
        } else {
            //插入缺失的右边的双引号
            EditorModificationUtil.insertStringAtCaret(editor, "\"")
        }
    }
}

private fun applyKeyOrValueInsertHandler(context: ProcessingContext, c: InsertionContext) {
    skipOrInsertRightQuote(context, c.editor)
}

@Suppress("UNUSED_PARAMETER", "SameParameterValue")
private fun applyValueInsertHandler(c: InsertionContext, context: ProcessingContext, insertCurlyBraces: Boolean) {
    if(!insertCurlyBraces) return
    val customSettings = CodeStyle.getCustomSettings(c.file, ParadoxScriptCodeStyleSettings::class.java)
    val text = if(customSettings.SPACE_WITHIN_BRACES) "{  }" else "{}"
    val length = if(customSettings.SPACE_WITHIN_BRACES) text.length - 2 else text.length - 1
    EditorModificationUtil.insertStringAtCaret(c.editor, text, false, true, length)
}

private fun applyKeyAndValueInsertHandler(c: InsertionContext, context: ProcessingContext, constantValue: String?, insertCurlyBraces: Boolean) {
    val editor = c.editor
    skipOrInsertRightQuote(context, c.editor)
    val customSettings = CodeStyle.getCustomSettings(c.file, ParadoxScriptCodeStyleSettings::class.java)
    val text = buildString {
        append(if(customSettings.SPACE_AROUND_PROPERTY_SEPARATOR) " = " else "=")
        if(constantValue != null) append(constantValue)
        if(insertCurlyBraces) append(if(customSettings.SPACE_WITHIN_BRACES) "{  }" else "{}")
    }
    val length = when {
        insertCurlyBraces -> if(customSettings.SPACE_WITHIN_BRACES) text.length - 2 else text.length - 1
        else -> text.length
    }
    EditorModificationUtil.insertStringAtCaret(editor, text, false, true, length)
}

private fun getDescriptors(constantConfigGroup: Map<CwtDataExpression, List<CwtMemberConfig<*>>>): List<ElementDescriptor> {
    val descriptors = mutableListOf<ElementDescriptor>()
    for((expression, constantConfigs) in constantConfigGroup) {
        when(expression) {
            is CwtValueExpression -> {
                val descriptor = ValueDescriptor(name = expression.expressionString)
                descriptors.add(descriptor)
            }
            is CwtKeyExpression -> {
                val name = expression.expressionString
                val constantValueExpressions = constantConfigs
                    .mapNotNull { it.castOrNull<CwtPropertyConfig>()?.valueExpression?.takeIf { e -> e.type == CwtDataTypes.Constant } }
                val mustBeConstantValue = constantValueExpressions.size == constantConfigs.size
                val value = if(mustBeConstantValue) constantValueExpressions.first().expressionString else ""
                val constantValues = if(constantValueExpressions.isEmpty()) emptyList() else buildList {
                    if(!mustBeConstantValue) add("")
                    constantValueExpressions.forEach { add(it.expressionString) }
                }
                val descriptor = PropertyDescriptor(name = name, value = value, constantValues = constantValues)
                descriptors.add(descriptor)
            }
        }
    }
    return descriptors
}

fun CompletionResultSet.addPlsElement(lookupElement: PlsLookupElement?) {
    if(lookupElement == null) return
    addElement(lookupElement)
    lookupElement.extraElements.forEachFast { addElement(it) }
}

fun CompletionResultSet.addSimpleScriptExpressionElement(lookupElement: LookupElement?, context: ProcessingContext) {
    if(lookupElement == null) return
    val id = lookupElement.lookupString
    if(context.completionIds?.add(id) == false) return
    addElement(lookupElement)
}

fun CompletionResultSet.addBlockScriptExpressionElement(context: ProcessingContext) {
    val id = "{...}"
    if(context.completionIds?.add(id) == false) return
    val lookupElement = PlsLookupElements.blockLookupElement
    addElement(lookupElement)
    
    //进行提示并在提示后插入子句内联模版（仅当子句中允许键为常量字符串的属性时才会提示）
    if(getSettings().completion.completeWithClauseTemplate) {
        val config = context.config!!
        val entryConfigs = CwtConfigHandler.getEntryConfigs(config)
        if(entryConfigs.isNotEmpty()) {
            val tailText1 = "{ <generate via template> }"
            val lookupElement1 = LookupElementBuilder.create("")
                .withPresentableText(tailText1)
                .withExpandClauseTemplateInsertHandler(context, entryConfigs)
            addElement(lookupElement1.withPriority(PlsCompletionPriorities.keywordPriority))
        }
    }
}
