package icu.windea.pls.localisation.psi

import com.intellij.lang.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.*
import icu.windea.pls.core.*
import icu.windea.pls.localisation.*
import icu.windea.pls.localisation.psi.ParadoxLocalisationElementTypes.*
import icu.windea.pls.script.psi.*

object ParadoxLocalisationFileStubElementType : IStubFileElementType<PsiFileStub<*>>(ParadoxLocalisationLanguage){
	private const val externalId = "paradoxLocalisation.file"
	private const val stubVersion = 5 //0.7.6
	
	override fun getExternalId() = externalId
	
	override fun getStubVersion() = stubVersion
	
	override fun getBuilder(): StubBuilder {
		return Builder()
	}
	
	override fun shouldBuildStubFor(file: VirtualFile?): Boolean {
		//仅为合法的paradox文件创建索引
		try {
			return file?.fileInfo?.rootType != null
		} catch(e: Exception) {
			return false
		}
	}
	
	class Builder: DefaultStubBuilder(){
		override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean {
			//仅包括propertyList和property
			return when {
				node.elementType == LOCALE -> true
				node.elementType == PROPERTY_LIST -> false
				node.elementType == PROPERTY -> false
				parent.elementType == PROPERTY -> true
				else -> true
			}
		}
	}
	
	override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
		//基于启用的特性来解析本地化文本的词法
		val project = psi.project
		val language = ParadoxLocalisationLanguage
		val features = ParadoxLocalisationFeatures.StellarisFormatReference
		val lexer = ParadoxLocalisationLexerAdapter()
		val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, language, chameleon.chars)
		val parser = ParadoxScriptParser()
		val node = parser.parse(this, builder)
		return node.firstChildNode
	}
}
