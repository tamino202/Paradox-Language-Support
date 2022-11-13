package icu.windea.pls.core.selector

import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import icu.windea.pls.config.internal.config.*
import icu.windea.pls.core.handler.*
import icu.windea.pls.core.model.*

fun <S: ChainedParadoxSelector<T>, T, K> S.distinctBy(selector: (T) -> K) = apply { selectors += ParadoxDistinctSelector(selector) }

fun <S: ChainedParadoxSelector<T>, T> S.gameType(gameType: ParadoxGameType?) = apply { if(gameType != null) selectors += ParadoxGameTypeSelector(gameType) }

/**
 * @param from [VirtualFile] | [PsiFile] | [PsiElement]
 */
fun <S: ChainedParadoxSelector<T>, T> S.gameTypeFrom(from: Any?) = apply { if(from != null) selectors += ParadoxGameTypeSelector(from = from) }

fun <S: ChainedParadoxSelector<T>, T> S.root(rootFile: VirtualFile?) = apply { if(rootFile != null) selectors += ParadoxRootFileSelector(rootFile) }

/**
 * @param from [VirtualFile] | [PsiFile] | [PsiElement]
 */
fun <S: ChainedParadoxSelector<T>, T> S.rootFrom(from: Any?) = apply { if(from != null) selectors += ParadoxRootFileSelector(from = from) }

fun <S: ChainedParadoxSelector<T>, T> S.preferRoot(rootFile: VirtualFile?) = apply { if(rootFile != null) selectors += ParadoxPreferRootFileSelector(rootFile) }

/**
 * @param from [VirtualFile] | [PsiFile] | [PsiElement]
 */
fun <S: ChainedParadoxSelector<T>, T> S.preferRootFrom(from: Any?) = apply { if(from != null) selectors += ParadoxPreferRootFileSelector(from = from) }


fun ParadoxScriptedVariableSelector.distinctByName() = distinctBy { it.name }

fun ParadoxDefinitionSelector.distinctByName() = distinctBy { ParadoxDefinitionInfoHandler.getName(it) }

fun ParadoxLocalisationSelector.distinctByName() = distinctBy { it.name }

fun ParadoxComplexEnumValueSelector.distinctByName() = distinctBy{ ParadoxComplexEnumValueInfoHandler.getName(it) }

fun ParadoxValueSetValueSelector.distinctByValue() = distinctBy{ it.value.substringBefore('@') }

fun ParadoxLocalisationSelector.locale(locale: ParadoxLocaleConfig?) = apply { if(locale != null) selectors += ParadoxLocaleSelector(locale) }

fun ParadoxLocalisationSelector.preferLocale(locale: ParadoxLocaleConfig?) = apply { if(locale != null) selectors += ParadoxPreferLocaleSelector(locale) }