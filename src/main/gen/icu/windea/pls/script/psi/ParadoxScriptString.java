// This is a generated file. Not intended for manual editing.
package icu.windea.pls.script.psi;

import com.intellij.navigation.*;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import icu.windea.pls.core.psi.*;
import icu.windea.pls.lang.model.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.util.*;

public interface ParadoxScriptString extends ParadoxScriptValue, PsiLiteralValue, ParadoxScriptStringExpressionElement, ParadoxParameterAwareElement {

  @NotNull
  List<ParadoxScriptParameter> getParameterList();

  @NotNull
  Icon getIcon(@IconFlags int flags);

  @NotNull
  String getName();

  @NotNull
  String getValue();

  @NotNull
  ParadoxScriptString setValue(@NotNull String name);

  @NotNull
  String getStringValue();

  @NotNull
  ParadoxType getType();

  @NotNull
  String getExpression();

  @Nullable
  String getConfigExpression();

  @Nullable
  PsiReference getReference();

  @NotNull
  PsiReference[] getReferences();

  @NotNull
  ItemPresentation getPresentation();

  @NotNull
  GlobalSearchScope getResolveScope();

  @NotNull
  SearchScope getUseScope();

}
