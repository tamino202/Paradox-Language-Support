// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import icu.windea.pls.core.psi.ParadoxConditionParameter;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable.IconFlags;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import icu.windea.pls.core.references.ParadoxArgumentPsiReference;
import javax.swing.Icon;

public interface ParadoxScriptParameterConditionParameter extends ParadoxConditionParameter {

  @NotNull
  Icon getIcon(@IconFlags int flags);

  @NotNull
  String getName();

  @NotNull
  ParadoxScriptParameterConditionParameter setName(@NotNull String name);

  @NotNull
  String getValue();

  int getTextOffset();

  @NotNull
  ParadoxArgumentPsiReference getReference();

  @NotNull
  ItemPresentation getPresentation();

  @NotNull
  GlobalSearchScope getResolveScope();

  @NotNull
  SearchScope getUseScope();

}
