package icu.windea.pls.tool.localisation

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import com.intellij.util.*
import icu.windea.pls.*
import icu.windea.pls.core.*
import icu.windea.pls.cwt.psi.*
import icu.windea.pls.localisation.highlighter.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.script.psi.*
import icu.windea.pls.tool.*
import java.util.*
import java.util.concurrent.atomic.*

@Suppress("UnstableApiUsage")
object ParadoxLocalisationTextInlayRenderer {
    /**
     * 如果[truncateLimit]小于等于0，则仅渲染首行文本。
     */
    class Context(
        val editor: Editor,
        var builder: MutableList<InlayPresentation>
    ) {
        var truncateLimit: Int = -1
        var iconHeightLimit: Int = -1
        val truncateRemain by lazy { AtomicInteger(truncateLimit) } //记录到需要截断为止所剩余的长度
        val guardStack = LinkedList<String>() //防止StackOverflow
    }
    
    fun render(element: ParadoxLocalisationProperty, factory: PresentationFactory, editor: Editor, truncateLimit: Int, iconHeightLimit: Int): InlayPresentation? {
        //虽然看起来截断后的长度不正确，但是实际上是正确的，因为图标前后往往存在或不存在神秘的空白
        val context = Context(editor, SmartList())
        context.truncateLimit = truncateLimit
        context.iconHeightLimit = iconHeightLimit
        context.guardStack.addLast(element.name)
        val r = factory.renderTo(element, context)
        if(!r) {
            context.builder.add(factory.smallText("...")) //添加省略号
        }
        return context.builder.mergePresentation()
    }
    
    private fun PresentationFactory.renderTo(element: ParadoxLocalisationProperty, context: Context): Boolean {
        val richTextList = element.propertyValue?.richTextList
        if(richTextList == null || richTextList.isEmpty()) return true
        var continueProcess = true
        for(richText in richTextList) {
            ProgressManager.checkCanceled()
            val r = renderTo(richText, context)
            if(!r) {
                continueProcess = false
                break
            }
        }
        return continueProcess
    }
    
    private fun PresentationFactory.renderTo(element: ParadoxLocalisationRichText, context: Context): Boolean {
        return when(element) {
            is ParadoxLocalisationString -> renderStringTo(element, context)
            is ParadoxLocalisationEscape -> renderEscapeTo(element, context)
            is ParadoxLocalisationPropertyReference -> renderPropertyReferenceTo(element, context)
            is ParadoxLocalisationIcon -> renderIconTo(element, context)
            is ParadoxLocalisationCommand -> renderCommandTo(element, context)
            is ParadoxLocalisationColorfulText -> renderColorfulTextTo(element, context)
            else -> true
        }
    }
    
    private fun PresentationFactory.renderStringTo(element: ParadoxLocalisationString, context: Context): Boolean {
        val elementText = element.text
        context.builder.add(truncatedSmallText(elementText, context))
        return continueProcess(context)
    }
    
    private fun PresentationFactory.renderEscapeTo(element: ParadoxLocalisationEscape, context: Context): Boolean {
        //使用原始文本（内嵌注释不能换行，这时直接截断）
        val elementText = element.text
        val text = when {
            elementText == "\\n" -> return false
            elementText == "\\r" -> return false
            elementText == "\\t" -> "\t"
            else -> elementText
        }
        context.builder.add(truncatedSmallText(text, context))
        return continueProcess(context)
    }
    
    private fun PresentationFactory.renderPropertyReferenceTo(element: ParadoxLocalisationPropertyReference, context: Context): Boolean {
        //如果处理文本失败，则使用原始文本，如果有颜色码，则使用该颜色渲染，否则保留颜色码
        val colorConfig = element.colorConfig
        val resolved = element.scriptedVariableReference?.reference?.resolve()
            ?: element.reference?.resolve()
        val presentation = when {
            resolved is ParadoxLocalisationProperty -> {
                val resolvedName = resolved.name
                if(context.guardStack.contains(resolvedName)) {
                    //infinite recursion, do not render context
                    truncatedSmallText(element.text, context)
                } else {
                    context.guardStack.addLast(resolvedName)
                    val oldBuilder = context.builder
                    context.builder = SmartList()
                    renderTo(resolved, context)
                    context.guardStack.removeLast()
                    val newBuilder = context.builder
                    context.builder = oldBuilder
                    newBuilder.mergePresentation()
                }
            }
            resolved is CwtProperty -> {
                smallText(resolved.value ?: PlsConstants.unresolvedString)
            }
            resolved is ParadoxScriptScriptedVariable && resolved.value != null -> {
                smallText(resolved.value ?: PlsConstants.unresolvedString)
            }
            else -> {
                truncatedSmallText(element.text, context)
            }
        } ?: return true
        val textAttributesKey = if(colorConfig != null) ParadoxLocalisationAttributesKeys.getColorOnlyKey(colorConfig.color) else null
        val finalPresentation = when {
            textAttributesKey != null -> WithAttributesPresentation(presentation, textAttributesKey, context.editor)
            else -> presentation
        }
        context.builder.add(finalPresentation)
        return continueProcess(context)
    }
    
    private fun PresentationFactory.renderIconTo(element: ParadoxLocalisationIcon, context: Context): Boolean {
        val resolved = element.reference?.resolve() ?: return true
        val iconUrl = when {
            resolved is ParadoxScriptDefinitionElement -> ParadoxDdsUrlResolver.resolveByDefinition(resolved, defaultToUnknown = true)
            resolved is PsiFile -> ParadoxDdsUrlResolver.resolveByFile(resolved.virtualFile, defaultToUnknown = true)
            else -> return true
        }
        if(iconUrl.isNotEmpty()) {
            //忽略异常
            runCatching {
                //找不到图标的话就直接跳过
                val icon = IconLoader.findIcon(iconUrl.toFileUrl()) ?: return true
                if(icon.iconHeight <= context.iconHeightLimit) {
                    //基于内嵌提示的字体大小缩放图标，直到图标宽度等于字体宽度
                    context.builder.add(smallScaledIcon(icon))
                } else {
                    val unknownIcon = IconLoader.findIcon(PlsPaths.unknownPngUrl) ?: return true
                    context.builder.add(smallScaledIcon(unknownIcon))
                }
            }
        }
        return true
    }
    
    private fun PresentationFactory.renderCommandTo(element: ParadoxLocalisationCommand, context: Context): Boolean {
        //直接显示命令文本
        //点击其中的相关文本也能跳转到相关声明（如scope和scripted_loc）
        element.forEachChild { e ->
            getElementPresentation(context.builder, e, this)
        }
        return continueProcess(context)
    }
    
    private fun PresentationFactory.renderColorfulTextTo(element: ParadoxLocalisationColorfulText, context: Context): Boolean {
        //如果处理文本失败，则清除非法的颜色标记，直接渲染其中的文本
        val richTextList = element.richTextList
        if(richTextList.isEmpty()) return true
        val colorConfig = element.colorConfig
        val textAttributesKey = if(colorConfig != null) ParadoxLocalisationAttributesKeys.getColorOnlyKey(colorConfig.color) else null
        val oldBuilder = context.builder
        context.builder = SmartList()
        var continueProcess = true
        for(richText in richTextList) {
            ProgressManager.checkCanceled()
            val r = renderTo(richText, context)
            if(!r) {
                continueProcess = false
                break
            }
        }
        val newBuilder = context.builder
        context.builder = oldBuilder
        val presentation = newBuilder.mergePresentation() ?: return true
        val finalPresentation = if(textAttributesKey != null) WithAttributesPresentation(presentation, textAttributesKey, context.editor) else presentation
        context.builder.add(finalPresentation)
        return continueProcess
    }
    
    private fun MutableList<InlayPresentation>.mergePresentation(): InlayPresentation? {
        return when {
            isEmpty() -> null
            size == 1 -> first()
            else -> SequencePresentation(this)
        }
    }
    
    private fun PresentationFactory.truncatedSmallText(text: String, context: Context): InlayPresentation {
        if(context.truncateLimit <= 0) {
            val finalText = text
            val result = smallText(finalText)
            return result
        } else {
            val finalText = text.take(context.truncateRemain.get())
            val result = smallText(finalText)
            context.truncateRemain.addAndGet(-text.length)
            return result
        }
    }
    
    private fun continueProcess(context: Context): Boolean {
        return context.truncateLimit <= 0 || context.truncateRemain.get() >= 0
    }
    
    fun getElementPresentation(builder: MutableList<InlayPresentation>, element: PsiElement, factory: PresentationFactory) {
        val text = element.text
        val references = element.references
        if(references.isEmpty()) {
            builder.add(factory.smallText(element.text))
            return
        }
        var i = 0
        for(reference in references) {
            val startOffset = reference.rangeInElement.startOffset
            if(startOffset != i) {
                builder.add(factory.smallText(text.substring(i, startOffset)))
            }
            i = reference.rangeInElement.endOffset
            builder.add(factory.psiSingleReference(factory.smallText(reference.rangeInElement.substring(text))) { reference.resolve() })
        }
        val endOffset = references.last().rangeInElement.endOffset
        if(endOffset != text.length) {
            builder.add(factory.smallText(text.substring(endOffset)))
        }
    }
}
