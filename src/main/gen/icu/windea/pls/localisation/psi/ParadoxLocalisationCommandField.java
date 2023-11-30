// This is a generated file. Not intended for manual editing.
package icu.windea.pls.localisation.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable.IconFlags;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import icu.windea.pls.localisation.references.ParadoxLocalisationCommandFieldPsiReference;
import javax.swing.Icon;

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
