package com.windea.plugin.idea.paradox.script.psi

import com.intellij.lang.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.*
import com.windea.plugin.idea.paradox.*
import com.windea.plugin.idea.paradox.script.*
import com.windea.plugin.idea.paradox.script.psi.ParadoxScriptStubElementTypes.Companion.FILE
import com.windea.plugin.idea.paradox.script.psi.ParadoxScriptTypes.*

class ParadoxScriptFileStubElementType : IStubFileElementType<PsiFileStub<*>>(ParadoxScriptLanguage) {
	override fun getExternalId(): String {
		return "paradoxScript.file"
	}
	
	override fun getBuilder(): StubBuilder {
		return Builder()
	}
	
	override fun shouldBuildStubFor(file: VirtualFile?): Boolean {
		return file?.paradoxFileInfo?.rootType != null
	}
	
	class Builder : DefaultStubBuilder() {
		override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean {
			//仅包括作为scripted_variable的顶级的variable，以及作为definition的property
			val type = node.elementType
			val parentType = parent.elementType
			return when {
				parentType == FILE && type != ROOT_BLOCK -> true
				parentType == FILE -> false
				type == PROPERTY && isDefinition(node) -> false
				type == VARIABLE && isScriptVariable(node,parent) -> false
				parentType == ROOT_BLOCK && type == PROPERTY -> false
				parentType == ROOT_BLOCK || parentType == BLOCK -> true
				parentType == PROPERTY || parentType == PROPERTY_VALUE -> false
				else -> true
			}
		}
		
		private fun isDefinition(node: ASTNode):Boolean{
			val element = node.psi as? ParadoxScriptProperty?:return false
			return element.paradoxTypeInfo != null
		}
		
		private fun isScriptVariable(node: ASTNode,parent:ASTNode): Boolean {
			if(parent.elementType != ROOT_BLOCK) return false
			val file = node.psi.containingFile
			val parentPath = file.paradoxFileInfo?.path?.parent ?: return false
			return "common/scripted_variables".matchesPath(parentPath)
		}
	}
}
