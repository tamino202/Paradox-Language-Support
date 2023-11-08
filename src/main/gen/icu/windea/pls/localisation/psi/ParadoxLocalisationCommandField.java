// This is a generated file. Not intended for manual editing.
package icu.windea.pls.localisation.psi;

import com.intellij.navigation.*;
import com.intellij.psi.search.*;
import icu.windea.pls.localisation.references.*;
import org.jetbrains.annotations.*;

import javax.swing.*;

public interface ParadoxLocalisationCommandField extends ParadoxLocalisationCommandIdentifier {

  @Nullable
  ParadoxLocalisationPropertyReference getPropertyReference();

  @NotNull
  Icon getIcon(@IconFlags int flags);

  @NotNull
  String getName();

  @NotNull
  ParadoxLocalisationCommandField setName(@NotNull String name);

  @Nullable
  ParadoxLocalisationCommandFieldPsiReference getReference();

  @Nullable
  ParadoxLocalisationCommandScope getPrevIdentifier();

  @Nullable
  ParadoxLocalisationCommandIdentifier getNextIdentifier();

  @NotNull
  String getExpression();

  @Nullable
  String getConfigExpression();

  @NotNull
  ItemPresentation getPresentation();

  @NotNull
  GlobalSearchScope getResolveScope();

  @NotNull
  SearchScope getUseScope();

}
