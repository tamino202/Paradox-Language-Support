package icu.windea.pls.localisation.psi

import com.intellij.lang.*
import com.intellij.lang.ParserDefinition.SpaceRequirements.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.TokenType.*
import com.intellij.psi.tree.*
import icu.windea.pls.localisation.psi.ParadoxLocalisationElementTypes.*

class ParadoxLocalisationParserDefinition : ParserDefinition {
	companion object {
		val WHITE_SPACES = TokenSet.create(WHITE_SPACE)
		val COMMENTS = TokenSet.create(COMMENT)
		val STRINGS = TokenSet.create(STRING_TOKEN)
		val FILE = ParadoxLocalisationStubElementTypes.FILE
	}
	
	override fun getWhitespaceTokens() = WHITE_SPACES
	
	override fun getCommentTokens() = COMMENTS
	
	override fun getStringLiteralElements() = STRINGS
	
	override fun getFileNodeType() = FILE
	
	override fun createFile(viewProvider: FileViewProvider): ParadoxLocalisationFile {
		return ParadoxLocalisationFile(viewProvider)
	}
	
	override fun createElement(node: ASTNode): PsiElement {
		return Factory.createElement(node)
	}
	
	override fun createParser(project: Project?): ParadoxLocalisationParser {
		return ParadoxLocalisationParser()
	}
	
	override fun createLexer(project: Project?): ParadoxLocalisationLexerAdapter {
		return ParadoxLocalisationLexerAdapter()
	}
	
	fun createLexer(virtualFile: VirtualFile, project: Project?): ParadoxLocalisationLexerAdapter {
		return ParadoxLocalisationLexerAdapter(ParadoxLocalisationParsingContext(virtualFile, project))
	}
	
	override fun spaceExistenceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements {
		val leftType = left?.elementType
		val rightType = right?.elementType
		return when {
			leftType == LOCALE_ID && rightType == COLON -> MUST_NOT 
			//语言区域之前必须换行
			rightType == LOCALE_ID -> MUST_LINE_BREAK
			//属性之前必须换行
			rightType == PROPERTY_KEY_TOKEN -> MUST_LINE_BREAK
			else -> MAY
		}
	}
}


