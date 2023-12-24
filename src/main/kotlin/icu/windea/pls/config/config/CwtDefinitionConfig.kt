package icu.windea.pls.config.config

import com.intellij.psi.*

class CwtDefinitionConfig(
	override val pointer: SmartPsiElementPointer<out PsiElement>,
	override val info: CwtConfigGroupInfo,
	val config: CwtMemberConfig<*>,
	val name: String,
	val type: String
): CwtConfig<PsiElement> //CwtProperty | CwtValue
