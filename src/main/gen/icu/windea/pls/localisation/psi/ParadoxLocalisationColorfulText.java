// This is a generated file. Not intended for manual editing.
package icu.windea.pls.localisation.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import icu.windea.pls.localisation.reference.ParadoxLocalisationColorReference;

public interface ParadoxLocalisationColorfulText extends ParadoxLocalisationRichText {

  @NotNull
  List<ParadoxLocalisationRichText> getRichTextList();

  @Nullable
  String getName();

  @NotNull
  ParadoxLocalisationColorfulText setName(@NotNull String name);

  @Nullable
  ParadoxLocalisationColorReference getReference();

}
