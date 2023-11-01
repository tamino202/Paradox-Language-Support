package icu.windea.pls.lang.expressionIndex.impl

import com.intellij.psi.*
import icu.windea.pls.core.*
import icu.windea.pls.core.collections.*
import icu.windea.pls.core.index.*
import icu.windea.pls.core.psi.*
import icu.windea.pls.lang.expressionIndex.*
import icu.windea.pls.localisation.psi.*
import icu.windea.pls.model.*
import icu.windea.pls.model.constraints.*
import icu.windea.pls.model.expressionInfo.*
import java.io.*

private val compressComparator = compareBy<ParadoxValueSetValueInfo>({ it.valueSetName }, { it.name })

class ParadoxValueSetValueIndexSupport: ParadoxExpressionIndexSupport<ParadoxValueSetValueInfo> {
    override fun id() = ParadoxExpressionIndexId.ValueSetValue.id
    
    override fun type() = ParadoxValueSetValueInfo::class.java
    
    override fun indexScriptElement(element: PsiElement, fileData: MutableMap<String, List<ParadoxExpressionInfo>>) {
        val constraint = ParadoxResolveConstraint.ValueSetValue
        if(!constraint.canResolveReference(element)) return
        element.references.forEachFast f@{ reference ->
            if(!constraint.canResolve(reference)) return@f
            val resolved = reference.resolve()
            if(resolved !is ParadoxValueSetValueElement) return@f
            resolved.valueSetNames.forEach { valueSetName ->
                val info = ParadoxValueSetValueInfo(resolved.name, valueSetName, resolved.readWriteAccess, resolved.parent.startOffset, resolved.gameType)
                addToFileData(info, fileData)
            }
        }
    }
    
    override fun indexLocalisationCommandIdentifier(element: ParadoxLocalisationCommandIdentifier, fileData: MutableMap<String, List<ParadoxExpressionInfo>>) {
        val constraint = ParadoxResolveConstraint.ValueSetValue
        if(!constraint.canResolveReference(element)) return
        element.references.forEachFast f@{ reference ->
            if(!constraint.canResolve(reference)) return@f
            val resolved = reference.resolve()
            if(resolved !is ParadoxValueSetValueElement) return@f
            resolved.valueSetNames.forEach { valueSetName ->
                val info = ParadoxValueSetValueInfo(resolved.name, valueSetName, resolved.readWriteAccess, resolved.parent.startOffset, resolved.gameType)
                addToFileData(info, fileData)
            }
        }
    }
    
    override fun compressData(value: List<ParadoxValueSetValueInfo>): List<ParadoxValueSetValueInfo> {
        return value.sortedWith(compressComparator)
    }
    
    override fun writeData(storage: DataOutput, info: ParadoxValueSetValueInfo, previousInfo: ParadoxValueSetValueInfo?, gameType: ParadoxGameType) {
        storage.writeOrWriteFrom(info, previousInfo, { it.name }, { storage.writeUTFFast(it) })
        storage.writeOrWriteFrom(info, previousInfo, { it.valueSetName }, { storage.writeUTFFast(it) })
        storage.writeByte(info.readWriteAccess.toByte())
        storage.writeIntFast(info.elementOffset)
    }
    
    override fun readData(storage: DataInput, previousInfo: ParadoxValueSetValueInfo?, gameType: ParadoxGameType): ParadoxValueSetValueInfo {
        val name = storage.readOrReadFrom(previousInfo, { it.name }, { storage.readUTFFast() })
        val valueSetName = storage.readOrReadFrom(previousInfo, { it.valueSetName }, { storage.readUTFFast() })
        val readWriteAccess = storage.readByte().toReadWriteAccess()
        val elementOffset = storage.readIntFast()
        return ParadoxValueSetValueInfo(name, valueSetName, readWriteAccess, elementOffset, gameType)
    }
}