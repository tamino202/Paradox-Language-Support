package icu.windea.pls.cwt.psi

import com.intellij.psi.*
import com.intellij.psi.tree.*
import icu.windea.pls.cwt.psi.CwtElementTypes.*

object CwtTokenSets {
	@JvmField val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
	@JvmField val COMMENTS = TokenSet.create(COMMENT)
	//@JvmField val COMMENTS = TokenSet.create(COMMENT, OPTION_COMMENT, DOCUMENTATION_COMMENT) // do not use this
	@JvmField val STRING_LITERALS = TokenSet.create(STRING_TOKEN)
	
	@JvmField val IDENTIFIER_TOKENS = TokenSet.create(PROPERTY_KEY_TOKEN, OPTION_KEY_TOKEN)
	@JvmField val COMMENT_TOKENS = TokenSet.create(COMMENT, DOCUMENTATION_TOKEN)
	@JvmField val LITERAL_TOKENS = TokenSet.create(STRING_TOKEN)
}