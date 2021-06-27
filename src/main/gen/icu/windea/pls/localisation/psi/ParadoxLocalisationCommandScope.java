// This is a generated file. Not intended for manual editing.
package icu.windea.pls.localisation.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.util.Iconable.IconFlags;
import icu.windea.pls.localisation.reference.ParadoxLocalisationCommandScopePsiReference;
import javax.swing.Icon;

public interface ParadoxLocalisationCommandScope extends ParadoxLocalisationCommandIdentifier, ParadoxLocalisationNamedElement {

  @NotNull
  PsiElement getCommandScopeId();

  @NotNull
  Icon getIcon(@IconFlags int flags);

  @NotNull
  String getName();

  @NotNull
  ParadoxLocalisationCommandScope setName(@NotNull String name);

  @NotNull
  PsiElement getNameIdentifier();

  @NotNull
  ParadoxLocalisationCommandScopePsiReference getReference();

  @Nullable
  ParadoxLocalisationCommandIdentifier getPrevIdentifier();

  @Nullable
  ParadoxLocalisationCommandIdentifier getNextIdentifier();

}
