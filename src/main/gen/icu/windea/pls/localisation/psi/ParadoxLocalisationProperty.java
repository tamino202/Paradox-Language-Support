// This is a generated file. Not intended for manual editing.
package icu.windea.pls.localisation.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import icu.windea.pls.model.ParadoxLocalisationCategory;
import javax.swing.Icon;

public interface ParadoxLocalisationProperty extends ParadoxLocalisationNamedElement, StubBasedPsiElement<ParadoxLocalisationStub> {

  @NotNull
  ParadoxLocalisationPropertyKey getPropertyKey();

  @Nullable
  ParadoxLocalisationPropertyValue getPropertyValue();

  @NotNull
  Icon getIcon(@IconFlags int flags);

  @NotNull
  String getName();

  @NotNull
  ParadoxLocalisationProperty setName(@NotNull String name);

  @NotNull
  PsiElement getNameIdentifier();

  int getTextOffset();

  @Nullable
  ParadoxLocalisationCategory getCategory();

  @Nullable
  String getValue();

}
