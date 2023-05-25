// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiLiteralValue;
import com.intellij.psi.ContributedReferenceHost;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import icu.windea.pls.lang.model.ParadoxType;

public interface ParadoxScriptFloat extends ParadoxScriptValue, PsiLiteralValue, ContributedReferenceHost {

  @NotNull
  String getName();

  @NotNull
  String getValue();

  float getFloatValue();

  @NotNull
  ParadoxType getType();

  @NotNull
  ItemPresentation getPresentation();

  @NotNull
  GlobalSearchScope getResolveScope();

  @NotNull
  SearchScope getUseScope();

}
